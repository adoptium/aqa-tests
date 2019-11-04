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

import java.lang.reflect.InvocationTargetException;

import net.adoptopenjdk.casa.data_structures.AtomicDouble;
import net.adoptopenjdk.casa.data_structures.AtomicMutex;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.util.format.Alignment;
import net.adoptopenjdk.casa.util.format.CellException;
import net.adoptopenjdk.casa.util.format.RowException;
import net.adoptopenjdk.casa.util.format.Table;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Encapsulates a running cache simulation and related threads. 
 * 
 *  
 */
public class CacheSimulation
{		
	// Sample at most this number of transactions at a time
	private static final int MAX_SAMPLE_CYCLES = 10000;
	
	// Always sample at least this number of transactions 
	private static final int MIN_SAMPLE_CYCLES = 100;
	
	private static final double STARTUP_TIME_PER_THREAD = ParseUtilities.parseTime("500ms"); 

	// Preempt sampling if time exceeds this, in seconds 
	private static final double MAX_SAMPLE_TIME = ParseUtilities.parseTime("1s");
	
	// Report time windows which include these percentages of transactions. 
	private static final double[] THRESHOLDS = {99.999,99.99,99.9,99,95.00,90.00,50.00};
	
	/*
	 *  The precision of probabilistic events. These are computed as random integers (fixed point) for efficiency.
	 *  eg: 100000 supports percent probabilities down to 0.001%
	 */	
	private static final int RANDOM_PRECISION = 100000; 

	// The time allotted to spin-up threads and start this simulation, calculated from STARTUP_TIME_PER_THREAD 
	double startupTime;  	
	
	// Buckets to sort transaction times into 
	private TransactionTimeBuckets buckets; 
	
	// The absolute start time to the millisecond. 
	private volatile double absoluteStartTime; 
	
	// The start time of this simulation down to the nanosecond. 
	private volatile double startTime; 
	
	// This will bet set to either 0 or the time at which shutdown or kill was first called. 
	private double stopTime; 

	// The sum of all transaction times
	private AtomicDouble sumOfTransactionTimes;
	
	// The sum of all sampled transaction times squared
	private AtomicDouble sumOfTransactionTimesSquared;
		
	// The configuration from which this simulation was built 
	private CacheSimulationConfiguration configuration;
	
	// The caches that this simulation queries and updates. 
	private TableCache[] caches; 
	
	// Worker threads perform transactions on the caches. 
	private WorkerThread[] threads;  
	
	// The absolute minimum number of transactions to perform between logging. Updated regularly by thread 0. 
	private volatile int minTransactionsInReportingInterval = 100; 
	
	// The logger, shared amongst all simulations. 
	private ThroughputLogger logger; 

	// Locked before and during startup and during and after shutdown.  
	private AtomicMutex shutdownMutex;

	private CacheSimulations simulations; 
	

	private boolean[] populatedFlags;  
	
	/**
	 * Instantiates a new cache simulation, building essential structures. 
	 * 
	 * start() is called to start the simulation(). To stop the simulation, call 
	 * shutdown(). 
	 * 
	 * @param configuration
	 */
	protected CacheSimulation(CacheSimulationConfiguration configuration, CacheSimulations simulations) 
	{
		this.configuration = configuration;
		this.simulations = simulations; 
		
		this.startupTime =  (STARTUP_TIME_PER_THREAD * getConfiguration().getNumThreads());
		
		this.logger = simulations.getLogger();
		this.minTransactionsInReportingInterval = 100; 
		
		this.threads = null; 
		this.buckets = null; 
		
		shutdownMutex = new AtomicMutex(true);
		
		startTime = 0;
		absoluteStartTime = 0; 
		stopTime = 0; 	
		
		// Counters for tracking transaction counts and times. 
		sumOfTransactionTimes        = new AtomicDouble(0); 
		sumOfTransactionTimesSquared = new AtomicDouble(0);				
	}
	
	/**
	 * Executes a sampling cycle on the calling thread which also 
	 * much be passed as a parameter. 
	 * 
	 * @param thread
	 */
	protected void executeSimulationSamplingCycle(WorkerThread thread)
	{
		TableCache cache = getRandomCache(thread);
		
		/*
		 * Select a random upper limit between MIN_SAMPLE_CYCLES 
		 * and MAX_SAMPLE_CYCLES for the number of transactions to 
		 * perform this iteration. The actual number may be less 
		 * if the iteration runs for longer than MAX_SAMPLE_TIME
		 * or a transaction is interrupted.   
		 */
		final int maxCycles = thread.RANDOM_INSTANCE.nextInt(MAX_SAMPLE_CYCLES - MIN_SAMPLE_CYCLES) + MIN_SAMPLE_CYCLES; 
				
		/*
		 * Update if a random integer between 0 and RANDOM_PRECISION-1 is not greater than updateThreshold 
		 * eg: 50.000% -> 50000/100000 so a random int between 0 and 99999  is generated and the boolean is 
		 * true if it is 50000 or less.  
		 */
		final int updateThreshold = (int)Math.round(cache.getConfiguration().getUpdateProbability() * (double)RANDOM_PRECISION); 
		
		// Mark the start time of the sampling interval
		final double intervalStartTime = Utilities.getTime(); 				
	
		// Thread 0 will recompute the logger reporting interval 
		if (logger != null && thread.getID() == 0)			
			minTransactionsInReportingInterval = logger.getMinInterval(this);
		
		// Check to see if the cache has just become populated. 
		if (!populatedFlags[cache.getID()] && thread.getID() == 0 && cache.isPopulated())
		{
			populatedFlags[cache.getID()] = true;
			
			Event.NOTICE.issue(" cache for table " + cache.getConfiguration().getIDString()  + " populated." );
		}
		
		// Transaction count for this simulation sampling cycle.  
		int transactionsInSimulationCycle = 0;
		
		// Transaction count since last attempt to report in this cycle. 
		int transactionsInCurrentReportingInterval = 0;
		
		// Sample some number of transactions 
		do
		{			
			/* Generate a random integer to compute the two 
			 * random booleans for the two probabilistic
			 * boolean branch points: whether to update or query 
			 * and whether to expose  
			 */
			final int random = thread.RANDOM_INSTANCE.nextInt(RANDOM_PRECISION); 
			
			// Decide whether or not to perform an update based on the generated random. 
			final boolean update = random <= updateThreshold; 
			
			// Attempt to perform the transaction					
			if (update)
			{						
				// Update a random row and then release the cache semaphore 
				cache.updateRandomRow(thread);							
			}
			else
			{															
				// Query a random row and release the query semaphore 
				QueryResponse response = cache.queryRandomRow(thread);																						
								
				// Decide whether or not to expose the response. 
				final boolean expose = random <= 1;											
				
				/*
				 * if exposing, we place the response in a 
				 * global variable  						
				 */
				if (expose)
				{						
					Main.exposeResponse(response);
				}						
			}
			
			transactionsInSimulationCycle++; 
			
			// Don't start reporting until the cache is started. 
			if (isStarted())
			{
				transactionsInCurrentReportingInterval++;
						
				// Check to see if it's time to poll the logger. 
				if (logger != null && transactionsInCurrentReportingInterval >= minTransactionsInReportingInterval)
				{	
					logger.processReportingCycle(this, transactionsInCurrentReportingInterval);
					transactionsInCurrentReportingInterval = 0;
				}
			}
			
			// Preempt if more than one second is expended, check every 100 transactions
			if (transactionsInSimulationCycle > MIN_SAMPLE_CYCLES && transactionsInSimulationCycle % MIN_SAMPLE_CYCLES == 0)
			{									
				// Check the time and preempt if we've exceeded the maximum sample time. 
				if (Utilities.getTime() - intervalStartTime > MAX_SAMPLE_TIME)  
				{						
					break;							
				}							
			}
			
			// On interrupt, restore the interrupted flag and end this sampling cycle. 
			if (Thread.interrupted())
			{
				thread.interrupt();
				break; 
			}			
		}					
		while (transactionsInSimulationCycle < maxCycles);
		
		// Start tracking stats only once the cache has *officially* started. 
		if (isStarted())
		{
			// Log the remaining transactions from this cycle. 
			if (transactionsInCurrentReportingInterval > 0)
				logger.processReportingCycle(this, transactionsInCurrentReportingInterval);
						
			// Compute the time taken for this iteration. 
			final double intervalTime = Utilities.getTime() - intervalStartTime;
				
			// Compute the average transaction time for this iteration.  
			final double transactionTime = intervalTime/transactionsInSimulationCycle;
								
			// Store sum of times to compute average. 
			sumOfTransactionTimes.add(intervalTime);			
			
			// Store sum of squares to compute standard deviation.   
			sumOfTransactionTimesSquared.add((transactionTime * transactionTime) * transactionsInSimulationCycle);									
			
			// Add the number of completed transactions to the appropreate bucket
			buckets.countTransactions(transactionTime, transactionsInSimulationCycle);	
		}
	}
	
	/**
	 * Returns true if the simulation has run and is finished. 
	 * 
	 * @return
	 */
	protected boolean isFinished()
	{
		return isStarted() && shutdownMutex.isLocked(); 
	}
	
	/**
	 * Returns true if the simulation is currently running.  
	 * 
	 * @return
	 */
	protected boolean isRunning()
	{
		return isStarted() && !isFinished(); 
	}
	
	/**
	 * Returns true if the simulations has started. 
	 * 
	 * Call isRunning() to find out if it is currently running. 
	 * 
	 * @return
	 */
	protected boolean isStarted()
	{
		return (startTime > 0 && absoluteStartTime > 0);
	}
	
	/**
	 * Returns the worker threads. 
	 * 
	 * @return
	 */
	protected WorkerThread[] getWorkerThreads()
	{
		return threads; 
	}

	/**
	 * Computes the estimated end time of this simulation based on the 
	 * configuration (if the simulation has not yet started) or the 
	 * actual start time and configured duration if the simulation 
	 * has actually started.  
	 * 
	 * @return
	 */
	protected double getEndTime()
	{	
		double endTime; 
		
		// Simulation not started yet. Report application start time + configured end time
		if (!isStarted())  
		{
			endTime = simulations.getStartTime() + startupTime + getConfiguration().getEndTime();			
		}
		// Otherwise, report the actual start time plus the configured duration.
		else  
		{
			endTime = (getStartTime() + getConfiguration().getDuration());
		}			
		
		return endTime;
	}
	
	/**
	 * Gets the configuration for this simulation as parsed from 
	 * the config file. 
	 * 
	 * @return
	 */
	protected CacheSimulationConfiguration getConfiguration()
	{
		return configuration; 
	}
	
	/**
	 * Gets the absolute start time of the simulation in seconds, 
	 * accurate to the millisecond 
	 * 
	 * @return
	 */
	protected double getAbsoluteStartTime()
	{
		return absoluteStartTime; 
	}
	
	/**
	 * Starts the simulation 
	 * 
	 * @throws InterruptedException
	 */
	protected void start() throws InterruptedException
	{	
		synchronized (this)
		{				
			// Buckets to sort transaction times into for later analysis and reporting. 
			buckets = new TransactionTimeBuckets(ParseUtilities.parseTime("10ns"), ParseUtilities.parseTime("1s")); 

		
			threads = new WorkerThread[configuration.getNumThreads()];
			
			for (int i = 0; i < threads.length; i++)		
				threads[i] = new WorkerThread(this, i);
			
			// Create caches 
			try 
			{
				// Create a new cache for each table. 
				caches = new TableCache[configuration.getTableConfigurations().size()]; 
				
				for (int i = 0; i < configuration.getTableConfigurations().size(); i++)
					caches[i] = TableCacheType.getNewCache(i, configuration.getTableConfigurations().get(i), this);
			} 
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) 
			{
				Event.FATAL_ERROR.issue(e, "Failed to initialize table cache");
				return;
			}
			
			
			populatedFlags = new boolean[caches.length];
						
			for (int i = 0; i < caches.length; i++)
			{
				populatedFlags[i] = caches[i].isPopulated(); 
			}
						
			// Calculate a such that the sum of i * a over 0 to i < threads.length equals startupTime
			double startupMultiplier = 2 * (startupTime) / (threads.length * (threads.length + 1)); 
			
			Event.NOTICE.issue("simulation " + configuration.getIDString() + ": starting threads...");
								
			// Start the worker threads
			for (int i = 0; i < threads.length; i++)
			{								
				threads[i].start();
			
				Utilities.idle(startupMultiplier * i);				
			}
			
			Event.NOTICE.issue("simulation " + configuration.getIDString() + ": all threads started.");
			
			// Stop time is initialized to 0 to indicate that the simulation has not yet stopped. 
			startTime = Utilities.getTime();
			absoluteStartTime = Utilities.getAbsoluteTime();	
			
			shutdownMutex.release();				
		}
	}
	
	/**
	 * Shuts down the simulation without blocking.  
	 */
	protected void kill()
	{	
		synchronized (this)
		{
			if (shutdownMutex.acquire())
			{
				// Interrupt all worker threads. 
				for (WorkerThread w : threads)		
					w.interrupt();
							
				// Nonzero stop times signify that the simulation has stopped. 
				stopTime = Utilities.getTime(); 
				
				if (caches != null)
				{
					Main.disableStatus(); 
					Main.out.println(toString());							
					Main.enableStatus();									
				}
			}
		}
	}
	
	/**
	 * Shuts down the simulation and blocks until all threads 
	 * have stopped. 
	 * 
	 * @throws InterruptedException
	 */
	protected void shutdown() throws InterruptedException
	{	
		synchronized (this)
		{
			if (shutdownMutex.acquire())
			{								
				for (WorkerThread w : threads)		
				{				
					w.interrupt();			
					w.join();
				}
											
				stopTime = Utilities.getTime();
			
				if (caches != null)
				{
					for (TableCache cache : caches)			
						cache.shutdown();
					
					Main.disableStatus(); 
					Main.out.println(toString());							
					Main.enableStatus();												
				}							
			}		
		}
	}
	
	/** 
	 * Returns a count of the number of completed transactions 
	 * at the time of the call. This count is updated 
	 * after every transaction. 
	 * 
	 * @return
	 */
	protected long getCompletedTransactionCount()
	{
		long count = 0; 
		
		for (int i = 0; i < caches.length; i++)
			count += caches[i].getNumCompletedTransactions(); 
		
		return count; 
	}
	
	/** 
	 * Gets the time at which the simulation started as returned 
	 * by Utilities.getTime().  
	 * 
	 * @return
	 */
	protected double getStartTime()
	{
		return startTime; 
	}
	
	/**
	 * If the simulation is running, this returns the time 
	 * since the simulation started. If the simulation has stopped, 
	 * the stop time minus the start time is returned, equating to 
	 * the time between the start and stop of the simulation.  
	 * 
	 * @return
	 */
	protected double getElapsedTime()
	{
		/*
		 * Stop time is initialized to 0 and set when the simulation 
		 * shuts down.  
		 */
		if (startTime == 0)
			return 0; 
		else if (stopTime < startTime)		
			return Utilities.getTime() - startTime;		
		else 		
			return stopTime - startTime; 		
	}
	
	/**
	 * Gets the mean transaction time. 
	 * 
	 * @return
	 */
	protected double getTransactionTimeMean()
	{
		return sumOfTransactionTimes.get() / (double)getCompletedTransactionCount();
	}
	
	/**
	 * Gets the variance of the transaction times, sampled. Actual 
	 * variance may be higher, depending on the sample interval.  
	 * 
	 * The quotient of the sum of squares and transaction count 
	 * minus the square of the mean.
	 * 
	 * @return
	 */
	protected double getTransactionTimeVariance()
	{
		double mean =  getTransactionTimeMean(); 
		
		// The quotient of the sum of squares and transaction count minus the square of the mean.  
		return (sumOfTransactionTimesSquared.get() / (double)getCompletedTransactionCount()) - (mean * mean);
	}
	
	/**
	 * Picks a random cache from the caches array. 
	 * 
	 * @return
	 */
	protected TableCache getRandomCache(WorkerThread thread)
	{
		return caches[thread.RANDOM_INSTANCE.nextInt(caches.length)]; 
	}
	
	/**
	 * Gets the (sampled) standard deviation. 
	 * 
	 * @return
	 */
	protected double getTransactionTimeStandardDeviation()
	{
		// Standard deviation is the square root of the variance.  
		return Math.sqrt(getTransactionTimeVariance());		
	}
	
	/**
	 * Calls toString(StringBuilder builder) with a new string builder and returns 
	 * the string returned by builder.toString() following the call.  
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder(); 
		
		toString(builder); 
		
		return builder.toString(); 
	}
	
	/**
	 * Produces a summary of statistics pertaining to the completed cache simulation  
	 * 
	 * @param builder
	 */
	private void toString(StringBuilder builder)
	{	
		try 
		{
			Alignment[] alignment = new Alignment[] {Alignment.LEFT, Alignment.RIGHT};				
			Table table = new Table(); 							
			table.addHeadings(table.new Headings(
				new String[] {"CACHE SIMULATION", configuration.getIDString() + ""}, 
				new int[] {24, 20}, 
				alignment, true, false
			));		
		
			// Append the total number of completed transactions 
			table.addRow(table.new Row(new String[] {"Total Transactions", getCompletedTransactionCount() + ""}, alignment));
			
			// If there are any complete transactions, generate a report.  
			if (getCompletedTransactionCount() > 0)			 
			{				
				// Mean and standard deviation 
				table.addRow(table.new Row(new String[] {"Mean Transaction Time", Utilities.formatTime(getTransactionTimeMean(), 8)}, alignment));
				table.addRow(table.new Row(new String[] {"  Standard Deviation", Utilities.formatTime(getTransactionTimeStandardDeviation(), 8)}, alignment));
				table.addRow(table.new Row(new String[] {"Average Throughput", String.format("%.8g", getCompletedTransactionCount() / (double)getElapsedTime()) + "tr/s"}, alignment));
				table.addRow(table.new Row(new String[] {"  per Thread", String.format("%.8g", (getCompletedTransactionCount() / (double)getElapsedTime()) / (double)configuration.getNumThreads()) + "tr/s"}, alignment));
				
				table.addLine();
				
				// Threshold header 
				alignment = new Alignment[] {Alignment.LEFT, Alignment.LEFT};
				table.addRow(table.new Row(new String[] {"% OF TRANSACTIONS", "COMPLETE WITHIN" + ""}, alignment));
				table.addLine();
				alignment = new Alignment[] {Alignment.RIGHT, Alignment.RIGHT};
								
				// Proportion of transactions longer than the value of bucket 0 rounded to 4 decimal places. 
				double longTransactionProportion = Utilities.roundDouble(buckets.getProportionLongTransactions(), 4);
				
				/* 
				 * total x 0.0001 or more transactions are larger than the place 
				 * value of bucket 0, report it. Otherwise, ignore the long 
				 * transactions 
				 */
				if (longTransactionProportion > 0)
				{
					table.addRow
					(
						table.new Row(new String[] {Utilities.formatProportionAsPercent(longTransactionProportion, 3), 
						"> " + Utilities.formatTime(buckets.getLargestPlaceValue())}, alignment)
					);							
				}
				
				// Add thresholds 
				for (int thresholdIndex = 0; thresholdIndex < THRESHOLDS.length; thresholdIndex++)				
				{	
					// Threshold as a proportion 
					double threshold = THRESHOLDS[thresholdIndex] / 100.0;
					
					// The time window within which the target threshold proportion of transactions occur. 
					double timeWindow = buckets.getThresholdTime(threshold);
						
					// Add a row to the table for the threshold
					table.addRow(table.new Row(new String[] {Utilities.formatProportionAsPercent(threshold, 5), Utilities.formatTime(timeWindow)}, alignment));							 											
				}		
			}
										 		
			table.appendToStringBuilder(builder);
			
			builder.append("\n");
			
			// Create another table to summarize the activity of each table. 			
			alignment = new Alignment[] {Alignment.LEFT, Alignment.LEFT, Alignment.LEFT, Alignment.LEFT};				
			table = new Table(); 							
			table.addHeadings(table.new Headings(
				new String[] {"Table ID", "Hits", "Misses", "Hit Rate"}, 
				new int[] {9, 10, 10, 9}, 
				alignment, true, false
			));	
			
			table.addLine();
			
			alignment = new Alignment[] {Alignment.RIGHT, Alignment.RIGHT, Alignment.RIGHT, Alignment.RIGHT};

			// Produce a summary of activity for each table including hit/miss stats
			for (TableCache cache : caches)						
				table.addRow(table.new Row(new String[] {cache.getConfiguration().getIDString(), cache.getNumHits() + "", cache.getNumMisses() + "", Utilities.formatProportionAsPercent(cache.getHitRate(), 3)}, alignment));				
			
			table.addLine(); 	 		
			table.appendToStringBuilder(builder);			
		} 
		catch (RowException | CellException e) 
		{
			Event.FATAL_ERROR.issue(e);
		}
		catch (Throwable e)
		{
			Event.FATAL_ERROR.issue(e);
		}
	};
}

