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
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import net.adoptopenjdk.casa.data_structures.AtomicMutex;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.util.format.Alignment;
import net.adoptopenjdk.casa.util.format.CellException;
import net.adoptopenjdk.casa.util.format.RowException;
import net.adoptopenjdk.casa.util.format.Table;
import net.adoptopenjdk.casa.workload_sessions.configuration.Configuration;
import net.adoptopenjdk.casa.workload_sessions.configuration.WorkloadConfiguration;
import net.adoptopenjdk.casa.xml_parser.ParseError;

/**
 * Runs Workloads in parallel. 
 * 
 *  
 */
public class Workloads 
{
	// Workload daemon threads 
	private Thread[] workloadThreads;
	
	// The running workloads; won't be set until workloads are actually started.  
	private Workload[] workloads;
	
	// Configurations of all workloads 
	private WorkloadConfiguration[] workloadConfigurations;
		
	// The global configuration 
	private Configuration configuration; 
	
	// Prints progress indicator
	private final StatusThread statusThread;	
	
	private double startTime; 
	
	// The actual expected duration of the workloads. 
	private double duration; 
	
	/**
	 * Creates a new Workloads instance including a watchdog thread, but does 
	 * not parse any workloads or start any threads. 
	 * 
	 * Call execute() to start executing workloads. 
	 * 
	 * @param configuration
	 */
	protected Workloads (Configuration configuration) throws ParseError
	{	
		this.configuration     = configuration;
		
		// Parse the workload configurations 
		workloadConfigurations = configuration.getWorkloadConfigurations(); 		
		
		// Figure out how long this is going to take 
		duration               = calculateDuration(); 		
		
		// Init the status thread 
		statusThread           = new StatusThread();				 		
	}
	
	/**
	 * Processes workloads. May shutdown successfully or shutdown with an 
	 * error. 
	 * 
	 * May throw the InterruptedException if shutdown was premature.    
	 * 
	 * @throws InterruptedException - thrown if execution stops prematurely.  
	 */
	protected void execute() throws InterruptedException
	{
		// Populate the workloads array 
		initializeWorkloads();
	
		// Print configuration 
		Main.out.println(this);
		
		// Mark start time
		startTime = Utilities.getTime();
		
		// Start the workloads, each in a daemon thread.  
		startWorkloadDaemonThreads();
		
		// Start the status thread
		statusThread.start();
		statusThread.resumeOutput(); 
		
		// Wait for the duration of the workloads, or until interrupted. 
		Utilities.idle(duration);
		
		// Shutdown the workloads. 
		Event.SUCCESS.issue(); 
	}
	
	/**
	 * Shuts down the running workloads.  
	 */
	protected void shutdown() throws InterruptedException
	{			
		// Stop printing status updates		
		statusThread.interrupt();
		statusThread.join(); 
		
		// Signal workload shutdown. 
		for (Thread thread : workloadThreads)
			if (thread != null)
				thread.interrupt(); 

		// Wait for workloads to finish. 
		for (Thread thread : workloadThreads)		
			if (thread != null)			
				thread.join();		
	}
	
	/**
	 * Shuts down the running Workload and RuntimeWatchdog without blocking 
	 */	
	protected void kill() 
	{		
		// Stop printing status updates
		statusThread.suppressOutput();
		statusThread.interrupt();
		
		// Interrupt all threads
		for (Thread thread : workloadThreads)
			if (thread != null)
				thread.interrupt(); 
	}
	
	/**
	 * Calculates the total duration of the workloads. 
	 * 
	 * @return
	 */
	private double calculateDuration()
	{
		double maxDuration = 0; 
		
		// Find the workload with the longest duration + startTime. 
		for (int i = 0; i < configuration.getNumWorkloads(); i++) 
		{	
			double endTime = workloadConfigurations[i].getEndTime(); 
			
			if (endTime > maxDuration)
				maxDuration = endTime; 
		}
		
		// Cap duration at configuration.getMaxDuration. 
		if (maxDuration <= configuration.getMaxDuration())
			return maxDuration;
		else 
			return configuration.getMaxDuration();
	}
	
	/**
	 * Creates new Workload objects and populates the 
	 * workloads array with them. 
	 */
	private void initializeWorkloads()
	{
		workloads = new Workload[configuration.getNumWorkloads()];	
						
		// Iterate over all workloads 
		for (int i = 0; i < configuration.getNumWorkloads(); i++) 
		{	
			// Construct the workload from the given configuration. 
			workloads[i] = new Workload(workloadConfigurations[i]);	
			
			if (workloads[i].isRepeatingWorkload()) {
				//we must copy over the workloads in as many workloads as needed, and modify the start and end times
				workloads[i].createRepeatingPayloads();
			}			
		}
	}

	/**
	 * Iterates over the workloadConfigurations and runs each workload. 
	 * 
	 * @throws InterruptedException - thrown on premature shutdown
	 */
	private void startWorkloadDaemonThreads() throws InterruptedException
	{		
		workloadThreads = new Thread[configuration.getNumWorkloads()];
		
		// Create daemon threads for workloads 
		for (int i = 0; i < configuration.getNumWorkloads(); i++) 
		{	
			final Workload workload = workloads[i];
			
			workloadThreads[i] = new Thread() 
			{					
				public void run()
				{						
					try 
					{								

						// Delay startup until the workload's designated startup time. 
						Utilities.idle(workload.getWorkloadConfiguration().getStartTime());
							
						// Start the workload. (returns immediately) 
						workload.startup();

						try
						{
							// Check for interrupt 
							if (Thread.interrupted())
								throw new InterruptedException();
							
							// Go to sleep for the duration of the workload. 
							Utilities.idle(workload.getWorkloadConfiguration().getDuration());

							// Shutdown the workload.
							workload.shutdown();						
						} 
						catch (InterruptedException e) 
						{ 
							workload.shutdown(); 
							throw e; 
						}
					}
					
					catch (InterruptedException e)
					{					
						return; 
					}
					// Something bad has happened, exit on a runtime exception. 
					catch (Throwable e)
					{
						Event.RUNTIME_EXCEPTION.issue(e, "In workload daemon thread."); 
					}
				}
			};
		}
		
		// Start workload daemon threads. 
		for (int i = 0; i < configuration.getNumWorkloads(); i++)
		{
			workloadThreads[i].setDaemon(true);
			workloadThreads[i].start();
		}
	}
	
	/**
	 * Sums the payment rates of all workloads and returns the result. 
	 * 
	 * @return a payment rate in bytes per second. 
	 */
	protected double getPaymentRate()
	{
		double rate = 0; 		
		for (Workload workload : workloads) 		
			rate += workload.getPaymentRate(); 
		
		return rate; 
	}
	
	/**
	 * Gets the total average allocation data rate of all workloads since the 
	 * start of the run
	 * 
	 * @return the data rate in bytes per second 
	 */
	protected double getDataRate()
	{
		double rate = 0; 		
		for (int i = 0; i < workloads.length; i++) 		
			rate += workloads[i].getDataRate(); 
		
		return rate;
	}
	
	/**
	 * Gets the total throughput of all workloads. 
	 * 
	 * @return the throughput in bytes
	 */
	protected double getThroughput()
	{
		double throughput = 0; 		
		for (Workload workload : workloads) 		
			throughput += workload.getThroughput(); 
		
		return throughput ;
	}
	
	/**
	 * Gets the amount of time (in seconds) since this Workloads started.   
	 * 
	 * @return
	 */
	private double getElapsedTime()
	{
		return Utilities.getTime() - startTime;
	}
	
	/**
	 * Stops the status thread from printing updates. 
	 */
	protected void suppressStatusOutput()
	{
		statusThread.suppressOutput();
	}
	
	/**
	 * Allows the status thread to resume printing updates. 
	 */
	protected void resumeStatusOutput()
	{
		statusThread.resumeOutput();
	}
	
	protected int getNumPayloadbuilderSets()
	{
		int count = 0; 
		for (int i = 0; i < workloads.length; i++)
			count += workloads[i].getPayloadBuilderSets().length;
		return count; 
	}
	
	/**
	 * Produces a String with configuration information on the workloads and global settings. 
	 */
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		// Add global configuration information 
		try 
		{
			Alignment[] alignment = new Alignment[] {Alignment.LEFT, Alignment.RIGHT};
			Table table = new Table(); 			
			Table.Headings headings;
			headings = table.new Headings(
					new String[] {"CONFIGURATION ", new File(configuration.getFilename()).getName()}, 
					new int[] {24, 20}, 
					alignment, true, false
			);
			table.addHeadings(headings);												
			table.addRow(table.new Row(new String[] {"Free Heap", Utilities.formatDataSize(Runtime.getRuntime().freeMemory()) + "/" + Utilities.formatDataSize(Runtime.getRuntime().maxMemory())}, alignment));				
			table.addRow(table.new Row(new String[] {"Maximum duration", Utilities.formatTime(configuration.getMaxDuration())}, alignment));
			table.addRow(table.new Row(new String[] {"Sataus update period", Utilities.formatTime(configuration.getStatusUpdatePeriod())}, alignment));
			table.appendToStringBuilder(stringBuilder);			
		} 
		catch (RowException | CellException e) 
		{
			Event.FATAL_ERROR.issue(e);
		}
				
		// Add all workload configuration information 
		for (int i = 0; i < configuration.getNumWorkloads(); i++) 
		{
			// Print the workload configuration
			stringBuilder.append("\n");
			stringBuilder.append(workloads[i]);
		}
		
		return stringBuilder.toString(); 
	}
	
	/**
	 * A thread that displays status updates to the user. 
	 *
	 * @author Andrew Somerville <andrew.somerville@unb.ca>
	 */
	private class StatusThread extends Thread 
	{	
		private AtomicMutex printMutex; 
		private AtomicBoolean stopped; 
		private Table outputTable; 
			
		protected StatusThread()
		{			
			outputTable = new Table(); 							
			printMutex  = new AtomicMutex(true);
			stopped     = new AtomicBoolean(true); 
			setDaemon(true);					
		}	
		
		/**
		 * Blocks until we have the print mutex or the StatusThread is done. Prints a newline before 
		 * returning if the status thread is not done. If the status thread is done, a newline will have been 
		 * printed by the terminating StatusThread rather than this function. 
		 * 
		 * In all cases, StatusThread will not print after this function returns. 
		 */
		protected void suppressOutput()
		{		
			// Keep trying to get the print mutex.  
			while (!printMutex.acquire())
				// If the status thread is stopped, give up.
				if (stopped.get()) 
					return; 											
				
			// If the status thread isn't stopped, meaning we successfully acquired the print mutex, print a newline. 
			if (!stopped.get()) 								
				Main.out.println();			
		}
		
		/**
		 * Release the printMutex, allowing the status thread to resume printing.
		 * 
		 * The status thread will not resume printing if: 
		 * - It is done 
		 * - The status update period is 0, indicating that the status thread should not print retular updates. 
		 */
		protected void resumeOutput()
		{				
			printMutex.release();		
		}
					
		/**
		 * Gets an updated status row contents array 
		 * 
		 * @return
		 */
		private String[] getStatusRowContents() 
		{		
			StringBuilder stringBuilder = new StringBuilder(); 
			
			String separator = "";
			
			// Construct a string describing the container occupancy of each workload 
			for (int i = 0; i < workloads.length; i++)
			{				
					stringBuilder.append(separator + Utilities.formatDataSize(workloads[i].getLiveSetSize()));
					separator = " | ";
			}
			
			return 
				new String[] 
				{
					Utilities.formatTime(getElapsedTime()), 
					Utilities.formatDataRate(getPaymentRate()), 
					Utilities.formatDataRate(getDataRate()),										
					stringBuilder.toString(),
					Utilities.formatDataSize(getThroughput()), 					
					Utilities.formatDataSize(Runtime.getRuntime().freeMemory())
				}; 
		}
		
		/**
		 * Updates the contents of the given row with information from getStatusRowContents(). 
		 * 
		 * @param outputRow
		 * @throws RowException
		 * @throws CellException
		 */
		private void updateStatusRow(Table.Row outputRow) throws RowException, CellException
		{
			outputRow.setCellContents(getStatusRowContents());
		}
				
		/**
		 * Prints status updates until interrupted or suppressed. 
		 */
		public void run() 
		{	
			try 
			{							
				stopped.set(false);				
				Table.Headings headings = outputTable.new Headings
				(
						new String[] {"", "Pay", "Allocation Rate", (workloads.length == 1) ? "Live Set" : "Live Sets", "Throughput", "Free Heap"}, 
						new int[] {6, 9, 9, (8 * workloads.length) + 2, 7, 7},
						Alignment.LEFT
				); 							
				outputTable.addHeadings(headings);				
				Main.out.println(headings);
				
				Table.Row statusRow = outputTable.new Row(new String[] {"","","","","",""}, Alignment.LEFT); 
					
				// Period of zero implies no regular updates
				if (configuration.getStatusUpdatePeriod() == 0) 
				{
					try 										
					{
						// Just wait for the duration of the workloads. 
						Utilities.idle(duration);
					}
					catch (InterruptedException e) 
					{}
				}
				// Otherwise, update at most once per cycle 
				else 
				{
					while (!Thread.interrupted()) 
					{					 				
						try 										
						{																																								
							// Skip the update cycle if we can't get the print mutex immediately (we're suppressed) 
							if (printMutex.acquire()) 
							{		
								// Update the contents of the status row 
								updateStatusRow(statusRow);
								
								// Print the status row. 
								synchronized(Main.out)
								{
									if (Main.alwaysUseNewLines())
									{
										Main.out.println(statusRow);
									}
									else 
									{
										Main.out.print("\r" + statusRow);
									}
								}
								
								printMutex.release(); 
							}
							
							// Sleep until the next update
							Utilities.idle(configuration.getStatusUpdatePeriod());
						} 
						catch (InterruptedException e) 
						{								
							break; 
						}
					}
				}
				
				// Print one last update before dying. 
				if (printMutex.acquire()) 
				{		
					updateStatusRow(statusRow); 
					
					// Print the last update followed by a line 
					StringBuilder builder = new StringBuilder();					
					builder.append("\r");
					statusRow.appendToStringBuilder(builder);
					builder.append("\n");
					outputTable.new Line().appendToStringBuilder(builder);
					
					Main.out.println(builder);
				}
				stopped.set(true);
			} 			
			catch (Throwable e)
			{
				stopped.set(true);
				Event.RUNTIME_EXCEPTION.issue(e, "Status thread threw a fatal exception");				
			}						
		}		
	}
	
}
