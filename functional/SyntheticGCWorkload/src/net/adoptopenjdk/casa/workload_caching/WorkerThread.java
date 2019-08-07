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

import java.util.Random;

/**
 * Worker threads perform transactions on the cache   
 * 
 *  
 */
public class WorkerThread extends Thread	
{			
	// Thread-local random instance. Sharing an instance between threads causes significant contention.  
	protected final Random RANDOM_INSTANCE; 
	
	private final CacheSimulation simulation; 
	
	// 0-referenced id, unique within the simulation only. 
	private final int id; 
	
	// Thread-local hit and miss counters, one element per cache. 
	private final long[] hits, misses;
	
	/**
	 * Initializes the thread with thread local data buffers and RANDOM_INSTANCE. 
	 * 
	 * @param simulation
	 * @param id
	 */
	protected WorkerThread(CacheSimulation simulation, int id)
	{
		this.RANDOM_INSTANCE = new Random(); 
		this.simulation = simulation;
		this.id = id;
		
		
		hits   = new long[simulation.getConfiguration().getNumTables()];
		for (int i = 0; i < hits.length; i++)
			hits[i] = 0; 
		
		misses = new long[simulation.getConfiguration().getNumTables()];
		for (int i = 0; i < misses.length; i++)
			misses[i] = 0;
	}
	
	/**
	 * Increments the hit counter for the given table ID, referenced from 1. 
	 * 
	 * @param tableID
	 */
	protected void incrementHits(int tableID)
	{
		hits[tableID - 1]++; 
	}
	
	/**
	 * Increments the miss counter for the given table ID, referenced from 1. 
	 * @param tableID
	 */
	protected void incrementMisses(int tableID)
	{
		misses[tableID - 1]++; 
	}
	
	/**
	 * Gets the hit counter for the given table ID, referenced from 1. 
	 * 
	 * @param tableID
	 * @return
	 */
	protected long getHits(int tableID)
	{
		return hits[tableID - 1]; 
	}
	
	/**
	 * Gets the miss counter for the given table ID, referenced from 1. 
	 * 
	 * @param tableID
	 * @return
	 */
	protected long getMisses(int tableID)
	{
		return misses[tableID - 1]; 
	}
	
	/**
	 * Gets the thread's 0-referenced ID, unique within the simulation only. 
	 * 
	 * @return
	 */
	protected int getID()
	{
		return id; 
	}
					
	/**
	 * Continually calls simulation.executeSimulationSamplingCycle() until interrupted. 
	 */
	public void run()
	{		
		try 
		{
			// Run until interrupted. 
			while (!Thread.interrupted())
			{	
				// Execute a series of transactions 
				simulation.executeSimulationSamplingCycle(this); 
			}
		}
		catch (Throwable t)
		{
			Event.FATAL_ERROR.issue(t, "unexpected error in WorkerThread");
		}
	}			
}