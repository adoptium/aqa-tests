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

import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;

/**
 * A payload class that has a Gaussian-distributed randomly sized array. 
 * 
 *  
 */
public class GaussianDistributedArrayPayload extends ArrayPayload 
{		
	protected static final int BYTES_PER_ELEMENT = 1;  
	
	byte[] payload; 	
	private long size; 
	
	/**
	 * Creates a new array payload with a random size determined according to the 
	 * configuration. 
	 * 
	 * @param configuration
	 * @throws PayloadException
	 */
	public GaussianDistributedArrayPayload(PayloadConfiguration configuration) throws PayloadException
	{
		super(configuration);
		
		size = Utilities.generateGaussianDataSize(
				getConfiguration().getSize(), 
				getConfiguration().getSizeVariance(), 
				getConfiguration().getSizeRadius(), 
				getConfiguration().getMinSize(), 
				getConfiguration().getMaxSize(), 
				PayloadType.GAUSSIAN_ARRAY.getAlignment());
		
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