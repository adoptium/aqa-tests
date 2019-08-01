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
package net.adoptopenjdk.casa.workload_sessions.payload;
import java.util.concurrent.atomic.AtomicReference;

import net.adoptopenjdk.casa.util.Sizes;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Payload is extended by automatically generated Payload classes which contain 
 * additional fields in order to consume heap space. 
 * 
 * ArrayPayload derived classes also extend Payload. 
 * 
 *  
 *
 */
public abstract class Payload 
{
	protected static final long INSTANCE_FIELD_SIZE = Sizes.DOUBLE_SIZE + Sizes.REFERENCE_SIZE + Sizes.REFERENCE_SIZE; 			
	protected static final String PACKAGE_NAME = Payload.class.getPackage().getName();  
	protected static final long OVERHEAD = Sizes.MIN_OBJECT_SIZE + Sizes.ATOMIC_REFERENCE_SIZE + INSTANCE_FIELD_SIZE;
	protected static final long REFLEXIVE_CUTOFF = Math.round(ParseUtilities.parseDataSizeToBytes("256kB"));
	
	// The overhead for this Payload. 
	private final PayloadConfiguration configuration;
	private final AtomicReference<Payload> next; 		
	private final double expirationTime;	
	/////////////////////////////////
	
	/**
	 * 
	 * @param configuration
	 */
	public Payload(PayloadConfiguration configuration)
	{		
		this.configuration = configuration;
		this.next = new AtomicReference<Payload> (null);
		
		// Non-zero radius means we are generating a random lifespan 
		if (configuration.getLifespanRadius() != 0)
		{
			// Nonzero variance means we are generating a gaussian lifespan 
			if (configuration.getLifespanVariance() != 0)
				expirationTime = Utilities.getTime() + Utilities.generateGaussianTime(configuration.getLifespan(), configuration.getLifespanVariance(), configuration.getLifespanRadius());
			else 
				expirationTime = Utilities.getTime() + Utilities.generateRandomTime(configuration.getLifespan(), configuration.getLifespanRadius());
		}
		else 
		{
			// No randomness 
			expirationTime = Utilities.getTime() + configuration.getLifespan();
		}
	}
	
	/**
	 * Gets the atomic reference to the next payload in the list. 
	 * 
	 * @return
	 */
	public AtomicReference<Payload> getNext()
	{
		return next; 
	}
	
	/**
	 * Sets the atomic reference to the next payload in the list. 
	 * 
	 * @param payload
	 */
	public void setNext(Payload payload)	
	{
		next.set(payload); 
	}
	
	/**
	 * Checks to see if this object's expiration time has been reached. 
	 * 
	 * @return true if the object has expired and should be removed from the container. 
	 */
	public boolean isExpired()
	{			
		return (expirationTime < Utilities.getTime()); 
	}
	
	/**
	 * Gets this Payload's configuration 
	 * 
	 * @return
	 */	
	protected PayloadConfiguration getConfiguration()
	{
		return configuration; 
	}
	
	/**
	 * Returns the actual size of the Payload. Must be correctly overridden for 
	 * statistical types  
	 * 
	 * @return
	 */
	public long getSize()
	{
		return configuration.getSize(); 
	}
}
