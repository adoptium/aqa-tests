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
 * Wraps a Row to return as a response to a Query. 
 * 
 *  
 *
 */
public class QueryResponse 
{	
	public static final long ELEMENT_SIZE = Sizes.INT_SIZE;	
	
	public int sum;  
	
	public Integer[] responseData; 
	
	/**
	 * Creates a QueryResponse wrapper for the given row
	 * 
	 * @param row
	 */
	public QueryResponse(Row row, int size)
	{	
		sum = 0;  
		// The size of the query response is capped by either the given size or the row size, whichever is smaller.  
		final int mySize = row.getData().length > size ? size : row.getData().length;  
		
		responseData = new Integer[mySize]; 
	
		for (int i = 0; i < mySize; i++)
		{
			responseData[i] = new Integer(row.getData()[i]); 
			sum += responseData[i];			
		}
	}	
		
}
