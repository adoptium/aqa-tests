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

import net.adoptopenjdk.casa.util.Sizes;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;

/**
 * This class is overridden to create an array payload 
 * type class. 
 * 
 *  
 */
public abstract class ArrayPayload extends Payload 
{		
	protected static final long OVERHEAD = Sizes.ARRAY_SIZE + Sizes.REFERENCE_SIZE + Payload.OVERHEAD; // array header + array pointer (in subclass) + numElements + Payload overhead. 
	  
	/**
	 * 
	 * 
	 * @param configuration
	 * @throws PayloadException
	 */
	protected ArrayPayload(PayloadConfiguration configuration) throws PayloadException
	{
		super(configuration);					
	}
	
	/**
	 * Calculates the amount of space left for the array after overhead is accounted for. 
	 * 
	 * @return
	 */
	protected long getArraySize() 
	{			
		return getSize() - OVERHEAD;
	}
	
	/**
	 * Calculates the number of elements to place in the array. 
	 * 
	 * @param bytesPerElement
	 * @return
	 */
	protected long getNumElements(int bytesPerElement)
	{
		return Math.round(Math.floor(getArraySize()/(double)bytesPerElement));
	}
}
