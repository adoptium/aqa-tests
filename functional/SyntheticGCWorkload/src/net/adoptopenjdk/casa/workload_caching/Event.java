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
package net.adoptopenjdk.casa.workload_caching;
import java.io.PrintStream;

import net.adoptopenjdk.casa.event.EventHandler;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.verbose_gc_parser.VerboseUtil;

/**
 * Handles events that happen in the db cache simulation.    
 * 
 *  
 *
 */
enum Event implements EventHandler
{	
	WARNING         (0, "WARNING: ",          Action.MESSAGE),
	NOTICE          (0, "NOTICE: ",           Action.MESSAGE),
	SUCCESS         (0, null,                 Action.EXIT_SUCCESS),
	FAILURE         (1, null,                 Action.EXIT_SUCCESS),
	INTERRUPT       (5, "INTERRUPTED: ",      Action.EXIT),
	BAD_ARGUMENTS   (10, null,                Action.EXIT_USAGE),
	FATAL_ERROR     (-1, "FATAL ERROR: ",     Action.EXIT_TRACE),	
	PARSE_ERROR     (-4, "PARSE ERROR: ",     Action.EXIT);
	
	private static final boolean DEBUG = false;  
	
	private final int code;
	private final String messagePrefix;	
	private final PrintStream errorStream;
	
	private String message; 
	private Throwable associatedThrowable;
	
	private Action action; 
	
	/**
	 * Describes the nature of the event and informs on 
	 * the type of action that should be taken. 
	 * 
	 *  
	 */
	private enum Action 
	{
		MESSAGE (), 
		EXIT (), 
		EXIT_SUCCESS (),
		EXIT_TRACE (), 
		EXIT_USAGE ();
				
		public boolean isFatal()
		{
			return !equals(MESSAGE); 
		}			
		
		public boolean isGracefulShutdown()
		{
			return isFatal() && (equals(EXIT_SUCCESS) || equals(EXIT_USAGE));
		}
		
		public boolean printStackTrace()
		{
			return equals(Action.EXIT_TRACE) || DEBUG;
		}
		
		public boolean printThrowableName()
		{
			return (isFatal() && !equals(EXIT_USAGE)) || DEBUG;
		}
	}

	/**
	 * 
	 * @param code
	 * @param messagePrefix
	 * @param action
	 */
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
	 * Prints the encapsulated message and throwable stack trace, if applicable. 
	 */
	private void printMessage()
	{		
		if (message != null) 
		{
			// Synchronize on the error stream 
			
			// Print the message and nullify it. 
			errorStream.println(message);
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
	}	
	
	public boolean issue(String message) 
	{		 	
		try 
		{
			
			setMessage(message);
			
			// Stop printing the progress bar and make way for a message 
			Main.disableStatus();
			
			if (action.isFatal())
			{		
				printMessage();
				// Attempt shutdown in the manner specified. Returns true if this call triggered the shutdown. 
				if (action.isGracefulShutdown() ? Main.shutdown(code) : Main.kill(code))								
					return true;				
				else													
					return false;				
			}
			else 
			{	
				// Just print the message for non-fatal cases, return true. 
				printMessage();
				// All the progress bar to be printed again. 
				Main.enableStatus();
				
				return true; 
			}		
			
						
		}
		catch (Error | Exception e) 
		{	
			Main.err.println(e);
			System.exit(code);
			return false; 
			//return Event.FATAL_ERROR.issue(e); 
		}				
	}
	
	public boolean issue()
	{
		return issue((String)null);				
	}
		
	public boolean issue(Throwable e, String message)
	{	
		this.associatedThrowable = e; 
		
		if (message == null)
			message = formatException(e);
		else 
			message = formatException(e) + ": " + message;
						
		return issue(message);
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
	 * Formats the given exception for use in an error message. 
	 * 
	 * @param e
	 * @return
	 */
	private String formatException(Throwable e)
	{
		try 
		{
			String string = ""; 
							
			if (e.getMessage() != null) 
			{
				if (action.printThrowableName()) 
					string += e.getClass().getName() + ": "; 			
				string += e.getMessage();
			}
			else 
			{
				string += e.getClass().getName();
			}
			
			return string;
		}
		catch (Throwable t) 
		{
			return null; 
		}
	}

	/**
	 * Formats the message for output and sets the 
	 * message instance variable. 
	 * 	
	 * @param message
	 */
	private void setMessage(String message)
	{		
		if (message != null) 
		{	
			this.message = VerboseUtil.formatTimestamp(0, Utilities.getAbsoluteTime()) + ": "; 
			
			if (this.messagePrefix != null)
				this.message += this.messagePrefix; 

			this.message += message;					
		}
		else 
		{
			this.message = null; 
		}
	}
}
