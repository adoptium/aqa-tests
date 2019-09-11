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
import java.util.concurrent.atomic.AtomicInteger;
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
public class FullyAssociativeTableCache extends AbstractTableCache
{
	// Approximate overhead for each cache line  
	public static final long UNIT_OVERHEAD = 
			AbstractTableCache.UNIT_OVERHEAD + 
			// Cache map entry size. 
			Sizes.REFERENCE_SIZE + Sizes.MIN_OBJECT_SIZE + Sizes.LONG_SIZE +  
			// Reference to the Row in the cache array. 
			Sizes.ATOMIC_REFERENCE_SIZE;
	
	
	// Rough count of the header and instance fields for the cache itself.
	public static final long OVERHEAD = 
			AbstractTableCache.OVERHEAD + 
			// Reference to cache array 
			Sizes.REFERENCE_SIZE +
			// Cache reference array 
			Sizes.ATOMIC_REFERENCE_ARRAY_SIZE +  			
			// Reference to cache map  
			Sizes.REFERENCE_SIZE + 			
			// Cache Map 
			Sizes.MIN_OBJECT_SIZE; // Expand 
	
	// Contains references to all items currently in cache. 
	private AtomicReferenceArray<Row> cache;
	
	// Maps imaginary table IDs to cache lines 
	private ConcurrentHashMap<Long, Integer> cacheMap;

	// 
	private AtomicInteger nextUninitializedCacheIndex = new AtomicInteger(0);
	
	/**
	 * Instantiates a new table cache with the given 
	 * configuration. Initializes data structures, and nullifies 
	 * all cache lines.  
	 * 
	 * @param tableConfiguration
	 */
	public FullyAssociativeTableCache(Integer id, TableConfiguration tableConfiguration, CacheSimulation simulation)
	{
		super(id, tableConfiguration, simulation);
				
		cache = new AtomicReferenceArray<Row>(tableConfiguration.getNumCacheRows());
		
		for (int i = 0; i < tableConfiguration.getNumCacheRows(); i++)
			cache.set(i, null);
		
		cacheMap = new ConcurrentHashMap<Long, Integer>();
	}
	
	/**
	 * Frees memory taken by cache structures. 
	 */
	public void shutdown()
	{
		cache = null; 
		cacheMap = null; 
	}

	/**
	 * Checks to see if all cache lines are used. 
	 */
	public boolean isPopulated()
	{
		if (cacheMap != null)
			return cacheMap.size() >= getConfiguration().getNumCacheRows();
		else
			return false; 
	}
	
	
	private int getNextCacheLineIndex(WorkerThread thread)
	{
		int index;
		int newIndex; 
		
		do
		{	
			index    = nextUninitializedCacheIndex.get(); 
			newIndex = index + 1; 
		}
		while (index < getConfiguration().getNumCacheRows() && !nextUninitializedCacheIndex.compareAndSet(index, newIndex));
		
		if (index < getConfiguration().getNumCacheRows())
			return index; 
		else 
			return thread.RANDOM_INSTANCE.nextInt(getConfiguration().getNumCacheRows());
	}
		
	/**
	 * Queries the database for the given table index through 
	 * the cache. If the row is in the cache, it is returned from the cache 
	 * and tableConfiguration.getHitDelay() is incurred. If the 
	 * row is not in the cache, tableConfiguration.getMissDelay() is 
	 * incurred, a new random Row is created and cached and then this 
	 * new Row is returned.  
	 * 
	 * The algorithm is as follows: 
	 * 
	 * 1 - If the row id is mapped to cache pull the line it's mapped to. 
	 * Otherwise, pick a random cache index and atomically update the cache 
	 * map to point the row ID to it, then pull the contents of the cache 
	 * line.
	 * 2 - If the pulled cache line has the id we're looking for, register 
	 * a hit, incur the hit delay and return it. Otherwise, remove the 
	 * item currently in the cache line from the cache map, generate a
	 * new Row, incur the miss delay and try to add it to the cache line.  
	 * 3 - If adding the new Row fails (due to the line having been changed), 
	 * repeat the algorithm. The previously generated Row is used if a miss 
	 * is again registered and the miss delay is not incurred a second time.   
	 * 
	 * @param tableIndex
	 * @return
	 * @throws InterruptedException
	 */
	public Row query(long tableIndex, WorkerThread thread)
	{
		Integer cacheIndex;
		
		Row newRow = null;		
		Row row = null;
		
		boolean hit = false; 
		
		/*
		 *  Keep trying to bring the Row into cache until we succeed
		 *  or find it already present.  
		 */
		do 
		{
			// Check the cache map for the requested row. 
			cacheIndex = cacheMap.get(tableIndex);
		
			// If the ID is not mapped to any cache line, map it to a random cache line.  
			if (cacheIndex == null)			
			{						
				// Pick a random cache row  
				cacheIndex = getNextCacheLineIndex(thread);
				
				// Try to map the table ID to the random row
				Integer previous = cacheMap.putIfAbsent(tableIndex, cacheIndex);
				
				// If the table ID was mapped to a row already, use the existing mapping
				if (previous != null)
					cacheIndex = previous; 
			}					
			
			// Query the selected cache row. 
			row = cache.get(cacheIndex);
						
			// If the row is populated, it's either a hit, or we displace the existing item. 
			if (row != null)
			{
				// If the cache line already contains the requested item, return it. 
				if (row.getIndex() == tableIndex)
				{					
					hit = true; 
					break; 
				}
				else
				{
					// Attempt to displace the existing item from the cache line 
					cacheMap.remove(row.getIndex(), cacheIndex);																						
				}
			}				
			
			// We only fabricate a new row if we haven't done so already.  																				
			if (newRow == null)
			{
				// Create a new row with the given index. 
				newRow = new Row(tableIndex,getConfiguration());				
			}
		} 
		// Repeat the entire algorithm if the cache line has changed. 
		while (!cache.compareAndSet(cacheIndex, row, newRow)); 
		
		// Register hit or miss
		finalizeQuery(hit, thread);
		
		return hit ? row : newRow; 		
	}
}
