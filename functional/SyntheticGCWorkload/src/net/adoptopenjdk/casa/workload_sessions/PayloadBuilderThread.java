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
package net.adoptopenjdk.casa.workload_sessions;

import java.util.Random;

/**
 * The PayloadBuilder thread iterates the builders, absorbs balance from them and 
 * tries to allocate instances of those for which it has sufficient balance. 
 * 
 *  
 *
 */
public class PayloadBuilderThread extends Thread 
{
	public final Random RANDOM = new Random();
	private int id; 
	private PayloadBuilder[] builders;  
	private PayloadBuilderThreadPool threadPool; 
	//private volatile long throughput; 	  
	
	// Thread-local throughput counters for each builder. 
	private long[] throughput; 
	
	// Thread-local balances for each builder. 
	private double[] balance; 
	
	// The quantity of throughput unaccounted for in maintenance.  
	private long neverAdded;
	
	/**
	 * 
	 * 
	 * @param id
	 * @param threadPool
	 * @param workload
	 * @param builders
	 */
	public PayloadBuilderThread(int id, PayloadBuilderThreadPool threadPool, Workload workload, PayloadBuilder[] builders)
	{
		this.id = id; 
		this.builders = builders; 
		this.threadPool = threadPool; 
		this.throughput = new long[builders.length];
		this.balance = new double[builders.length]; 
		this.neverAdded = 0; 
		
	}

	/**
	 * Gets the number of threads in this thread's thread pool. 
	 * 
	 * @return
	 */
	protected int getNumThreads()
	{
		return threadPool.getNumThreads(); 
	}
	
	/**
	 * Adds the given number of bytes to the neverAdded 
	 * counter, accounting for bytes that are allocated but
	 * are never added to a container.   
	 * 
	 * @param size
	 */
	protected void addToNeverAdded(long size)
	{
		neverAdded += size; 
	}
	
	/**
	 * Gets the number of bytes allocated but never added to a container. 
	 * 
	 * @return
	 */
	protected long getBytesNeverAdded()
	{
		return neverAdded; 
	}
	
	/**
	 * Copies this thread's portion of the balance from the builder at index i. 
	 * 
	 * @param i
	 */
	protected void pullBalance(int i)
	{
		balance[i] = builders[i].getBalance() / (double)threadPool.getNumThreads();
	}
	
	/**
	 * Pulls this thread's portion of the blance from all builders.  
	 */
	protected void pullBalance()
	{
		for (int i = 0; i < builders.length; i++)
		{
			pullBalance(i);						
		}
	}

	/**
	 * Processes the given builder if it has sufficient balance and 
	 * records the resulting throughput.  
	 * 
	 * @param i
	 * @return
	 * @throws Throwable
	 */
	protected long processBuilder(int i) throws Throwable
	{
		if ((balance[i] - throughput[i] >= builders[i].getPayloadConfiguration().getSize()))
		{
			long size = builders[i].process(this);
			
			throughput[i] += size;
			
			return size; 
		}
		else 
		{			
			return 0;
		}		
	}
	
	/**
	 * Iterates builders and processes them until interrupted.  
	 */
	public void run()
	{		
		while (!Thread.interrupted())
		{					
			try 
			{		
				pullBalance();			
				
				boolean allocated; 
				
				do
				{
					allocated = false;
										
					for (int i = 0; i < builders.length; i++)
					{		
						if (builders[i].getContainer().isDone() || Thread.interrupted())
							throw new PayloadContainer.DoneException(); 
						
						if(processBuilder(i) != 0 && !allocated)						
							allocated = true;																			
					}
				}
				while (allocated);																			
			}										
			// All done, return so thread can die. Interrupt other threads so that they can do the same.  
			catch (PayloadContainer.DoneException | InterruptedException e) 
			{		
				threadPool.interruptAll(); 
				return; 
			}						
			// Something went wrong, interrupt all other threads too. 
			catch (Throwable e)
			{ 				
				Event.FATAL_ERROR.issue(e,"Producer thread threw runtime exception."); 				
				return; 
			}
		}
		
		threadPool.interruptAll(); 		
	}
	
	/**
	 * Gets the total number of bytes allocated by this thread. 
	 * 
	 * @return
	 */
	protected long getThroughput()
	{
		long count = 0; 
		
		for (int i = 0; i < throughput.length; i++)
		{
			count += throughput[i];
		}
		
		return count; 
	}

	/**
	 * Gets the thread's numeric ID, 0-referenced and unique within the workload. 
	 * 
	 * @return
	 */
	public int getID() 
	{
		return id; 
	}
}
