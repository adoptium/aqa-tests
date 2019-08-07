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

import java.util.concurrent.atomic.AtomicReferenceArray;

import net.adoptopenjdk.casa.util.Sizes;

/**
 * Simulates a remote database table accessed through a local cache. 
 * Configuration is given via a TableConfiguration object, parsed 
 * from the configuration file.   
 * 
 *  
 *
 */
public class DirectMappedTableCache extends AbstractTableCache
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
	private AtomicReferenceArray<Row> cache;
	
	/**
	 * Instantiates a new table cache with the given 
	 * configuration. Initializes data structures, and nullifies 
	 * all cache lines.  
	 * 
	 * @param tableConfiguration
	 */
	public DirectMappedTableCache(Integer id, TableConfiguration tableConfiguration, CacheSimulation simulation)
	{
		super(id, tableConfiguration, simulation);
		
		cache = new AtomicReferenceArray<Row>(tableConfiguration.getNumCacheRows());
		
		// Set all slots in cache to null. 
		for (int i = 0; i < tableConfiguration.getNumCacheRows(); i++)
			cache.set(i, null);		
	}
	
	/**
	 * Nullifies the pointer to the cache, allowing it to be GC'd 
	 */
	public void shutdown()
	{
		cache = null; 
	}
	
	/**
	 * Returns true for this cache type as it doesn't support a fast 
	 * way to check to see if all lines are populated. 
	 * 
	 * Normally this method would only return true if all lines contained data. 
	 */
	public boolean isPopulated()
	{		
		return true; 
	}
	
	/**
	 * Gets the index in cache that the given index maps to. For this type, 
	 * this is simply the modulus over the cache size. 
	 * 
	 * @param tableIndex
	 * @return
	 */
	private int getCacheIndex(long tableIndex)
	{
		// Direct mapping
		return Math.round(tableIndex % (getConfiguration().getNumCacheRows() - 1)); 
	}
	
	/**
	 * Queries the given row in the imaginary table. 
	 * 
	 * In this implementation, the table index maps to a 
	 * particular line in cache. We check that line and return 
	 * the Row stored there if it is the one we want. Otherwise, we 
	 * replace it with a new Row. 
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
			if (row != null && (row.getIndex() == tableIndex))
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
		// Repeat the entire algorithm if the cache line has changed. 
		while (!cache.compareAndSet(cacheIndex, row, newRow)); 
		
		// Register hit or miss
		finalizeQuery(hit, thread);
		
		return hit ? row : newRow; 		
	}	
}
