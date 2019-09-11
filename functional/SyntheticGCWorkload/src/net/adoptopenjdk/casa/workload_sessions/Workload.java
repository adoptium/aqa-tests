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
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.adoptopenjdk.casa.data_structures.AtomicMutex;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.util.format.Alignment;
import net.adoptopenjdk.casa.util.format.CellException;
import net.adoptopenjdk.casa.util.format.RowException;
import net.adoptopenjdk.casa.util.format.Table;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadSetConfiguration;
import net.adoptopenjdk.casa.workload_sessions.configuration.WorkloadConfiguration;

/**
 * The Workload executes the task described by a WorkloadConfiguration, read from 
 * a <workload ...> tag in the configuration file. 
 * 
 *  
 */
public class Workload {
	// Configuration data for this workload read from the configuration file.
	private final WorkloadConfiguration workloadConfiguration;

	// Prevents multiple threads from executing kill() or shutdown(). Each method
	// will only execute on the first call.
	private final AtomicMutex shutdownMutex;

	// The builders track quota for each type of payload and are organized into
	// sets.
	// sets[] is a flat array of all sets containing payloads, each including only
	// payloads located directly within.
	private PayloadBuilderSet[] sets;

	// A (convenience) flat list of all builders
	private PayloadBuilder[] builders;

	// Allocation threads. Limited queue size and limited number of threads. Limits
	// set via configuration.
	// private final FixedQueueThreadPool producerThreadPool;
	private final PayloadBuilderThreadPool producerThreadPool;

	// Pays builders (distributes allocation quota)
	private final PayerThread payerThread;

	// Start time for this Workload
	private double startTime;

	/**
	 * Constructs a new instance including the configuration, container, thread
	 * pools and builders.
	 * 
	 * startup() must be called to start running the workload
	 * 
	 * @param workloadConfiguration
	 */
	protected Workload(WorkloadConfiguration workloadConfiguration) {
		this.workloadConfiguration = workloadConfiguration;

		// Prevent shutdown until startup is complete.
		shutdownMutex = new AtomicMutex(true);

		// Create PayloadBuilderSets from configuration by traversing <payloadSet ...>
		// tags
		traverseSets();

		// Construct producer thread pool as per configuration ;
		producerThreadPool = new PayloadBuilderThreadPool(workloadConfiguration.getNumProducerThreads());

		// Construct payer than status thread.
		payerThread = new PayerThread();
	}

	/**
	 * Starts running the workload. Returns as soon as the workload is running.
	 */
	protected void startup() {
		synchronized (this) {
			startTime = Utilities.getTime();

			// Start sets (and their containers' maintenance threads).
			for (int i = 0; i < sets.length; i++) {
				sets[i].startup();
			}

			producerThreadPool.start(this, builders);

			payerThread.start();

			// Allow shutdown
			shutdownMutex.release();
		}
	}

	/**
	 * Stops processing the workload and prints final stats.
	 * 
	 * @throws InterruptedException - thrown if any of the join() calls are
	 *                              interrupted.
	 */
	protected void shutdown() throws InterruptedException {
		synchronized (this) // Don't allow shutdown during startup.
		{
			// Only the first call to shutdown() will be executed.
			if (shutdownMutex.acquire()) {
				// Stop payment.
				payerThread.interrupt();
				payerThread.join();

				// Stop allocating.
				producerThreadPool.shutdown();

				// Stop sets (and their containers' maintenance threads).
				for (int i = 0; i < sets.length; i++) {
					sets[i].shutdown();
				}
			}
		}
	}

	/**
	 * Shutdown immediately using as little heap as possible. Doesn't block on
	 * thread termination.
	 */
	protected void kill() {
		synchronized (this) // Don't allow shutdown during startup.
		{
			// Only execute for the first caller.
			if (shutdownMutex.acquire()) {
				// Shutdown payer thread.
				payerThread.interrupt();

				for (int i = 0; i < sets.length; i++) {
					sets[i].kill();
				}

				// Shutdown the thread pools.
				producerThreadPool.kill();

				// Let the GC figure the rest out.
			}
		}
	}

	/**
	 * Gets the configuration for this workload.
	 * 
	 * @return
	 */
	protected WorkloadConfiguration getConfguration() {
		return workloadConfiguration;
	}

	/**
	 * Gets the actual time when the workload was started.
	 * 
	 * @return
	 */
	protected double getStartTime() {
		return startTime;
	}

	/**
	 * Creates and adds new builder sets for all sets containing payloads in the
	 * tree of sets. Adds all builders to a mater builder list. Uses a BFS to
	 * traverse the tree.
	 * 
	 * 
	 */
	private void traverseSets() {
		ArrayList<PayloadBuilderSet> sets = new ArrayList<PayloadBuilderSet>();
		ArrayList<PayloadBuilder> builders = new ArrayList<PayloadBuilder>();

		for (PayloadSetConfiguration payloadSetConfiguration : workloadConfiguration.getPayloadSetConfigurations()) {
			ConcurrentLinkedQueue<PayloadSetConfiguration> queue = new ConcurrentLinkedQueue<PayloadSetConfiguration>();
			queue.offer(payloadSetConfiguration);

			while (!queue.isEmpty()) {
				PayloadSetConfiguration setConfiguration = queue.poll();

				// If the configuration has payloads, create a new set and add it.
				if (setConfiguration.getNumPayloads() > 0) {
					PayloadBuilderSet set = new PayloadBuilderSet(setConfiguration, this);

					sets.add(set);

					for (PayloadBuilder builder : set.getBuilders())
						builders.add(builder);
				}

				// Enqueue child sets for traversal
				for (PayloadSetConfiguration childSetConfiguration : setConfiguration
						.getChildPayloadSetConfigurations()) {
					queue.offer(childSetConfiguration);
				}
			}
		}

		this.sets = sets.toArray(new PayloadBuilderSet[0]);
		this.builders = builders.toArray(new PayloadBuilder[0]);

	}

	/**
	 * Sums the payment rates of all encapsulated sets and returns the result.
	 * 
	 * @return a payment rate in bytes per second.
	 */
	protected double getPaymentRate() {
		double rate = 0;

		for (PayloadBuilderSet set : sets) {
			if (set.isActive())
				rate += set.getPaymentRate();
		}

		return rate;
	}

	/**
	 * Provides detailed information about each encapsulated set.
	 */
	private void appendSetsToStringBuilder(StringBuilder stringBuilder) {
		for (PayloadBuilderSet set : sets) {
			stringBuilder.append("\n");
			set.appendToStringBuilder(stringBuilder);
		}
	}


	/**
	 * Returns a formatted representation of the configuration data encapsulated in
	 * this Workload.
	 * 
	 * @param workloadIteration
	 * @return
	 */
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();

		try {
			Alignment[] alignment = new Alignment[] { Alignment.LEFT, Alignment.RIGHT };
			Table table = new Table();
			table.addHeadings(table.new Headings(new String[] { "WORKLOAD", workloadConfiguration.getID() + "" },
					new int[] { 24, 20 }, alignment, true, false));
			table.addRow(table.new Row(
					new String[] { "Start Time", Utilities.formatTime(workloadConfiguration.getStartTime()) },
					alignment));
			table.addRow(table.new Row(
					new String[] { "Duration", Utilities.formatTime(workloadConfiguration.getDuration()) }, alignment));
			table.addRow(table.new Row(
					new String[] { "Producer threads", workloadConfiguration.getNumProducerThreads() + "" },
					alignment));
			table.addRow(table.new Row(new String[] { "Maintenance period",
					Utilities.formatTime(workloadConfiguration.getMaintenancePeriod()) }, alignment));
			table.addRow(table.new Row(
					new String[] { "Pay period", Utilities.formatTime(workloadConfiguration.getPaymentPeriod()) },
					alignment));
			table.addLine();
			table.appendToStringBuilder(stringBuilder);
		} catch (RowException | CellException e) {
			Event.FATAL_ERROR.issue(e);
		}

		appendSetsToStringBuilder(stringBuilder);

		return stringBuilder.toString();
	}

	/**
	 * 
	 * @return true if the workload configuration has been set by the config file
	 *         (Ie, we want the workload to repeat itself)
	 */
	public boolean isRepeatingWorkload() {
		return getConfguration().getRepetitionDelay() != workloadConfiguration.getConfiguration().getMaxDuration();
	}

	/**
	 * we will repeat the workload as many times as possible given the workload
	 * configuration repetitionDelay if repetition delay is not set by the user,
	 * then it will be the same value as the max duration of the configuration
	 * Always return 1 or more
	 * 
	 * @return the number of time the payloads within this workload needs to be repeated, based on the repetition delay
	 */
	public double getNumberOfPayloadRepetitions() {
		double repetitionDelay = workloadConfiguration.getRepetitionDelay();
		double duration = workloadConfiguration.getDuration();
		double numberOfRepetitions = 1;

		if (numberOfRepetitions <  (duration / repetitionDelay)) {
			numberOfRepetitions =  (duration / repetitionDelay);
		}

		return numberOfRepetitions;
	}

	/**
	 * Gets the average allocation data rate since the start of this workload
	 * 
	 * @return the data rate in bytes per second
	 */
	protected double getDataRate() {
		return (double) getThroughput() / (double) getElapsedTime();
	}

	/**
	 * Gets an array containing all sets that directly contain payloads.
	 * 
	 * @return
	 */
	protected PayloadBuilderSet[] getPayloadBuilderSets() {
		return sets;
	}

	/**
	 * Gets the throughput in bytes up 'till this point, as reported by the thread
	 * pool.
	 * 
	 * @return
	 */
	protected long getThroughput() {
		return producerThreadPool.getThroughput();
	}

	/**
	 * Gets an estimate of the total number of bytes of throughput up until this
	 * point.
	 * 
	 * @return
	 */
	protected long getLiveSetSize() {
		// Sum of bytes removed from the containers.
		long removed = 0;

		for (int i = 0; i < sets.length; i++)
			removed += sets[i].getContainer().getBytesRemoved();

		/*
		 * The total number of butes still alive is the throughput minus the number of
		 * bytes either removed from the container or never added in the first place.
		 */
		long size = getThroughput() - removed - producerThreadPool.getBytesNeverAdded();

		return (size > 0) ? size : 0;
	}

	/**
	 * Gets the configuration of this workload.
	 * 
	 * @return
	 */
	protected WorkloadConfiguration getWorkloadConfiguration() {
		return workloadConfiguration;
	}

	/**
	 * Gets the amount of time (in seconds) since this Workload started.
	 * 
	 * @return
	 */
	protected double getElapsedTime() {
		return Utilities.getTime() - startTime;
	}

	/**
	 * Thread that allocates quota to (or "pays") each builder over time
	 * 
	 * @author Andrew Somerville <andrew.somerville@unb.ca>
	 *
	 */
	private class PayerThread extends Thread {
		/**
		 * Constructs a new PayerThread for the given WorkloadConfiguration and
		 * builders.
		 * 
		 * @param workloadConfiguration
		 * @param builders
		 */
		protected PayerThread() {
			Event.ASSERTION.issue(sets == null, "builders is null");
			Event.ASSERTION.issue(workloadConfiguration == null, "configuration is null");

			setDaemon(true);
		}

		/**
		 * Calls payAll() on each set. (Pays each builder)
		 * 
		 * @param lastCycleTime
		 */
		private void payAllBuilders(double lastCycleTime) {
			// Iterate through sets
			for (int j = 0; j < sets.length; j++) {
				// Only pay active sets
				if (sets[j].isActive()) {
					// Calculate payment
					double payment = sets[j].getCyclePayment(lastCycleTime);

					// Adjust counter for payment amount
					sets[j].adjustTotalPayment(payment);

					// Pay builders
					for (int k = 0; k < sets[j].getBuilders().length; k++)
						sets[j].getBuilders()[k].pay(payment);
				}
			}
		}

		/**
		 * Pays and processes builders until interrupted.
		 */
		public void run() {
			final double IDLE_PERIOD = workloadConfiguration.getPaymentPeriod();

			try {
				// Initial payment cycle time is the time since workload startup.
				double lastCycleTime = Utilities.getTime() - startTime;

				// Pay until interrupted
				while (!Thread.interrupted()) {
					double cycleStartTime = Utilities.getTime();

					// Distributes quota (tokens) to the builders.
					payAllBuilders(lastCycleTime);

					// Idle for the specified period
					Utilities.idle(IDLE_PERIOD - (Utilities.getTime() - cycleStartTime));

					// Measure the time taken by the cycle just completed, including any idle time.
					lastCycleTime = Utilities.getTime() - cycleStartTime;
				}
			} catch (InterruptedException e) {
				return;
			} catch (Throwable e) {
				Event.RUNTIME_EXCEPTION.issue(e, "Failure in payer thread.");
			}
		}
	}

	public PayloadBuilderSet[] getSets() {
		return this.sets;
	}

	public void setSets(PayloadBuilderSet[] sets) {
		this.sets = sets;
	}

	/**
	 *  A repetition delay is being used for the workload, so the payloadSets will now be repeated
	 */
	public void createRepeatingPayloads(){
		this.sets = generateRepeatingPayload();
	}


	/**
	 * We need to generate n number of payloads based on the known number of repetitions.
	 * If we have 3 sets, and 4 repetitions of each, then each set needs to be repeated 4 times for a total of 12 sets.
	 * Each set must scale its start and end time appropriately,so that the repetition delay is applied to each of the values.
	 * Ex: if set 1 starts at t=10 and ends at t=15, and repeition delay is 30, then the second iteration of that set should
	 * start at t=40, and end at t=45. The sequence number represents the iteration of each individual set.
	 * 
	 * @return the repeatedPayloadSet with all repetitions
	 */
	public PayloadBuilderSet[] generateRepeatingPayload() {
		PayloadBuilderSet[] updatedPayloadList = new PayloadBuilderSet[(int)getNumberOfPayloadRepetitions()];

		System.arraycopy(sets, 0, updatedPayloadList, 0, sets.length); //keep the original payloads, and put them into the updated list

		// create the repetitions for each of the payloadSets within the workload, and put them into the updatedList of payloads
		for (int i = sets.length; i<updatedPayloadList.length; i++) {
			
			int setNumber = getRemainder(i, sets.length);
			int sequenceNumber = i/sets.length;
			int scalarValue = sequenceNumber * (int)workloadConfiguration.getRepetitionDelay();
			updatedPayloadList[i] = sets[setNumber].generateSet(scalarValue);
		}

		return updatedPayloadList;
	}

	/**
	 * Remainder calculation which is faster than using %
	 * 
	 * @param num number to be divided
	 * @param divisor number to divide by
	 * @return the remainder
	 */
	int getRemainder(int num, int divisor) {
		return (num - divisor * (num/divisor));
	}


}
