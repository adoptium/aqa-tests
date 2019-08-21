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
package net.adoptopenjdk.casa.workload_sessions.configuration;

import java.util.ArrayList;

import net.adoptopenjdk.casa.event.EventHandler;
import net.adoptopenjdk.casa.util.AbstractConfiguration;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;

/**
 * Encapsulates the configuration attributes and payloadSet's of a Workload as represented in the 
 * configuration file. 
 * 
 *  
 *
 */
public class WorkloadConfiguration extends AbstractConfiguration {
	public final static String TAG_NAME = "workload";

	private final int numProducerThreads;
	private final double duration;
	private final double paymentPeriod;
	private final double maintenancePeriod;
	private final double startTime;
	private final double repetitionDelay;
	private boolean repetitionDelaySet = false;

	private PayloadSetConfiguration[] payloadSetConfigurations;

	private Configuration configuration;

	/**
	 * Parses a configuration from the given element including the parsing of
	 * payloads from any child elements.
	 * 
	 * 
	 * @throws ParseError
	 */
	public WorkloadConfiguration(Element element, Configuration configuration, EventHandler notice,
			EventHandler warning) throws ParseError {
		super(element);

		if (getElement().getNumChildren(PayloadSetConfiguration.TAG_NAME) == 0)
			throw new ParseError(
					WorkloadConfiguration.TAG_NAME + " has no " + PayloadSetConfiguration.TAG_NAME + "elements.",
					element);

		// Warn about deprecated attributes.
		getElement().setAttributeDeprecated("workQueueSize", warning); // There is no longer a work queue.
		getElement().setAttributeDeprecated("numRepetitions", warning); // Repetition is no longer supported.
		getElement().setAttributeDeprecated("statusUpdatePeriod", warning); // Repetition is no longer supported.
		getElement().setAttributeDeprecated("containerSize", warning);
		getElement().setAttributeDeprecated("numPayloadContainerLists", warning); // Contariners are now set-specific

		// Inherit max duration.
		getElement().setDefaultForAttribute("duration", Utilities.formatTime(configuration.getMaxDuration()), null);

		// Set defaults
		// getElement().setDefaultForAttribute("containerSize", "100%", null); //
		// Container can fill all free space
		getElement().setDefaultForAttribute("paymentPeriod", "1us", notice); // High frequency payment
		getElement().setDefaultForAttribute("maintenancePeriod", "1us", notice); // High frequency maintenance
		getElement().setDefaultForAttribute("numProducerThreads", "1", notice); // Single producer thread
		getElement().setDefaultForAttribute("startTime", "0", null); // Container can fill all free space
		getElement().setDefaultForAttribute("repetitionDelay", Utilities.formatTime(configuration.getMaxDuration()),
				null);

		// Parse attributes
		// parseContainerSizeField("containerSize");
		this.paymentPeriod = parseTimeField("paymentPeriod");
		this.duration = parseTimeField("duration");
		this.numProducerThreads = parseNumProducerThreads("numProducerThreads", configuration.getNumWorkloads());
		this.maintenancePeriod = parseTimeField("maintenancePeriod");
		this.startTime = parseTimeField("startTime");
		this.repetitionDelay = parseTimeField("repetitionDelay");

		this.configuration = configuration;
		// The duration must be greater than zero.
		if (this.duration <= 0)
			throw new ParseError("Each workload must have a finite duration.", element);

		if (this.repetitionDelay != configuration.getMaxDuration()) {
			this.repetitionDelaySet = true;
		}
		// Parse top-level payload sets.
		parsePayloadSets(notice, warning);

		// Ensure we've consumed all symbols.
		getElement().checkForUnusedAttributes();
	}

	/**
	 * Get the repetition delay for workload
	 * 
	 * @return
	 */
	public double getRepetitionDelay() {
		return repetitionDelay;
	}

	/**
	 * Get the parent configuration
	 * 
	 * @return
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the ID of the parsed element.
	 * 
	 * @return
	 */
	public int getID() {
		return getElement().getID();
	}

	public double getStartTime() {
		return startTime;
	}

	/**
	 * - If field is set to AUTO or auto, calculates the number of threads based on
	 * the number of available processors. - Otherwise, it's parsed as an integer.
	 * 
	 * @param key
	 * @param numWorklaods
	 * @return
	 * @throws ParseError
	 */
	private int parseNumProducerThreads(String key, int numWorklaods) throws ParseError {
		String value = getElement().consumeAttribute(key);

		// Choose the number of threads based on the size of the machine.
		if (value.toUpperCase().equals("AUTO")) {
			final int MAX_THREADS = 128;
			final int MIN_THREADS = 2;

			int availableProcessors = Runtime.getRuntime().availableProcessors();

			int numThreads = (availableProcessors / numWorklaods);

			if (numThreads < MIN_THREADS)
				return MIN_THREADS;
			else if (numThreads > MAX_THREADS)
				return MAX_THREADS;
			else
				return numThreads;
		} else {
			try {
				int numThreads = Integer.parseInt(value);

				// Reject negative numbers
				if (numThreads <= 0)
					throw new NumberFormatException();

				return numThreads;
			} catch (NumberFormatException e) {
				throw new ParseError(key + "=\"" + value + "\" is invalid. Must be a positive integer. ", getElement());
			}
		}
	}

	/**
	 * Parses PayloadSetConfiguration objects from the workload's children and adds
	 * the repeated payloadSetConfigurations to the list of payloadSetConfigurations
	 * 
	 * @throws ParseError                 - thrown on any error.
	 * @throws CloneNotSupportedException
	 */
	private void parsePayloadSets(EventHandler notice, EventHandler warning) throws ParseError {
		ArrayList<Element> children = getElement().getChildren(PayloadSetConfiguration.TAG_NAME);

		payloadSetConfigurations = new PayloadSetConfiguration[children.size()];

		// Parse payload set configurations from child elements.
		for (int i = 0; i < children.size(); i++)
			payloadSetConfigurations[i] = new PayloadSetConfiguration(children.get(i), null, this, notice, warning);

		for (PayloadSetConfiguration psc : payloadSetConfigurations) {

			boolean isRepeatingPayload = psc.getRepetitionDelay() != psc.getWorkloadConfiguration().getConfiguration()
					.getMaxDuration();

			/** Repeat payloads if necessary */
			if (isRepeatingPayload) {
				PayloadSetConfiguration[] repeatedPayloads = null;
				try {
					repeatedPayloads = updatePayload(psc);
				} catch (CloneNotSupportedException e) {
					// This should never happen
					System.out.println("Clone not supported... Remove repetition delay");
					e.printStackTrace();
				}

				/** -1 necessary because we do not double count the original payloadSetConfiguration. Doing -1 effectively omits the doubled up payload */
				PayloadSetConfiguration[] updatedPayloads = new PayloadSetConfiguration[payloadSetConfigurations.length
						+ repeatedPayloads.length -1];

				/**
				 * Append the repeated payloads at the end of the original
				 * payloadSetConfigurations. Again, the -1 is necessary so we do not double count the first repeated payload
				 */
				System.arraycopy(payloadSetConfigurations, 0, updatedPayloads, 0, payloadSetConfigurations.length);
				System.arraycopy(repeatedPayloads, 0, updatedPayloads, payloadSetConfigurations.length -1, repeatedPayloads.length);
				this.payloadSetConfigurations = updatedPayloads;
			}

		}

	}

	/**
	 * Generates a repeated payloadSetConfiguration [] based off of the original payloadSetConfiguration param.
	 * The value returned should then be put into the workload configurations payloadSetConfigurations list
	 * 
	 * @param payloadSetConfiguration
	 * @return The repeated payloadSetConfigurations
	 * @throws CloneNotSupportedException
	 * 
	 */

	private PayloadSetConfiguration[] updatePayload(PayloadSetConfiguration payloadSetConfiguration)
			throws CloneNotSupportedException, ParseError {
		PayloadSetConfiguration originalPayload = (PayloadSetConfiguration) payloadSetConfiguration.clone();
		PayloadSetConfiguration[] repeatedPayloads = generateRepeatedPayloads(originalPayload);
		return repeatedPayloads;
	}

	/**
	 * 
	 * @param payloadSetConfiguration the base payloadSetconfiguration for which we
	 *                                are generating several repetitions. If the
	 *                                current payloadSetConfiguration does not have
	 *                                a repetition delay, then it will only generate
	 *                                one repetition (IE, the original set)
	 * @return The list of repeatedConfigurations
	 * @throws CloneNotSupportedException if the payload cannot be cloned
	 */
	private PayloadSetConfiguration[] generateRepeatedPayloads(PayloadSetConfiguration payloadSetConfiguration)
			throws CloneNotSupportedException, ParseError {

		PayloadSetConfiguration[] repeatedPayloads = new PayloadSetConfiguration[payloadSetConfiguration
				.getNumberOfRepeatedPayloads()];
		double repetitionDelay = payloadSetConfiguration.getRepetitionDelay();

		// keep original payload
		repeatedPayloads[0] = payloadSetConfiguration;

		for (int i = 1; i < repeatedPayloads.length; i++) {
			double timeDelta = repetitionDelay * i;
			repeatedPayloads[i] = payloadSetConfiguration.generatePayload(timeDelta);
		}

		return repeatedPayloads;
	}

	/**
	 * Gets the target duration of the workload as specified by the user in the
	 * workload tag.
	 * 
	 * @return
	 */
	public double getDuration() {
		return duration;
	}

	/**
	 * Returns the sum of the user-specified startTime and duration for the workload
	 * 
	 * @return
	 */
	public double getEndTime() {
		return duration + startTime;
	}

	/**
	 * Returns the number of threads in the workloads's producer thread pool.
	 * 
	 * @return
	 */
	public int getNumProducerThreads() {
		return numProducerThreads;
	}

	/**
	 * Gets the period of time the PayerThread remains idle between payment
	 * operations.
	 * 
	 * @return
	 */
	public double getPaymentPeriod() {
		return paymentPeriod;
	}

	/**
	 * Gets the period of time the MaintenanceThread should remain idle between
	 * container cleanup operations.
	 * 
	 * @return
	 */
	public double getMaintenancePeriod() {
		return maintenancePeriod;
	}

	/**
	 * Gets an iterable list of the payload set configurations contained within this
	 * workload.
	 * 
	 * @return
	 */
	public PayloadSetConfiguration[] getPayloadSetConfigurations() {
		return payloadSetConfigurations;
	}

	/**
	 * 
	 * @return true if the workload is repeating
	 */
	public boolean isRepetitionDelaySet() {
		return this.repetitionDelaySet;
	}

}