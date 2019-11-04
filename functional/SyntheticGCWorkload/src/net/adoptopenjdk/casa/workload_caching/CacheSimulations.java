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

import java.util.ArrayList;

import net.adoptopenjdk.casa.data_structures.AtomicMutex;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.util.format.ProgressBar;

/**
 * Encapsulates all running simulations. Runs each in a 
 * separate daemon thread and manages starting and stopping 
 * these threads.
 * 
 *  
 *
 */
public class CacheSimulations 
{	
	private static final int PROGRESS_BAR_LENGTH = 24;
	
	// The time to wait between progress bar updates, in seconds. 
	private static final double STATUS_UPDATE_PERIOD = 1.0; 
	
	private final ArrayList<CacheSimulation> simulations; 
	
	// The start time of this simulation down to the nanosecond. 
	private double startTime; 
		
	// The absolute start time to the millisecond. 
	private double absoluteStartTime;
		
	// Threads that manage each simulation's startup and shutdown sequence 
	private Thread[] simulationDaemonThreads; 
	
	// Handles fine-grained logging of transaction rates. 
	private ThroughputLogger logger;
	
	// Locked during startup and shutdown 
	private AtomicMutex shutdownMutex; 
	
	// Status output is inhibited when this mutex is locked. 
	private AtomicMutex statusInhibitMutex;
	
	// Always print newlines after status messages instead of returning the cursor to the beginning of the line. 
	private boolean alwaysUseNewLines;
	
	/**
	 * Creates new simulations from the given configurations and initializes them. 
	 * 
	 * @param configurations
	 * @param throughputLogFile
	 * @param througputGranularity
	 * @param samplingWindow1Time
	 * @param samplingWindow2Time
	 */
	protected CacheSimulations(CacheSimulationConfigurations configurations, String throughputLogFile, double throughputGranularity, double samplingWindow1Time, double samplingWindow2Time, boolean alwaysUseNewLines)
	{	
		logger = null; 		
		this.alwaysUseNewLines = alwaysUseNewLines; 
		this.shutdownMutex = new AtomicMutex(true);
		this.statusInhibitMutex = new AtomicMutex(false); 
		this.simulations = new ArrayList<CacheSimulation>();
		
		// Initialize the logger. 
		if (throughputLogFile != null)
		{
			try 				
			{
				logger = new ThroughputLogger(this, throughputLogFile, throughputGranularity, samplingWindow1Time, samplingWindow2Time);				
			} 
			catch (Throwable e) 
			{
				logger = null; 											
				Event.FATAL_ERROR.issue(e, "failed to initialize throughput logger");					
			}				
		}
		
		// Initialize simulations
		for (CacheSimulationConfiguration configuration : configurations.getConfigurations())
		{
			simulations.add(new CacheSimulation(configuration, this));
		}
											
		this.simulationDaemonThreads = new Thread[simulations.size()];
	}
	
	/**
	 * Returns true if all simulations are finished according to their 
	 * isFinished() method. 
	 * 
	 * @return
	 */
	protected boolean isFinished()
	{
		for (int i = 0; i < simulations.size(); i++)
		{
			if (!simulations.get(i).isFinished())
				return false;
		}
		
		return true; 
	}
	
	/**
	 * Starts simulations 
	 * 
	 * @throws InterruptedException
	 */
	protected void run() throws InterruptedException
	{
		// Don't allow start to run concurrently with shutdown or kill
		synchronized (this) 
		{			
			// Record the start time. 
			startTime = Utilities.getTime();			
			absoluteStartTime = Utilities.getAbsoluteTime() - getElapsedTime();
			
			// Construct daemon threads that will manage simulations 
			for (int i = 0; i < simulationDaemonThreads.length; i++)
			{				 				
				simulationDaemonThreads[i] = new SimulationDaemonThread(simulations.get(i));
			}
						
			// Start all simulation daemon threads, in effect starting the simulations 
			for (int i = 0; i < simulationDaemonThreads.length; i++)						 
			{
				simulationDaemonThreads[i].setDaemon(true);				
				simulationDaemonThreads[i].start(); 							
			}
												
			// Start the logger
			if (logger != null)
				logger.start(); 
			
			// Allow shutdown 
			shutdownMutex.release(); 			
		}	
		
		// Print status updates until the simulations are finished. 
		monitor(); 		
	}
	
	/**
	 * Enables updates to the status output.  
	 * 
	 * @param mainShutdownMutex
	 */
	protected void enableStatus(AtomicMutex mainShutdownMutex)
	{		
		if (!isFinished())				
			statusInhibitMutex.release();							
	}
	
	/**
	 * Disables updates to the status output and outputs a 
	 * newline. (Progress bar) 
	 * 
	 * @param mainShutdownMutex
	 */
	protected void disableStatus(AtomicMutex mainShutdownMutex)
	{		
		// Keep trying to get the status mutex 
		while (!statusInhibitMutex.acquire())
		{
			// Stop trying to acquire the inhibit mutex if the simulation is shutting down
			if (isFinished())
				return;
		}
		
		/*
		 *  Print a newline following the last status message so that
		 *  the next line of output appears below the last status message.  
		 */		 
		synchronized(Main.out)
		{	
			Main.out.println(); 		
		}
	}
	
	/**
	 * Continually monitors status of running simulations and updates progress 
	 * bar output until all simulations are completed. 
	 */
	private void monitor()
	{	
		// The monitor prints a running average for each simulation 
		long lastCounts[] = new long[simulations.size()]; 
		double lastCountTimes[] = new double[simulations.size()];
		
		// Create and display a progress bar until the cache simulation is complete.  		
		try 
		{
			ProgressBar progressBar = new ProgressBar(getStartTime(), PROGRESS_BAR_LENGTH, getDuration(), true, true, alwaysUseNewLines);
			
			// Continually print updates to the console until the simulations are finished. 
			while (!isFinished() && !Thread.interrupted())
			{										
				synchronized(statusInhibitMutex)
				{
					if (statusInhibitMutex.acquire())
					{
						// Update the scale up the progress bar with the latest available duration estimate 
						progressBar.setScale(getDuration());
						
						synchronized(Main.out)
						{																		
							Main.out.print(progressBar.update(getElapsedTime()));
							Main.out.print(" | " + Utilities.formatDataSize(Runtime.getRuntime().freeMemory()));
								
							// Print a running average for each (running) simulation 
							for (int i = 0; i < simulations.size(); i++)
							{
								double momentaryAgerage = 0;
								
								// If the simulation is running, update the momentary average. 
								CacheSimulation simulation = simulations.get(i);								
								if (simulation.isRunning())
								{
									long count = simulation.getCompletedTransactionCount(); 
									double time = Utilities.getTime(); 
																	
									long deltaC = count - lastCounts[i];
									double deltaT = time - lastCountTimes[i];   

									momentaryAgerage = deltaC / (double) deltaT;  
									
									lastCounts[i] = count; 
									lastCountTimes[i] = time; 
								}
								
								Main.out.print(" | " + Math.round(momentaryAgerage) + "tr/s");
							}

							Main.out.print("    ");
						}
						
						
						statusInhibitMutex.release();
					}
				}
				
				Utilities.idle(STATUS_UPDATE_PERIOD); 								
			}
			
			
			synchronized(Main.out)
			{
				Main.out.println(progressBar.update(getElapsedTime()));
			}
		} 
		catch (InterruptedException e) 
		{ 
			// If we're already shutting down, just return. 
			if (shutdownMutex.isLocked())
				return; 
		}
	}
	
	/**
	 * Signals all activity to stop but does not wait for 
	 * it to do so. This method is used to perform an 
	 * immediate unclean shutdown.  
	 */
	protected void kill()
	{
		synchronized (this) 
		{	
			if (shutdownMutex.acquire())
			{				
				// Stop the logger 
				if (logger!= null)
					logger.kill();
				
				// Stop all simulation daemon threads 
				for (int i = 0; i < simulationDaemonThreads.length; i++)					 
					simulationDaemonThreads[i].interrupt();
				
				// Stop all simulations 
				for (CacheSimulation simulation : simulations)
					simulation.kill(); 								
			}
		}		
	}
	
	/**
	 * Shuts down the simulations and waits for them to join. Will 
	 * block until startup has completed and will only perform shutdown 
	 * for the first caller.  
	 * 
	 * @throws InterruptedException
	 */
	protected void shutdown() throws InterruptedException
	{		
		// Don't allow startup or kill or shutdown to run concurrently  
		synchronized (this) 
		{			
			// Only allow the first thread to call shutdown to execute the procedure. 
			if (shutdownMutex.acquire())
			{
				for (Thread thread : simulationDaemonThreads)
				{	
					// Block until the thread is done, then interrupt it and wait for it to complete. 
					synchronized(thread)
					{
						thread.interrupt();
						thread.join();
					}
				}

				// Stop the logger. 
				if (logger != null)				
					logger.shutdown();				
			}
		}
	}

	/**
	 * Gets the logger instance, or null if there is no logger running. 
	 * 
	 * @return
	 */
	protected ThroughputLogger getLogger()
	{
		return logger; 
	}
	
	/**
	 * Returns the projected end time of the last cache 
	 * simulation from the start of the application or the 
	 * currentTime, whichever is later. 
	 * 
	 * @return
	 */
	private double getDuration()
	{
		double duration = Utilities.getTime() - getStartTime();
		
		// Find the latest end time...that's the duration of the simulations
		for (CacheSimulation simulation : simulations)
		{
			double endTime = simulation.getEndTime() - getStartTime(); 
			if (endTime > duration)
				duration = endTime;  
		} 
		
		return duration; 
	}

	/**
	 * Gets the start time of the simulations down to the nanosecond as 
	 * reported by Utilities.getTime() (or System.nanoTime())
	 * 
	 * @return
	 */
	protected double getStartTime()
	{
		return startTime;
	}
	
	/**
	 * Gets the start time down to the millisecond as an absolute time   
	 * 
	 * @return
	 */
	protected double getAbsoluteStartTime()
	{
		return absoluteStartTime; 
	}
	
	/**
	 * Gets the elapsed time down to the nanosecond. 
	 * 
	 * @return
	 */
	protected double getElapsedTime()
	{
		return Utilities.getTime() - startTime; 
	}
	
	/**
	 * Appends configuration information from all simulations to the given 
	 * StringBuilder. 
	 * 
	 * @param builder
	 */
	protected void toString(StringBuilder builder)
	{
		for (CacheSimulation simulation : simulations)
		{	
			simulation.getConfiguration().toString(builder);
			builder.append("\n");
		}
	}
	
	/**
	 * Returns configuration information for the simulations as a human 
	 * readable table. 
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder(); 
		
		toString(builder);
		
		return builder.toString(); 
	}
	
	/**
	 * Gets the cache simulation at the given ID, 0-referenced 
	 * 
	 * @param ID
	 * @return
	 */
	private CacheSimulation getSimulation(int ID) 
	{
		return simulations.get(ID);
	}
	
	/**
	 * Gets the total number of threads in all currently running simulations. 
	 * 
	 * @return
	 */
	protected int getNumThreads() 
	{
		int count = 0; 
		
		for (int i = 0; i < simulations.size(); i++)
		{
			CacheSimulation simulation = getSimulation(i);
			
			if (simulation.isRunning())
				count += simulation.getConfiguration().getNumThreads();
		}
						
		return count; 		
	}
}
