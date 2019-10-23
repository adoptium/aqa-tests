/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package net.adoptopenjdk.casa.workload_sessions;
import java.io.PrintStream;

import net.adoptopenjdk.casa.event.EventHandler;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.verbose_gc_parser.VerboseUtil;

/**
 * Error handling enum used for reporting errors to the user in the benchmark.  
 * 
 *  
 *
 */
enum Event implements EventHandler
{
	BAD_ARGUMENTS   (10, null,                     Action.CLEAN_SHUTDOWN),
	INTERRUPT       (1 , "INTERRUPTED: ",          Action.CLEAN_SHUTDOWN),	
	NOTICE          (0 , "NOTICE: ",               Action.MESSAGE),
	WARNING         (0 , "WARNING: ",              Action.MESSAGE),
	SUCCESS         (0 , null,                     Action.CLEAN_SHUTDOWN),
	// Thrown in fatal situations where execution of java code required to shutdown is likely to succeed. 
	FATAL_ERROR     (-1, "FATAL ERROR: ",          Action.IMMEDIATE_SHUTDOWN),
	// Thrown in emergencies when the VM must shutdown immediately. Will perform an orderly shutdown if possible. (eg: OOM) 
	RUNTIME_EXCEPTION (-2, "RUNTIME EXCEPTION: ",  Action.RUNTIME_EXCEPTION),
	// 
	ASSERTION       (-3, "FAILED ASSERTION: ",     Action.IMMEDIATE_SHUTDOWN),
	PARSE_ERROR     (-4, "PARSE ERROR: ",      Action.CLEAN_SHUTDOWN);

	private final int FAILED_ISSUE_CODE = -128;
	public  final int USER_EXCEPTION_CODE = -64;
	
	// The code upon which to exit, should the action call for it. 
	private final int code;
	
	// A prefix to describe the type of event. 
	private final String messagePrefix;	
	
	// All information printed is sent to this stream. 
	private final PrintStream errorStream;
	
	// A formatted message that may include the prefix and information from the throwable. 
	private String message; 
	
	// The event may have been issued due to something being thrown. 
	private Throwable associatedThrowable;
	
	// Informs on the action which should be taken when this event is issued. 
	private Action action; 
	
	// Set the debug flag to see stack traces on any event. 
	private static final boolean DEBUG = false; 
	
	/**
	 * Describes the nature of the event and informs on 
	 * the type of action that should be taken. 
	 * 
	 *  
	 */
	private enum Action 
	{
		/**
		 * Just print something. 
		 */
		MESSAGE (), 
		/**
		 * Shutdown cleanly, allowing threads to join, etc...
		 */
		CLEAN_SHUTDOWN (),
		/**
		 * Shutdown without joining threads or waiting for anything to finish. 
		 */
		IMMEDIATE_SHUTDOWN (), 
		/**
		 * Throw a runtime exception to get out fast. May attempt to shutdown gracefully first. 
		 */
		RUNTIME_EXCEPTION ();
				
		/**
		 * Does this event entail a shutdown? 
		 * 
		 * @return
		 */
		public boolean isFatal()
		{
			return equals(Action.IMMEDIATE_SHUTDOWN) || equals(Action.RUNTIME_EXCEPTION) || equals(Action.CLEAN_SHUTDOWN); 
		}
		
		/**
		 * Should the name of the associated Throwable be printed?
		 * 
		 * @return
		 */
		public boolean printExceptionName()
		{
			return printStackTrace(); 
		}
		
		/**
		 * Should a stack trace be printed? 
		 * 
		 * @return
		 */
		public boolean printStackTrace()
		{
			return equals(Action.IMMEDIATE_SHUTDOWN) || DEBUG;
		}
		
		/**
		 * Should this event cause a quick shutdown due to exceptional circumstances? 
		 * 
		 * @return
		 */
		public boolean isRuntimeException()
		{
			return equals(Action.RUNTIME_EXCEPTION);
		}
	}
		
	private Event(int code, String messagePrefix, Action action) 
	{
		this.code = code; 
		this.messagePrefix = messagePrefix;		
		this.message = null; 
		this.action = action; 
		this.associatedThrowable = null; 
		errorStream = Main.err; 
	}

	/**
	 * Lets the main thread know if the shutdown can be 
	 * graceful or not. This avoids long join() times 
	 * in situations where the VM is overloaded or out 
	 * of memory.  
	 * 
	 * @return true if it is OK to do a graceful shutdown. 
	 */
	public boolean doCleanShutdown()
	{
		return action.equals(Action.CLEAN_SHUTDOWN);  		
	}
	
	/**
	 * Prints the encapsulated message to standard out and nullifies it. Also 
	 * prints a stack trace if the action calls for it. 
	 * 
	 * Suppresses output from the status thread during printing. 
	 */
	private void printMessage()
	{
		try 
		{
			// Stop the status thread from printing.
			try { Main.getWorkloads().suppressStatusOutput(); }
			catch (NullPointerException e) {}
			
			
			if (message != null)
			{					
				// Output the message and nullify it. 
				Main.err.println(message);
				message = null;																		
			}
					
			// Look to the action type to see if we should print a trace.  
			if (action.printStackTrace())
			{
				// If we have a Throwable, print its stack trace 
				if (associatedThrowable != null)					
					associatedThrowable.printStackTrace(errorStream);										
				// Otherwise, fabricate one. 
				else 					
					new Throwable().printStackTrace(errorStream);				
			}
			
			associatedThrowable = null;
				
			// Allow the status thread to continue printing if the event isn't fatal
			if (!action.isFatal())
			{			
				try { Main.getWorkloads().resumeStatusOutput(); }
				catch (NullPointerException e) {}						
			}	
			
		}
		catch (Throwable t) 
		{ 
			System.exit(FAILED_ISSUE_CODE);
		}
	}	
	
	/**
	 * Performs a shutdown, if possible. 
	 * 
	 * @return
	 */
	private boolean shutdown() throws Throwable
	{
		// Try to perform shutdown. 
		if (Main.shutdown(this))
		{
			printMessage();
			
			// The main thread exits, others interrupt the main thread and return true. 
			if (Main.isMainThread())
			{
				System.exit(code);
			}
			else
			{
				Main.interruptMainThread();				
			}

			return true;
		}
		// Thread did not perform the shutdown 
		else 
		{
			return false;
		}
	}
	
	public boolean issue(String message) 
	{		 	
		try 
		{
			setMessage(message);
			
			// Throw a RuntimeException? 
			if (action.isRuntimeException()) 
			{							
				boolean performedShutdown = shutdown(); 
				 
				System.exit(code);

				return performedShutdown; 
			}
			// Shutdown? 
			else if (action.isFatal())
			{						
				return shutdown();
			}
			// Just print a message
			else 
			{	
				printMessage();
				return true;
			}			 		
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		// Issue failed. 
		catch (Throwable e) 
		{				
			System.exit(FAILED_ISSUE_CODE);
			return false; 
		}
	}

	public boolean issue(Throwable e, String message)
	{	
		try 
		{
			associatedThrowable = e; 

			// Append information about the throwable to the message. 
			if (e != null)
			{
				if (message == null)
					message = formatException(e);
				else 
					message = formatException(e) + ": " + message;
			}				
			
			return issue(message);
		}
		catch (RuntimeException f)
		{
			throw f;
		}
		catch (Throwable t)
		{
			System.exit(FAILED_ISSUE_CODE);
			return false; 
		}
	}
		
	public boolean issue()
	{				
		return issue((String)null);
	}
	
	public boolean issue(Throwable e)
	{						
		return issue(e, null);	
	}	
		
	public boolean issue(boolean condition, String message)
	{					
		if (condition) 																
			return issue(message);				
		else 
			return false; 
	}
	
	public boolean issue(boolean condition, Throwable e)
	{					
		if (condition) 															
			return issue(e);				
		else 
			return false; 
	}

	public boolean issue(boolean condition, Throwable e, String message)
	{					
		if (condition) 																
			return issue(e, message);				
		else
			return false; 	
	}
	
	/**
	 * Gets a string representation of the exception. 
	 * 
	 * @param e
	 * @return
	 */
	private String formatException(Throwable e)
	{
		try 
		{
			StringBuilder builder = new StringBuilder(); 
			
			// If there is a message in the throwable, we want to add it to the output. 
			if (e.getMessage() != null) 
			{
				// Append the name if called for. 
				if (action.printExceptionName()) 
					builder.append(e.getClass().getName() + ": "); 			
				
				// Append the message 
				builder.append(e.getMessage());
			}
			else 
			{
				// Append the name if there is no message, regardless of the action. 
				builder.append(e.getClass().getName());
			}
			
			return builder.toString();
		}
		catch (Throwable t) 
		{
			System.exit(FAILED_ISSUE_CODE);
			return ""; 
		}
	}
		
	/**
	 * Sets the message, possibly adding a prefix. 
	 * 
	 * @param message
	 */
	private void setMessage(String message)
	{	
		if (message == null)
		{
			this.message = null; 
		}
		// Format the message to include the prefix. 
		else 
		{	
			this.message = VerboseUtil.formatTimestamp(0, Utilities.getAbsoluteTime()) + ": ";  
			
			if (this.messagePrefix != null)
				this.message += this.messagePrefix; 

			this.message += message;					
		}
		
	}	
	
}
