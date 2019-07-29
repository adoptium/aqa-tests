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
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;

/**
 * An array payload with a flat random size, determined upon instantiation. 
 * 
 *  
 */
public class RandomArrayPayload extends ArrayPayload 
{		
	protected static final int BYTES_PER_ELEMENT = 1;  
	
	byte[] payload; 	
	private long size; 
	
	/**
	 * Instantiates a new random-length array payload with length based on 
	 * the criteria in the configuration file.  
	 * 
	 * @param configuration
	 * @throws PayloadException
	 */
	public RandomArrayPayload(PayloadConfiguration configuration) throws PayloadException
	{
		super(configuration);
		 
		size = Utilities.generateRandomDataSize(									
				getConfiguration().getMinSize(), 
				getConfiguration().getMaxSize(), 
				Sizes.ALIGNMENT);
		
		payload = new byte[(int)getNumElements(BYTES_PER_ELEMENT)]; 
	}
		
	/**
	 * Gets the actual size of the payload including overhead. 
	 */
	public long getSize()
	{
		return size; 
	}
}