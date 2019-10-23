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
 * A unified interface for using different kinds of simulated 
 * table caches. 
 *  
 *  
 *
 */
public interface TableCache 
{
	// Approximate overhead for each cache line  
	public static final long UNIT_OVERHEAD = 			
			// Overhead reported by the row. 
			Row.OVERHEAD;
	
	// Rough count of the header and instance fields for the cache itself.
	public static final long OVERHEAD = 
			// The TableCache instance 
			Sizes.MIN_OBJECT_SIZE; 
	
	public int getID(); 
	
	/**
	 * Returns the number of hits registered by the cache up 
	 * to this point. Updated immediately after every hit. 
	 * 
	 * @return
	 */
	public long getNumHits(); 	

	/**
	 * Returns the number of misses registered by the cache 
	 * up to this point. Updated immediately after every miss. 
	 * 
	 * @return
	 */
	public long getNumMisses();
	
	/**
	 * Gets the total number of completed transactions by summing 
	 * the number of hits and misses. 
	 * 
	 * @return
	 */
	public long getNumCompletedTransactions(); 
	
	/**
	 * Returns a random index from the imaginary table. 
	 * 
	 * @return
	 */
	public long getRandomRowIndex(WorkerThread thread);
	
	/**
	 * Calls query on a random index from the table and returns 
	 * the row wrapped in a QueryResponse
	 */	
	public QueryResponse queryRandomRow(WorkerThread thread);	
	
	/**
	 * Performs an update (row.update) on some random row in 
	 * the database. This involves querying a random row and 
	 * then calling update on that row.  
	 */
	public void updateRandomRow(WorkerThread thread); 
	
	/**
	 * Checks to see if all cache lines currently hold Row 
	 * instances, provided the structures support this feature. If 
	 * this feature is not supported, true is returned unconditionally.  
	 * 
	 * @return
	 */
	public boolean isPopulated(); 
	
	/**
	 * Queries the given table index and calls Row.update() on 
	 * the resulting row. 
	 * 
	 * @param tableIndex
	 */
	public void update(long tableIndex, WorkerThread thread);
	
	/**
	 * Queries the given row via the cache and returns it. 
	 * 
	 * @param tableIndex
	 * @return
	 */
	public Row query(long tableIndex, WorkerThread thread);
	
	/**
	 * Gets the table configuration used to construct this table cache
	 * 
	 * @return
	 */
	public TableConfiguration getConfiguration();
	
	/**
	 * Frees any structures no longer needed after simulation 
	 * has ended. Retains stats and counters. 
	 */
	public void shutdown(); 
	
	/**
	 * Gets the number of cache hits to date divided by the total 
	 * number of queries.  
	 * 
	 * @return
	 */
	public double getHitRate();
}
