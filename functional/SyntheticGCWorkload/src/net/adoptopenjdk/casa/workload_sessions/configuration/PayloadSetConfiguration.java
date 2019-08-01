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
import net.adoptopenjdk.casa.workload_sessions.payload.PayloadType;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Houses values parsed from a "payloadSet" tag in the configuration file.  
 * 
 *  
 */
public class PayloadSetConfiguration extends AbstractConfiguration
{	
	public final static String TAG_NAME = "payloadSet";
	
	private final double startTime; 
	private final double endTime; 
	private final double dataRate; 
	private final double transactionsPerByte;
	private final PayloadType payloadType;
	
	private final int numPayloadContainerLists; 
	
	private PayloadConfiguration[] payloadConfigurations; 	
	
	private PayloadSetConfiguration[] childPayloadSetConfigurations;

	private PayloadSetConfiguration parent;
	
	private WorkloadConfiguration workloadConfiguration;
		
	/**
	 * Constructs the payloadSet configuration from the given element. Payloads 
	 * are parsed from its child nodes.  
	 * 
	 * @param element
	 * @throws ParseError
	 */
	public PayloadSetConfiguration (Element element, PayloadSetConfiguration parent, WorkloadConfiguration workloadConfiguration, EventHandler notice, EventHandler warning) throws ParseError
	{
		super(element);
		this.parent = parent;
		this.workloadConfiguration = workloadConfiguration; 
				 	
		if (parent == null) 
		{			
			if (getNumPayloads() == 0) 
			{
				getElement().setDefaultForAttribute("dataRate", "0", null);
				getElement().setDefaultForAttribute("startTime", "0", null);
			}
			else 
			{
				getElement().assertAttributeIsSet("dataRate");
				getElement().setDefaultForAttribute("startTime", "0", notice);
			}			
			
			getElement().setDefaultForAttribute("endTime", "0", null);			
			getElement().setDefaultForAttribute("payloadType", PayloadType.NONE.toString(), null);
			getElement().setDefaultForAttribute("numPayloadContainerLists", 256 * Runtime.getRuntime().availableProcessors() + "", null);
		}
		else 
		{
			if (parent.getDataRate() == 0)
				getElement().assertAttributeIsSet("dataRate");
			else 
				getElement().setDefaultForAttribute("dataRate", Utilities.formatDataRate(parent.getDataRate()), null);
						
			getElement().setDefaultForAttribute("startTime", Utilities.formatTime(parent.getStartTime()), null);
			getElement().setDefaultForAttribute("endTime", Utilities.formatTime(parent.getEndTime()), null);			
			getElement().setDefaultForAttribute("payloadType", parent.getPayloadType().toString(), null);
			getElement().setDefaultForAttribute("numPayloadContainerLists", Integer.toString(parent.getNumPayloadContainerLists()), null);
		}
		
		endTime     = parseTimeField("endTime");
		startTime   = parseTimeField("startTime");
		payloadType = parsePayloadType("payloadType");		
	
		numPayloadContainerLists = parseIntegerField("numPayloadContainerLists"); 
		
		parsePayloads(warning); 
		 						
		double transactions = 0;		
		for (PayloadConfiguration configuration : payloadConfigurations) {
			transactions += (configuration.getProportionOfAllocation() /  configuration.getSize());
		}
		transactionsPerByte = transactions;
		
		this.dataRate = parseDataRateOrTransactionRateField("dataRate");
		
		parsePayloadSets(notice, warning);
		
		getElement().checkForUnusedAttributes(); 
	}
	
	/**
	 * 
	 * @return
	 */
	public int getNumPayloadContainerLists()
	{
		return numPayloadContainerLists; 
	}
	
	/**
	 * Gets a dot separated list of payloadset ID numbers from this payload set to its ancestor.  
	 * @return
	 */
	public String getIDString() 
	{
		if (parent == null)
			return "" + getElement().getID();
		else 
			return parent.getIDString() + "." + getElement().getID();				
	}

	/**
	 * Gets the number of transactions (allocation operations) which need 
	 * will be performed for each byte allocated. 
	 * 
	 * @return the number of transactions per byte allocated (in B^-1)
	 */
	public double getTransactionsPerByte()
	{
		return transactionsPerByte; 
	}
		
	/**
	 * Gets the configured data rate 
	 * 
	 * @return the data rate in B/s
	 */
	public double getDataRate()
	{
		return dataRate;
	}
	
	/**
	 * Gets the configured end time 
	 * 
	 * @return the configured end time, in seconds, or 0 if no end time was configured. 
	 */
	public double getEndTime()
	{		
		return endTime; 
	}
	
	/**
	 * Gets the configured start time.
	 * 
	 * @return
	 */
	public double getStartTime()
	{ 
		return startTime; 
	}
	
	/**
	 * Gets the expected start time from the start of the 
	 * workload run, taking into account the overall 
	 * workload duration. 
	 * 
	 * @return start time in seconds. 
	 */
	public double getExpectedStartTime(double workloadDuration)
	{	
		if (startTime >= workloadDuration)
			return workloadDuration;
		else 
			return startTime; 
	}
	
	/**
	 * 
	 * 
	 * @param workloadDuration
	 * @return
	 */
	public double getExpectedDuration(double workloadDuration)
	{
		return getExpectedEndTime(workloadDuration) - getExpectedStartTime(workloadDuration);
	}
	
	/**
	 * Gets the expected end time, taking into account 
	 * the overall duration of the workload. 
	 * 
	 * Note that payment will stop at this time, but allocation 
	 * may continue until builders' balances are exhausted.  
	 * 
	 * @return the payment end time of this set in seconds, from the start of the workload.  
	 */
	public double getExpectedEndTime(double workloadDuration)
	{			
		if (endTime == 0 || startTime >= workloadDuration || endTime >= workloadDuration)
			return workloadDuration; 
		else 
			return endTime; 
	}

	/**
	 * Gets the time this set is expected to run for.
	 * 
	 * @return zero or more seconds. 
	 */
	public double getExpectedLifespan(double workloadDuration)
	{
		if (getExpectedEndTime(workloadDuration) > getExpectedStartTime(workloadDuration))	
			return getExpectedEndTime(workloadDuration) - getExpectedStartTime(workloadDuration);
		else 
			return 0; 
	}
	
	/**
	 * Gets payload configurations located directly within this set.
	 * 
	 * @return
	 */
	public PayloadConfiguration[] getPayloadConfigurations() 
	{
		return payloadConfigurations; 
	}
	
	/**
	 * Parses a data rate from the symbol table at the given key. Supports 
	 * B/s, tr/s, B/s/thread and tr/s/thread, where B is any data unit. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private double parseDataRateOrTransactionRateField(String key) throws ParseError
	{
		String value = getElement().consumeAttribute(key);
		
		final String PER_THREAD_SUFFIX = "/thread";
		
		if (value.endsWith("/thread"))
		{
			double perThreadRate = parseDataRateOrTransactionRate(key, value.substring(0, value.length() - PER_THREAD_SUFFIX.length()));			
			return perThreadRate * workloadConfiguration.getNumProducerThreads(); 
		}
		else 
		{
			return parseDataRateOrTransactionRate(key, value);
		}
	}
	
	/**
	 * Parses a data rate (in B/s or tr/s) from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private double parseDataRateOrTransactionRate(String key, String value) throws ParseError
	{		
		double rate; 
		try 
		{
			if (value.endsWith("tr/s")) 
			{														
				rate = Double.parseDouble(value.substring(0, value.length() - 4));
				rate /= transactionsPerByte;								
			}				
			else 
			{
				rate = parseDataRate(key, value);
			}
		} 
		catch (NumberFormatException e) 
		{			
			throw new ParseError(key + " is invalid: " + e.getMessage(), getElement());
		}		
		
		return rate; 	
	}
	
	
	/**
	 * Parses a data rate (in B/s) from the given string. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private double parseDataRate(String key, String value) throws ParseError
	{		
		double rate; 
		try 
		{
			rate = ParseUtilities.parseDataRateToBytesPerSecond(value);
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid: " + e.getMessage(), getElement()); 
		}
				
		return rate; 
	}
	
	/**
	 * Parses the type attribute
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private PayloadType parsePayloadType(String key) throws ParseError 
	{
		try 
		{
			PayloadType type = PayloadType.valueOf(PayloadType.class, getElement().consumeAttribute(key).toUpperCase());
								
			return type; 
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			throw new ParseError(key + " is invalid. Must be a valid PayloadType: " + PayloadType.values().toString(), getElement()); 
		}
	}
	
	/**
	 * Parses PayloadConfiguration objects from the given NodeList and adds them to the given 
	 * WorkloadConfiguration 
	 * 
	 * @param nodes
	 * @param workloadConfiguration
	 * @throws ParseError
	 */
	private void parsePayloads (EventHandler warning) throws ParseError
	{	
		ArrayList<Element> children = getElement().getChildren(PayloadConfiguration.TAG_NAME);
		
		payloadConfigurations = new PayloadConfiguration[children.size()];
		
		if (children.size() > 0) 
		{
			// Parse Payload configurations
			for (int i = 0; i < children.size(); i++) 
				payloadConfigurations[i] = new PayloadConfiguration(children.get(i), this);		

			// Sum the total weight.
			double totalWeight = 0;
			for (PayloadConfiguration configuration : payloadConfigurations) 			
				totalWeight += configuration.getProportionOfAllocation();			

			// If it's not 100% +/- 0.5%, scale 
			if (warning.issue(Math.round(totalWeight * 100) != 100, "proportionOfAllocation fields in " + getElement().getName() + " " + getIDString() + " do not sum to 100%; scaling values.")) 
			{ 				
				for (PayloadConfiguration configuration : payloadConfigurations) 				
					configuration.scaleProportionOfAllocation(totalWeight);
				
			}	
		}
	}

	/**
	 * Parses all child sets 
	 * 
	 * @param notice
	 * @param warning
	 * @throws ParseError
	 */
	private void parsePayloadSets (EventHandler notice, EventHandler warning) throws ParseError
	{	
		ArrayList<Element> children = getElement().getChildren(PayloadSetConfiguration.TAG_NAME);
		
		childPayloadSetConfigurations = new PayloadSetConfiguration[children.size()]; 		
		
		for (int i = 0; i < children.size(); i++) {			 		
			childPayloadSetConfigurations[i] = new PayloadSetConfiguration(children.get(i), this, workloadConfiguration, notice, warning);						
		}				
	}
	
	/**
	 * Gets the parsed configuration of all child sets. 
	 * 
	 * @return
	 */
	public PayloadSetConfiguration[] getChildPayloadSetConfigurations()
	{ 
		return childPayloadSetConfigurations; 
	}
	
	/**
	 * Gets the value of the type field in this set, 
	 * possibly inherited from its parent. 
	 * 
	 * @return
	 */
	public PayloadType getPayloadType() 
	{
		return payloadType; 
	}

	/**
	 * Gets the number of payloads located directly within this set. 
	 * 
	 * @return
	 */
	public int getNumPayloads() 
	{
		return getElement().getNumChildren(PayloadConfiguration.TAG_NAME);
	}	
}
