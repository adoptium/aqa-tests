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



import net.adoptopenjdk.casa.util.Sizes;

/**
 * Represents a row in an imaginary database with a numeric ID and some data. 
 * 
 *  
 *
 */
public class Row 
{
	public static final long OVERHEAD =
			// Header for the Row instance
			Sizes.MIN_OBJECT_SIZE + 
			// Array header. 
			Sizes.ARRAY_SIZE + 
			//Reference to the byte array 
			Sizes.REFERENCE_SIZE +		
			// Index instance variable size 
			Sizes.LONG_SIZE;

	public static final long ELEMENT_SIZE = Sizes.INT_SIZE;	
	
	private long index;	
	private int[] data; 
	
	/**
	 * Constructs a new row based on the given table configuration. 
	 * 
	 * @param index
	 * @param tableConfiguration
	 */
	protected Row(long index, TableConfiguration tableConfiguration)
	{
		data = new int[tableConfiguration.getRowNumElements()];		
		this.index = index; 		
	}
	
	/**
	 * Overwrites the row with random data
	 */
	protected void update(WorkerThread thread)
	{
		for (int i = 0; i < data.length; i++)
		{
			data[i] = new Integer(thread.RANDOM_INSTANCE.nextInt()); 		 		
		}			
	}
	
	/**
	 * Gets the row's data. 
	 * 
	 * @return
	 */
	protected int[] getData()
	{
		return data; 
	}
	
	/**
	 * Gets the index
	 * 
	 * @return
	 */
	public long getIndex()
	{
		return index; 
	}
	
	/**
	 * Gets a string representation of the row
	 */
	public String toString()
	{
		return index + "";
	}
}