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
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * The main class for the caching workload. Execute with -h for help.  
 * 
 *  
 *
 */
public class Main 
{
	// Custom output and error streams. 
	protected static final PrintStream err; 
	protected static final PrintStream out; 
	
	// Multiplexers that output custom output and error streams. 
	private static final OutputMultiplexer errorOutputStream; 
	private static final OutputMultiplexer standardOutputStream;	
	
	// If true, carriage returns will not be used in any output. 
	private static boolean alwaysUseNewLines;  
	
	public static QueryResponse exposedResponse; 
	
	// 	 
	private static String configFilename; 
	private static Thread mainThread; 
	
	private static String throughputLogFilename; 
	private static double throughputLogGranularity; 
	
	private static double samplingWindow1Time;
	private static double samplingWindow2Time;
	
	private static AtomicMutex shutdownMutex; 
	
	private static CacheSimulations simulations; 
	
	private static final String HEADING_STRING = 
			"=========================================================\n" + 
			" J9 Synthetic Garbage Collection Workload - Caching\n" +
			"=========================================================";

	static
	{				 			
		configFilename = "";
		
		alwaysUseNewLines = false;		
		 
		shutdownMutex = new AtomicMutex(false); 		
		simulations = null; 
		
		// Throughput logging options 
		throughputLogFilename    = null; 
		throughputLogGranularity = ParseUtilities.parseTime("10ms");
		samplingWindow1Time      = throughputLogGranularity * 10;
		samplingWindow2Time      = throughputLogGranularity * 100;
		
		// Initialize output streams. 
		errorOutputStream    = new OutputMultiplexer();
		standardOutputStream = new OutputMultiplexer();
		// Add console output stream(s). Both streams go to System.out
		errorOutputStream.add(System.out, true);
		standardOutputStream.add(System.out, true);
		// Attach print streams 
		err = new PrintStream(errorOutputStream);
		out = new PrintStream(standardOutputStream);
	}
		

	/**
	 * 
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
	{
		mainThread = Thread.currentThread(); 
			
		Main.out.println(HEADING_STRING);
		
		// Parse the command line arguments. Errors handled by Event
		parseArgs(args);
		
		// Parse the configuration file and construct the CacheSimulations object. 
		try 
		{
			simulations = parseConfiguration(configFilename);
		} 
		catch (ParseError e) 
		{
			Event.BAD_ARGUMENTS.issue();
			simulations = null; 
		} 
		
		// Handle VM shutdown (eg: Ctrl+C)  
		Runtime.getRuntime().addShutdownHook
		(
			new Thread() 
			{
				public void run()
				{	
					if (!shutdownMutex.isLocked())
					{					
						Event.INTERRUPT.issue("run may be incomplete. ");													
					}
				}
			}
		);
			
		if (simulations != null)
		{
			// Print the configuration in human readable format. 
			Main.out.print(simulations);
			
			// Start the simulations. 
			simulations.run(); 
		}
			
		/*
		 * Shutdown on "success". If this isn't a success, a 
		 * call to issue on another status will have been 
		 * issued elsewhere. 
		 */
		Event.SUCCESS.issue(); 
	}
	
	/**
	 * Allows the progress bar to be printed again. 
	 */
	protected static void enableStatus()
	{
		if (simulations != null)
			simulations.enableStatus(shutdownMutex);		
	}
	
	/**
	 * Disables the progress bar. 
	 */
	protected static void disableStatus()
	{
		if (simulations != null)
			simulations.disableStatus(shutdownMutex);  				
	}
	
	/**
	 * Exposes the given query response in a public static variable. 
	 * 
	 * @param response
	 */
	protected static void exposeResponse(QueryResponse response)
	{
		exposedResponse = response; 
	}
	
	/**
	 * Shuts down the simulations without blocking
	 * 
	 * @param code
	 * @return
	 */
	protected static boolean kill(int code)
	{
		if (shutdownMutex.acquire())
		{
			if (simulations != null)						
				simulations.kill();			
					
			if (Thread.currentThread().equals(mainThread))							
				System.exit(code);			
			else if (mainThread != null)					
				mainThread.interrupt();
						
			return true;			
		}
		
		return false; 
	}
	
	/**
	 * Shuts down the simulations gracefully 
	 *  
	 * @param code
	 * @return
	 */
	protected static boolean shutdown(int code)
	{
		if (shutdownMutex.acquire())
		{					
			if (simulations != null)
			{
				try { simulations.shutdown(); } 
				catch (InterruptedException e) { }
			}
			
			if (Thread.currentThread().equals(mainThread))							
				System.exit(code);			
			else if (mainThread != null)					
				mainThread.interrupt();
			
			return true; 
		}
		
		return false; 
	}
	
	/**
	 * Parses the given command line arguments 
	 * 
	 * @param args
	 */
	private static boolean parseArgs(String args[])
	{
		try 
		{
			ArgParser argParser = new ArgParser(true);
			
			/*
			 * We expect to parse 1 configuration file from the command line.  
			 */
			Arg filenameArg = new Arg(
				"Configuration File", 
				true, 
				new ArgParameterType[] {ArgParameterType.STRING}, 
				new ArgHandler()
				{			
					public void handle(Arg arg) throws ArgException 
					{				
						configFilename = arg.getParameterType(0).parse(arg.getParameter(0));						
					}
				}
			);
											
			argParser.addArg(filenameArg);
			
			
			/*
			 * Throughput Log arguments 
			 * 
			 */
			Arg throughputLogArg = new Arg("Logs throughput to the given file.", "throughput_log_file", 't', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	
					throughputLogFilename = arg.getParameterType(0).parse(arg.getParameter(0)); 															
				}
			});
			
			throughputLogArg.add(new Arg("Logs throughput to the file at this interval.", "throughput_log_granularity", 'g', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	
					throughputLogGranularity = ParseUtilities.parseTime((String)arg.getParameterType(0).parse(arg.getParameter(0)));									
				}
			}));
			
			throughputLogArg.add(new Arg("Computes weighted average 1 over this period of time.", "throughput_log_sampling_time_1", 'w', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	
					samplingWindow1Time = ParseUtilities.parseTime((String)arg.getParameterType(0).parse(arg.getParameter(0))); 															
				}
			}));
			
			throughputLogArg.add(new Arg("Computes weighted average 2 over this period of time. Must be larger than window 1.", "throughput_log_sampling_time_2", 'W', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	
					samplingWindow1Time = ParseUtilities.parseTime((String)arg.getParameterType(0).parse(arg.getParameter(0))); 															
				}
			}));
						
			argParser.addArg(throughputLogArg);
			
			/*
			 * Log file for all output 
			 */			
			argParser.addArg(new Arg("Sends output to the supplied log file.", "log_file", 'l', false, new ArgParameterType[] {ArgParameterType.STRING}, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	
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
			
			/*
			 * Silences all console output except Main.err. 
			 */
			argParser.addArg(new Arg("Silences status and informational messages; errors will continue to be sent to stdout.", "silent", 's', false, null, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	 
					// Remove consoles from standard output stream, but allow the errors and warnings to reach them. 
					standardOutputStream.removeConsoles(); 							
				}
			}));

			/*
			 * Tells the workload to not print \r's
			 */
			argParser.addArg(new Arg("Forces the use of newlines rather than carriage returns in status thread console output.", "use-newlines", 'n', false, null, new ArgHandler() 
			{			
				public void handle(Arg arg) throws ArgException 
				{	 
					alwaysUseNewLines = true; 					
				}
			}));
			
			argParser.parse(args);		
		} 
		catch (Throwable e) 
		{						
			Event.BAD_ARGUMENTS.issue(e);			
			return false; 
		}	
		
		return true; 
	}	
	
	/**
	 * Parses the configuration file specified by the first argument and 
	 * returns a CacheSimulations instance containing cache 
	 * simulations constructed form the parsed configuration.   
	 * 
	 * @param filename
	 * @return
	 * @throws ParseError 
	 */
	private static CacheSimulations parseConfiguration(String filename) throws ParseError
	{			
		return new CacheSimulations(new CacheSimulationConfigurations(filename), throughputLogFilename, throughputLogGranularity, samplingWindow1Time, samplingWindow2Time, alwaysUseNewLines);	 
	}
}