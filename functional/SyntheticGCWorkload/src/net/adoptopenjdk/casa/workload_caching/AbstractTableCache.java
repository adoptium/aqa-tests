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
import net.adoptopenjdk.casa.util.Utilities;

/**
 * Skeleton for a table class including hit/miss 
 * accounting and configuration 
 *  
 *  
 */
public abstract class AbstractTableCache implements TableCache
{	
	// Approximate overhead for each cache line  
	public static final long UNIT_OVERHEAD = TableCache.UNIT_OVERHEAD; 
	
	// Rough count of the header and instance fields for the cache itself.
	public static final long OVERHEAD = TableCache.UNIT_OVERHEAD + 
			// Configuration reference 
			Sizes.REFERENCE_SIZE + 			
			// Reference to hits 
			Sizes.REFERENCE_SIZE +
			// Hits 
			Sizes.LONG_SIZE + 
			// References to misses 
			Sizes.REFERENCE_SIZE + 
			// Misses 
			Sizes.LONG_SIZE; 
	
	// The configuration for this table as parsed from the config file 
	private TableConfiguration tableConfiguration; 
	private CacheSimulation simulation;
	private int id; 
	
	/**
	 * 
	 * 
	 * @param tableConfiguration
	 */
	public AbstractTableCache(Integer id, TableConfiguration tableConfiguration, CacheSimulation simulation)
	{		 
		this.tableConfiguration = tableConfiguration;
		this.simulation = simulation;
		this.id = id; 
	}
	
	public int getID()
	{
		return id; 
	}
	
	/**
	 * Increments appropriate counters to register a hit or miss and 
	 * idles for the prescribed amount of time, depending. 
	 * 
	 * If interrupted, it preserves the interrupt flag and returns. 
	 * 
	 * @param hit
	 */
	public void finalizeQuery(boolean hit, WorkerThread thread)
	{
		if (simulation.isStarted())
		{
			// Increment hit/miss counter. 
			if (hit) incrementNumHits(thread); 			
			else     incrementNumMisses(thread);		
		}
			
		try 
		{ 
			// Idle for the configured time 
			if (hit) Utilities.idle(getConfiguration().getHitDelay());
			else     Utilities.idle(getConfiguration().getMissDelay()); 
		}
		catch (InterruptedException e) 
		{ 
			// Restore the interrupt flag. 
			Thread.currentThread().interrupt(); 
		}
	}
	
	/**
	 * Calls query on a random index from the table. 
	 * 
	 * @throws InterruptedException
	 */
	public QueryResponse queryRandomRow(WorkerThread thread)
	{					
		return new QueryResponse(query(getRandomRowIndex(thread), thread), tableConfiguration.getResponseElements());
	} 
	
	/**
	 * Performs an update (row.update) on some random row in 
	 * the database. This involves querying a random row and 
	 * then calling update on that row.  
	 * 
	 * @throws InterruptedException
	 */
	public void updateRandomRow(WorkerThread thread) 
	{						
		update(getRandomRowIndex(thread), thread);
	}
		
	/**
	 * Queries the given table index and calls Row.update() on 
	 * the resulting row. 
	 * 
	 * @param tableIndex
	 * @throws InterruptedException
	 */
	public void update(long tableIndex, WorkerThread thread)
	{
		query(tableIndex, thread).update(thread);				
	}	
	
	/**
	 * Returns a random index from the imaginary table. 
	 * 
	 * TODO: The generated index is (probably) not uniformly distributed and may actually be range limited.  
	 * @param randomInstance 
	 * 
	 * @return
	 */
	public long getRandomRowIndex(WorkerThread thread)
	{
		// Generate random index, retrying if the index turns out to be negative.   
		long index; 
		do 
		{
			index = (thread.RANDOM_INSTANCE.nextLong()) % getConfiguration().getNumRows();			
		} 
		while (index < 0);			
		
		return index; 
	}
	
	/**
	 * Returns the number of hits registered by the cache up 
	 * to this point. Updated immediately after every hit. 
	 * 
	 * @return
	 */
	public long getNumHits()
	{
		long count = 0;
		
		WorkerThread[] threads = simulation.getWorkerThreads();
		
		for (int i = 0; i < threads.length; i++)
			count += threads[i].getHits(tableConfiguration.getID()); 
				
		return count; 
	}
	
	/**
	 * Returns the number of misses registered by the cache 
	 * up to this point. Updated immediately after every miss. 
	 * 
	 * @return
	 */
	public long getNumMisses()
	{
		long count = 0;
		
		WorkerThread[] threads = simulation.getWorkerThreads();
		
		for (int i = 0; i < threads.length; i++)
			count += threads[i].getMisses(tableConfiguration.getID()); 
				
		return count; 
	}
	
	/**
	 * Gets the sum of the hits and misses. 
	 */
	public long getNumCompletedTransactions()
	{
		return getNumHits() + getNumMisses(); 
	}
	
	/**
	 * Gets the number of hits divided by the total number of transactions. 
	 * 
	 * @return
	 */
	public double getHitRate() 
	{		
		return (getNumHits() > 0) ? (getNumHits() / (double)getNumCompletedTransactions()) : 0.0;
	}
	
	/**
	 * Increments the number of hits in a thread-safe manner.  
	 */
	public void incrementNumHits(WorkerThread thread)
	{
		thread.incrementHits(tableConfiguration.getID());
	}
	
	/**
	 * Increments the number of misses in a thread-safe manner.  
	 */
	public void incrementNumMisses(WorkerThread thread)
	{
		thread.incrementMisses(tableConfiguration.getID());
	}
	
	/**
	 * Gets the table configuration used to construct this table cache
	 * 
	 * @return
	 */
	public TableConfiguration getConfiguration()
	{
		return tableConfiguration; 
	}
}
