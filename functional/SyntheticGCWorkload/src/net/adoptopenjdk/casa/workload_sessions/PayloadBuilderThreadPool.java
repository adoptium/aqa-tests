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

/**
 * A thread pool which polls builders and processes 
 * them, executing allocations according to their available balance.   
 * 
 *  
 */
public class PayloadBuilderThreadPool
{ 
	private PayloadBuilderThread[] threads; 	
	
	/**
	 * Creates thread pool. Call start() to create and start threads.  
	 * 
	 * @param numberOfThreads
	 * @param idleTime
	 * @param queue
	 * @param handler
	 */
	public PayloadBuilderThreadPool(int numberOfThreads)
	{ 		
		threads = new PayloadBuilderThread[numberOfThreads];			 
	}
		
	/**
	 * Starts all threads processing the builders . 
	 * 
	 * @param container
	 */
	public void start(final Workload workload, final PayloadBuilder[] builders)
	{		
		// Create threads 
		for (int i = 0; i < threads.length; i++)
		{					
			// Each thread iterates over the builders until interrupted. 
			threads[i] = new PayloadBuilderThread(i, this, workload, builders);				
		}
			
		// Start threads. 
		for (Thread t : threads)
		{					
			t.start();
		}				
	}
	
	/**
	 * Gets the sum of the throughput (bytes allocated) for all threads 
	 * and all builders. 
	 * 
	 * @return
	 */
	protected long getThroughput()
	{
		long throughput = 0; 
				
		if (threads != null)
		{
			for (int i = 0; i < threads.length; i++)
			{
				if (threads[i] != null)
					throughput += threads[i].getThroughput();
			}
		}
				
		return throughput;
			
	}
	
	/**
	 * Gets the number of that were not added to the container 
	 * and therefore would not be seen by the maintenance thread.  
	 * 
	 * @return
	 */
	protected long getBytesNeverAdded()
	{
		long count = 0; 
		
		if (threads != null)
		{
			for (int i = 0; i < threads.length; i++)
			{
				if (threads[i] != null)
					count += threads[i].getBytesNeverAdded();
			}
		}
		
		return count; 		
	}
	
	/**
	 * Calls interrupt() on all threads. 
	 */
	protected void interruptAll()
	{		
		for (int i = 0; i < threads.length; i++)
		{			
			threads[i].interrupt();
		}
	}
	
	/**
	 * Interrupts all threads and blocks until they complete 
	 * 
	 * @throws InterruptedException
	 */
	public void shutdown() throws InterruptedException
	{			
		interruptAll(); 
		
		for (int i = 0; i < threads.length; i++)
		{			
			threads[i].join();
		}
	}
	
	/**
	 * Interrupts all threads but does not block for them to complete 
	 */
	public void kill()
	{
		interruptAll(); 		
	}

	/**
	 * Gets the number of threads in this pool. 
	 * 
	 * @return
	 */
	public int getNumThreads() 
	{
		return threads.length; 
	}
}
