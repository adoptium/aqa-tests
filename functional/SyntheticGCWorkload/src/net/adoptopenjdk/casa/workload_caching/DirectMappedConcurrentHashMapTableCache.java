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

import java.util.concurrent.ConcurrentHashMap;

import net.adoptopenjdk.casa.util.Sizes;

/**
 * Simulates a remote database table accessed through a local cache. 
 * Configuration is given via a TableConfiguration object, parsed 
 * from the configuration file.   
 * 
 *  
 *
 */
public class DirectMappedConcurrentHashMapTableCache extends AbstractTableCache
{
	// Approximate overhead for each cache line  
	public static final long UNIT_OVERHEAD =
			AbstractTableCache.UNIT_OVERHEAD + 
			// Reference to the Row in the cache array. 
			Sizes.ATOMIC_REFERENCE_SIZE;
	
	// Rough count of the header and instance fields for the cache itself.
	public static final long OVERHEAD =
			AbstractTableCache.OVERHEAD + 
			// Reference to cache array 
			Sizes.REFERENCE_SIZE +
			// Cache reference array 
			Sizes.ATOMIC_REFERENCE_ARRAY_SIZE; 

	// Contains references to all items currently in cache. 
	private ConcurrentHashMap<Integer,Row> cache;
	
	/**
	 * Instantiates a new table cache with the given 
	 * configuration. Initializes data structures, and nullifies 
	 * all cache lines.  
	 * 
	 * @param tableConfiguration
	 */
	public DirectMappedConcurrentHashMapTableCache(Integer id, TableConfiguration tableConfiguration, CacheSimulation simulation)
	{
		super(id, tableConfiguration, simulation);
		
		cache = new ConcurrentHashMap<Integer, Row>();		
	}
	
	/**
	 * Frees memory taken by the cache. 
	 */
	public void shutdown()
	{
		cache = null; 
	}
	
	/**
	 * Asks the cache if all slots have been populated yet. 
	 */
	public boolean isPopulated()
	{
		if (cache == null)
			return false; 

		return cache.size() >= getConfiguration().getNumCacheRows();
	}
		
	/**
	 * Performs mapping of the given table index to the corresponding cache index. 
	 * 
	 * @param tableIndex
	 * @return
	 */
	private int getCacheIndex(long tableIndex)
	{
		// Direct mapping
		return Math.round(tableIndex % (getConfiguration().getNumCacheRows())); 
	}
	
	/**
	 * Queries the given rown in the imaginary table. 
	 */
	public Row query(long tableIndex, WorkerThread thread)
	{		
		Row newRow = null;		
		Row row = null;
		
		// Check the cache map for the requested row. 
		final int cacheIndex = getCacheIndex(tableIndex); 
		
		boolean hit = false; 
		
		/*
		 *  Keep trying to bring the Row into cache until we succeed
		 *  or find it already present.  
		 */		
		do 
		{																
			// Query the selected cache row. 
			row = cache.get(cacheIndex);
						
			// If the row is populated, it's either a hit, or we displace the existing item. 
			if (row != null && row.getIndex() == tableIndex)
			{									
				hit = true; 
				break; 
			}				
						
			// We only fabricate a new row if we haven't done so already.  																				
			if (newRow == null)
			{			
				// Create a new row with the given index. 
				newRow = new Row(tableIndex, getConfiguration());
			}
		} 
		// Repeat the entire algorithm if the cache line has changed. Otherwise, replace it with the new one.  
		while (row == null? null != cache.putIfAbsent(cacheIndex, newRow) : !cache.replace(cacheIndex, row, newRow)); 
			
		// Register hit or miss
		finalizeQuery(hit, thread);
				
		return hit ? row : newRow; 
	}	
}
