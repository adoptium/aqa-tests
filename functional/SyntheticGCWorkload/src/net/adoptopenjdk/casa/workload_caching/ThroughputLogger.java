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
import java.util.concurrent.atomic.AtomicLong;

import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Produces a fine-grained log of throughput in the cache simulation 
 * 
 *  
 *
 */
public class ThroughputLogger
{
	// The minimum value the user may specify for the granularity 
	private static final double MIN_USER_GRANULARITY = ParseUtilities.parseTime("1us");
	
	// The period with which the log is written, in seconds 
	private final double granularity; 
	 
				
	// The logger thread buffers prints to the log file. 
	private ThroughputLoggerPrintThread loggerThread;	

	// The sum of all delta values passed to processReportingCycle 
	private AtomicLong count = new AtomicLong(0);  
	
	// The time this logger was initialized. 
	private double startTime;
	
		
	/**
	 * Creates a new logger which outputs to the given file with the given granularity. The 
	 * logger uses two sample windows, each retaining samples from a different number of 
	 * past cycles. 
	 * 
	 * @param simulation
	 * @param logFile
	 * @param granularity
	 * @throws FileNotFoundException 
	 */
	protected ThroughputLogger(CacheSimulations simulations, String logFile, double granularity, double sampleWindow1Time, double sampleWindow2Time) throws FileNotFoundException
	{				
		this.granularity = granularity;
		
		double tolerance = granularity / 2.0; 
		
		this.startTime = Utilities.getTime(); 			
		
		// Insure that the log file granularity is greater than the minimum value 
		if (granularity < MIN_USER_GRANULARITY)		
			Event.PARSE_ERROR.issue("Log file granularity must be at least " + Utilities.formatTime(MIN_USER_GRANULARITY));
		
		/*
		 * The sample windows must be positioned at least a factor of two above the 
		 * granularity and at least a factor of two apart. 
		 */
		final double MIN_SAMPLE_WINDOW_1_SIZE = (granularity + (tolerance)) * 2.0;
		final double MIN_SAMPLE_WINDOW_2_SIZE = MIN_SAMPLE_WINDOW_1_SIZE * 2.0; 
		
		// The first sample window must be at MIN_SAMPLE_WINDOW_1_SIZE  
		if (sampleWindow1Time < MIN_SAMPLE_WINDOW_1_SIZE)		
			Event.PARSE_ERROR.issue("Sample window 1 size of " + Utilities.formatTime(sampleWindow1Time) + " is too small; it must be at least " + Utilities.formatTime(MIN_SAMPLE_WINDOW_1_SIZE));		
		
		// The first sample window must be at MIN_SAMPLE_WINDOW_1_SIZE  
		if (sampleWindow1Time < MIN_SAMPLE_WINDOW_2_SIZE)		
			Event.PARSE_ERROR.issue("Sample window 2 size of " + Utilities.formatTime(sampleWindow2Time) + " is too small; it must be at least " + Utilities.formatTime(MIN_SAMPLE_WINDOW_2_SIZE));				
						
		// Initialize the logger. 
		loggerThread = new ThroughputLoggerPrintThread(simulations, new PrintStream(new FileOutputStream(logFile)), granularity, tolerance, sampleWindow1Time, sampleWindow2Time);					
	}
	
	/**
	 * Starts the logger thread and the logger. 
	 */
	public void start()
	{			
		loggerThread.start();	
	}
	
	/**
	 * Shuts down the throughput logger followed by
	 * a shutdown of the logger thread. 
	 * 
	 * Releases pointed to the logger thread to allow 
	 * collection. 
	 * 
	 * @throws InterruptedException
	 */
	public void shutdown() throws InterruptedException
	{								
		if (loggerThread != null)
		{
			loggerThread.shutdown();	
			loggerThread.join(); 
		}
	}
	
	/**
	 * Sends a signal to stop the logger thread and 
	 * logger, but does not block until it has completed.
	 * 
	 * Releases the pointer to the logger thread to allow 
	 * for its collection. 
	 */
	public void kill()
	{			
		if (loggerThread != null)
		{
			loggerThread.kill(); 			
		}
	}
	
	/**
	 * Calculates the minimum number of transactions which should be 
	 * completed by each thread between calls to processReportingCycle
	 * 
	 * This function should not be called too frequently. Once every 
	 * sampling cycle is enough. 
	 * 
	 * @param simulation
	 * @return
	 */
	protected int getMinInterval(CacheSimulation simulation)
	{
		double transactionRate = count.get()/(Utilities.getTime() - startTime); 
		double transactionsPerLoggingPeriod = transactionRate * granularity; 
		double transcationsPerLoggingPeriodPerThread = transactionsPerLoggingPeriod / (double)simulation.getConfiguration().getNumThreads();  
		
		int minInterval = (int)Math.ceil(transcationsPerLoggingPeriodPerThread/2.0);
		
		minInterval = minInterval < 10 ? 10 : minInterval;
		
		return minInterval; 
	}
	
	/**
	 * Adds a (count, time) pair (new ThroughputLoggerThread.LogRequest) to the 
	 * log queue to be processed by the logger thread at a later time. The count 
	 * is the sum of all delta values for the hitory of the program, therefore each 
	 * simulation should take care to report all changes in count eventually.  
	 * 
	 * This function is not intended to be called every transaction but rather every 
	 * thread should call it every getMinInterval() transactions, recomputed 
	 * periodically.   
	 * 
	 * @param simulation
	 * @param delta
	 */
	protected void processReportingCycle(CacheSimulation simulation, long delta)
	{														
		loggerThread.add(loggerThread.new LogRequest(Utilities.getTime(), this.count.addAndGet(delta)));			
	}	
}
