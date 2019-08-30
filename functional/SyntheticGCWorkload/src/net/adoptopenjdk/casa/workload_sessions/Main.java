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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import net.adoptopenjdk.casa.arg_parser.Arg;
import net.adoptopenjdk.casa.arg_parser.ArgException;
import net.adoptopenjdk.casa.arg_parser.ArgHandler;
import net.adoptopenjdk.casa.arg_parser.ArgParameterType;
import net.adoptopenjdk.casa.arg_parser.ArgParser;
import net.adoptopenjdk.casa.data_structures.AtomicMutex;
import net.adoptopenjdk.casa.util.OutputMultiplexer;
import net.adoptopenjdk.casa.workload_sessions.configuration.Configuration;
import net.adoptopenjdk.casa.xml_parser.ParseError;

/**
 * Main class for the sessions workload application. This application 
 * simulates an application with a finite runtime.  
 * 
 *  
 */
public class Main 
{		
	// Allows only one thread to initiate shutdown 
	private static final AtomicMutex shutdownMutex;
	 
	// Custom output and error streams. 
	protected static final PrintStream err; 
	protected static final PrintStream out; 
	
	// Multiplexers that output custom output and error streams. 
	private static final OutputMultiplexer errorOutputStream; 
	private static final OutputMultiplexer standardOutputStream;	
	
	// Whether or not to use newlines (vs. carriage returns) in status updates. 
	private static boolean alwaysUseNewLines;  
	
	// The main thread must be treated differently during shutdown 
	private static Thread mainThread;
	
	// The currently running workloads instance 
	private static Workloads workloads;	
	
	// The configuration filename for the currently running workloads. 
	private static String configFilename; 
	
	private static final String HEADING_STRING = 
			"=========================================================\n" + 
			" J9 Synthetic Garbage Collection Workload - Sessions\n" + 
			"=========================================================";
		
	static
	{
		// Locked when shutting down to prevent multiple threads from performing shutdown 
		shutdownMutex   = new AtomicMutex();
		
		
		workloads = null;  
				
		// Default is to use \r after each status update. Not necessarily the best option for logging.  
		alwaysUseNewLines = false;
				
		// Initialize output streams. 
		errorOutputStream    = new OutputMultiplexer();
		standardOutputStream = new OutputMultiplexer();
		
		// Add console output stream(s). 
		errorOutputStream.add(System.out, true);
		standardOutputStream.add(System.out, true);
		
		// Attach print streams 
		err = new PrintStream(errorOutputStream);
		out = new PrintStream(standardOutputStream);			
	}

	
	/**
	 * Load workload configurations from the supplied XML file and run them. 
	 * 
	 * @param args	
	 */
	public static void main(String[] args)  
	{	
		mainThread      = Thread.currentThread(); 		 		 				
		
		try 
		{		
			// Parse the command line arguments 
			parseArgs(args);
			
			// Print heading 
			Main.out.println (HEADING_STRING);
			
			// Add hook for VM shutdown (Ctrl + C) 
			new ShutdownHook().addToRuntime();  									 	
			
			// Parse workloads from configuration file 
			workloads = new Workloads(Configuration.parseFrom(configFilename, Event.PARSE_ERROR, Event.NOTICE, Event.WARNING)); 
			
			// Execute workloads 
			workloads.execute(); 					
		}
		catch (ParseError e)
		{
			Event.PARSE_ERROR.issue(e); 
		}
		catch (ArgException e) 
		{			
			Event.BAD_ARGUMENTS.issue(e);	
		}
		catch (InterruptedException e) 
		{ 
			Event.INTERRUPT.issue();  
		}
		catch (Throwable e)
		{
			Event.FATAL_ERROR.issue(e, "unexpected error in main thread.");
		}
		
		// Attempt to exit on successfully 
		Event.SUCCESS.issue();		
	}

	/**
	 * Used when run as agent. Simply runs the main method in a new thread
	 */
	public static void premainRunner(final String [] args) {
		
		Thread main = new Thread(new Runnable(){
		
			@Override
			public void run() {
				main(args);
			}
		});

		main.start();		
    }
	
	/**
	 * Gets the workloads object. 
	 * 
	 * @return
	 */
	protected static Workloads getWorkloads()
	{
		return workloads; 
	}
	
	/**
	 * Parse the command line arguments. 
	 * 
	 * @param args
	 * @throws ArgException
	 */
	private static void parseArgs(String [] args) throws ArgException
	{
		ArgParser argParser = new ArgParser(true);			
		
		argParser.addArg(new Arg("Supply a single worklaod configuration file.", true, new ArgParameterType[] {ArgParameterType.STRING}, 
			new ArgHandler()
			{			
				public void handle(Arg arg) throws ArgException 
				{				
					configFilename = arg.getParameterType(0).parse(arg.getParameter(0));							
				}
			}));
									
		argParser.addArg(new Arg("Sends output to the supplied log file.", "log_file", 'l', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() {			
			public void handle(Arg arg) throws ArgException {	
				String logFilename = arg.getParameterType(0).parse(arg.getParameter(0)); 
				
				try 
				{
					FileOutputStream logStream = new FileOutputStream(logFilename);
					errorOutputStream.add(logStream, false);
					standardOutputStream.add(logStream, false);
				} 
				catch (FileNotFoundException e) 
				{
					Event.FATAL_ERROR.issue(e);
				}							
			}
		}));
		
		argParser.addArg(new Arg("Silences status and informational messages; errors will continue to be sent to stdout.", "silent", 's', false, null, new ArgHandler() 
		{			
			public void handle(Arg arg) throws ArgException 
			{	 
				// Remove consoles from standard output stream, but allow the errors and warnings to reach them. 
				standardOutputStream.removeConsoles(); 							
			}
		}));

		 			
			
		argParser.addArg(new Arg("Forces the use of newlines rather than carriage returns in status thread console output.", "use-newlines", 'n', false, null, new ArgHandler() 
		{			
			public void handle(Arg arg) throws ArgException 
			{	 
				alwaysUseNewLines = true; 					
			}
		}));

		argParser.parse(args);	
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	protected static boolean alwaysUseNewLines() 
	{
		return alwaysUseNewLines; 
	}

	/**
	 * Triggers a shutdown of the workloads and application. Performs shutdown on the 
	 * first call only. 
	 * 
	 * The method used to shutdown the workload depends on the Event's doCleanShutdown() 
	 * method's return value. 
	 * 
	 * @param event
	 * @return true if the caller triggered the shutdown; false if the shutdown was already triggered by another thread. 
	 */
	protected static boolean shutdown(Event event)
	{					  
		if (shutdownMutex.acquire()) 
		{														 					
			if (workloads != null) 
			{				
				try 
				{ 
					if (event.doCleanShutdown()) 
						workloads.shutdown();
					else 						
						workloads.kill();																	
				}
				catch (Exception e) 
				{ 
					Event.RUNTIME_EXCEPTION.issue(e);					
				} 								
			}				
			
			// The call triggered the shutdown. 
			return true;
		}
		
		// The call didn't trigger shutdown. 
		return false;				
	}
		
	/**
	 * Checks to see if the calling thread is the main thread.  
	 * 
	 * @return
	 */
	protected static boolean isMainThread()
	{
		return Thread.currentThread() == mainThread; 
	}
	
	/**
	 * Interrupts the thread that executed main()
	 */
	protected static void interruptMainThread()
	{
		mainThread.interrupt(); 
	}
	
	/**
	 * Handles VM shutdown. 
	 * 
	 *  
	 */
	protected static class ShutdownHook extends Thread 
	{			
		/**
		 * Registers the hook with the VM
		 */		 
		public void addToRuntime()
		{
			Runtime.getRuntime().addShutdownHook(this);
		}
		
		/**
		 * Issues an interrupt event. 
		 */
		public void run() 
		{		
			// Call issue() on INTERRUPT 
			Event.INTERRUPT.issue("caught VM shutdown signal: run may be incomplete.");	    		
		}					
	}
}
