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

import net.adoptopenjdk.casa.util.AbstractConfiguration;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.payload.PayloadType;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;

/**
 * Houses values parsed from a "payload" tag in the configuration file.  
 * 
 *  
 *
 */
public class PayloadConfiguration extends AbstractConfiguration
{
	public final static String TAG_NAME = "payload";
	
	private double proportionOfAllocation;
	private final long size; 
	private final double sizeVariance; 
	private final long sizeRadius; 
	private final double lifespan;
	private final double lifespanVariance; 
	private final double lifespanRadius; 
	private final PayloadType payloadType; 
	private final PayloadSetConfiguration parentPayloadSetConfiguration; 
	
	/**
	 * Constructs a new payload configuration, inheriting attributes from 
	 * the given parent set. 
	 * 
	 * @param element
	 * @param parentSet
	 * @throws ParseError
	 */
	public PayloadConfiguration (Element element, PayloadSetConfiguration parentSet) throws ParseError
	{
		super(element);
		
		this.parentPayloadSetConfiguration = parentSet;
		
		// Assert manditory attributes are set. 
		getElement().assertAttributeIsSet("proportionOfAllocation");
		getElement().assertAttributeIsSet("size");
		getElement().assertAttributeIsSet("lifespan");		
		
		// Silently set defaults for optional attributes. 
		getElement().setDefaultForAttribute("sizeVariance", "0", null);
		getElement().setDefaultForAttribute("sizeRadius", "0", null);		
		getElement().setDefaultForAttribute("lifespanVariance", "0", null);
		getElement().setDefaultForAttribute("lifespanRadius", "0", null);
						
		// Inherit default payload type from parent set. 
		getElement().setDefaultForAttribute("payloadType", parentSet.getPayloadType().toString(), null);
				
		proportionOfAllocation = parseProportionField("proportionOfAllocation");		
		size                   = Math.round(parseDataSizeField("size")); 
		sizeVariance           = parseDataSizeField("sizeVariance");
		sizeRadius             = Math.round(parseDataSizeField("sizeRadius"));
		lifespan               = parseTimeField("lifespan");
		lifespanVariance       = parseTimeField("lifespanVariance");
		lifespanRadius         = parseTimeField("lifespanRadius");
		payloadType            = parsePayloadType("payloadType");
					
		// Check maximum size 
		if (size > payloadType.maxSize())
			throw new ParseError("Payload exceeds the maximum allowable size of " + payloadType.maxSize() + ".", getElement());
		
		// Check minimum size
		if (size < payloadType.minSize())
			throw new ParseError("payload " + getID() + " is too small. Minimum size is " + payloadType.minSize(), element);		
		
		// Check alignment
		if ((size % payloadType.getAlignment()) != 0)
			throw new ParseError("payload " + getID() + " size must be divisible by " + Utilities.formatDataSize(payloadType.getAlignment()), element);
		
		// Variance should only be supplied with the gaussian_array type
		if ((sizeVariance != 0) && !payloadType.equals(PayloadType.GAUSSIAN_ARRAY))
			throw new ParseError("payload " + getID() + "'s type is incompatible with the sizeVariance parameter.", element);
		
		// Radius is only compatible with the random and gaussian array types  
		if ((sizeRadius != 0) && !payloadType.equals(PayloadType.GAUSSIAN_ARRAY) && !payloadType.equals(PayloadType.RANDOM_ARRAY))
			throw new ParseError("payload " + getID() + "'s type is incompatible with the sizeRadius parameter.", element);
														
		// Check radius alignment 
		if ((sizeRadius > 0) && ((sizeRadius % payloadType.getAlignment()) != 0))
			throw new ParseError("payload " + getID() + " sizeRadius must be divisible by " + Utilities.formatDataSize(payloadType.getAlignment()), element);					
			
		getElement().checkForUnusedAttributes(); 
	}
		
	/**
	 * Gets the element's ID. 
	 * 
	 * @return
	 */
	public int getID()
	{
		return getElement().getID(); 
	}
	
	/**
	 * Gets a dot separated list of payloadset ID numbers from this payload's 
	 * parent set to its farthest ancestor followed by this Payload's ID.  
	 * @return
	 */
	public String getIDString() 
	{
		if (parentPayloadSetConfiguration == null)
			return "" + getID();
		else 
			return parentPayloadSetConfiguration.getIDString() + "." + getID();				
	}
	
	/**
	 * Gets the payload type. 
	 * 
	 * @return
	 */
	public PayloadType getPayloadType()
	{
		return payloadType; 
	}
	
	/**
	 * Gets the proportion of the data rate the Payload is paid at.  
	 * 
	 * @return
	 */
	public double getProportionOfAllocation()
	{
		return proportionOfAllocation; 
	}
	
	/**
	 * Gets the size of the Payload. 
	 * 
	 * @return
	 */
	public long getSize()
	{
		return size; 
	}
	
	/**
	 * Gets the size variance of the payload as set in the configuration file. 
	 * 
	 * @return
	 */
	public double getSizeVariance()
	{
		return sizeVariance; 
	}
	
	/**
	 * Gets the size radius of the payload as set in the configuration file 
	 * 
	 * @return
	 */
	public long getSizeRadius()
	{
		return sizeRadius; 
	}
	
	/**
	 * Calculates the minimum size of the payload based on the type 
	 * overhead and, size and radius 
	 * 
	 * @return
	 */
	public long getMinSize()
	{
		long min = size - sizeRadius;
		
		if (min < payloadType.minSize())
			return payloadType.minSize(); 
		else 
			return min;
	 
	}
	
	/**
	 * Calculates the maximum size for the payload based on the size and 
	 * radius 
	 * 
	 * @return
	 */
	public long getMaxSize()
	{
		return size + sizeRadius; 
	}
	
	/**
	 * Gets the length of time the payload should remain in 
	 * the container after being added. 
	 * 
	 * @return
	 */
	public double getLifespan()
	{
		return lifespan; 
	}
	
	/**
	 * Gets the lifespan variance as parsed from the configuration 
	 * file. 
	 * 
	 * @return
	 */
	public double getLifespanVariance()
	{
		return lifespanVariance; 
	}
	
	/**
	 * Gets the lifespan variance as parsed from the configuration file 
	 * 
	 * @return
	 */
	public double getLifespanRadius()
	{
		return lifespanRadius; 
	}
	
	/**
	 * Calculates the expected lifespan of the object in the container 
	 * based on the length of the maintenance period and the lifetime of the 
	 * set it belongs to. 
	 * 
	 * @param payloadSetConfiguration
	 * @return
	 */
	public double getExpectedLifespan(double maintenancePeriod, double workloadDuration)
	{	
		double setLifespan = parentPayloadSetConfiguration.getExpectedLifespan(workloadDuration);
				
		double lifespan = getLifespan(); 
				
		if (maintenancePeriod > lifespan)
			lifespan = maintenancePeriod;
		
		if (setLifespan < lifespan)
			lifespan = setLifespan; 
		
		return lifespan; 			
	}
	
	/**
	 * Calculates the time between allocations
	 * 
	 * @return
	 */
	public double getAllocationPeriod()
	{
		return (getSize() / (getPayloadSetConfiguration().getDataRate() * getProportionOfAllocation()));
	}
	
	/**
	 * Determines if this payload will ever be allocated. 
	 * 
	 * @param workloadDuration
	 * @return
	 */
	public boolean willBeAllocated(double workloadDuration)
	{
		double firstAllocation = getTimeToFirstAllocation(workloadDuration); 
		
		if (firstAllocation > getPayloadSetConfiguration().getExpectedEndTime(workloadDuration))
			return false; 
		else 
			return true; 
	}
	
	/**
	 * Calculates the time at which this Payload will first be 
	 * allocated. The time may be greater than the expected duration. 
	 * 
	 * @param workloadDuration
	 * @return
	 */
	public double getTimeToFirstAllocation(double workloadDuration)
	{
		return getAllocationPeriod() + getPayloadSetConfiguration().getExpectedStartTime(workloadDuration);		
	}
	
	/**
	 * Calculates the time at which the first instance of this payload will 
	 * expire. 
	 * 
	 * @param maintenancePeriod
	 * @param workloadDuration
	 * @return
	 */
	public double getTimeToFirstDecay(double maintenancePeriod, double workloadDuration)
	{
		if (!willBeAllocated(workloadDuration))
			return 0; 
		else {
			double firstAllocation = getTimeToFirstAllocation(workloadDuration);
			double lifespan = getExpectedLifespan(maintenancePeriod, workloadDuration); 
			
			return firstAllocation + lifespan;
		}
	}

	/**
	 * Calculates the maximum number of this payload that will be concurrently 
	 * live. 
	 * 
	 * @param maintenancePeriod
	 * @param workloadDuration
	 * @return
	 */
	public long getMaxConcurrentPayloads(double maintenancePeriod, double workloadDuration)
	{			
		if (!willBeAllocated(workloadDuration))
			return 0; 
		
		double duration = getPayloadSetConfiguration().getExpectedDuration(workloadDuration);
		double lifespan = getExpectedLifespan(maintenancePeriod, workloadDuration); 
		double period = getAllocationPeriod();
		
		double allocations = Math.floor(duration / period);
		double maxConcurrent = Math.ceil(lifespan / period);
		
		return Math.round(Math.min(allocations, maxConcurrent));
	}
	
	/**
	 * Divides the proportionOfAllocation by the given total in order 
	 * to scale it. 
	 * 
	 * @param total
	 */
	public void scaleProportionOfAllocation(double total)
	{		
		proportionOfAllocation /= total;
	}
	
	/**
	 * Parses the payload type from the symbol table. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private PayloadType parsePayloadType(String key) throws ParseError 
	{
		try 
		{
			PayloadType type = PayloadType.valueOf(getElement().consumeAttribute(key).toUpperCase());
			
			if (type.equals(PayloadType.NONE))
			{
				throw new ParseError("Must supply a payloadType either in the payloadSet tag or the payload tag.", getElement());
			}
			// AUTO is a metatype that will be resolved here into a best-fit payload type. 
			else if (type.equals(PayloadType.AUTO)) 
			{
				// If we have variance, we're a gaussean array 
				if (sizeVariance != 0)
				{
					// Variance requires radius 
					if (sizeRadius == 0)
						throw new ParseError("payload " + getID() + ": cannot set sizeVariance without giving a sizeRadius.", getElement());
					else 
						type = PayloadType.GAUSSIAN_ARRAY;
				}
				// Random array has radius but no variance
				else if (sizeVariance == 0 && sizeRadius != 0)
				{							
					type = PayloadType.RANDOM_ARRAY;
				}
				// Too large for reflexive, make it a byte array. 
				else if (getSize() > PayloadType.REFLEXIVE.maxSize()) 
				{
					// If too big for byte array, jump to long array.  
					if (getSize() > PayloadType.BYTE_ARRAY.maxSize())
						type = PayloadType.LONG_ARRAY;					
					else 
						type = PayloadType.BYTE_ARRAY; 
				}
				// Default to reflexive for small, fixed-size payloads.  
				else 
				{
					type = PayloadType.REFLEXIVE;
				}
			}
			
			return type; 
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			throw new ParseError(key + " is invalid. Must be a valid PayloadType: " + PayloadType.values().toString(), getElement()); 
		}
	}

	/**
	 * Gets this payload's immediate parent set. 
	 * 
	 * @return
	 */
	public PayloadSetConfiguration getPayloadSetConfiguration() 
	{	
		return parentPayloadSetConfiguration; 
	}
	
	/**
	 * Gets the class name of this 
	 * 
	 * @return
	 */
	public String getClassName()
	{
		if (payloadType.equals(PayloadType.REFLEXIVE))
			return "GeneratedPayloadS" + getSize();
		else 
			return null; 
	}
}
