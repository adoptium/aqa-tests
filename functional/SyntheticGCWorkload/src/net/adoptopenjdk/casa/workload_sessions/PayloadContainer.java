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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.payload.Payload;

/**
 * Container for Payloads. Multiple Payloads can be added concurrently. Maintenance 
 * begins after a call to startup()   
 * 
 *  
 */
public class PayloadContainer 
{	
	// The container contains p * 10 lists where p is the number of processors. 
	private final int NUM_PAYLOAD_LISTS;
	
	// The Payload container divides the Payloads among NUM_PAYLOAD_LISTS separate linked lists   
	private AtomicReferenceArray<Payload> payloadListHeads;
		
	// Indicates whether or not the container has shutdown. 
	private volatile boolean done; 	
	
	// The number of bytes removed during maintenance.   
	private volatile long removed;
	
	// Thread responsible for performing cleanup operations on the container. 
	private final MaintenanceThread maintenanceThread; 
		
	// Thrown when the container is done and therefore cannot perform the requested operation. 
	public static class DoneException extends Exception { private static final long serialVersionUID = 1L; }	
	
	// The next container list to add to. 
	private int nextList; 

	// The workload this container belongs to. 
	private Workload workload; 

	/**
	 * Instantiates a new payload container and creates and starts the maintence thread. 
	 * 
	 * @param capacity
	 * @param maintenancePeriod
	 * @param maintenanceHighWaterMark
	 */
	public PayloadContainer (Workload workload, PayloadBuilderSet set)
	{		
		NUM_PAYLOAD_LISTS = set.getConfiguration().getNumPayloadContainerLists(); 
		
		this.workload = workload; 
		this.removed = 0; 
		this.done = false;  
		this.maintenanceThread = new MaintenanceThread(); 
		this.payloadListHeads = new AtomicReferenceArray<Payload>(NUM_PAYLOAD_LISTS);
		this.nextList = 0; 
	}

	/**
	 * Gets the nextList and increments the nextList variable circularly. 
	 * 
	 * @return
	 */
	protected int getNextListIndex()
	{
		// Range check the counter...pick a random valid index if range check fails. 
		int listID = nextList;
		
		listID = listID < NUM_PAYLOAD_LISTS ? listID : 0;
		
		// Circularly increment the counter in a thread-safe way. (although some updates might be lost)  
		nextList = listID < NUM_PAYLOAD_LISTS - 1 ? listID + 1 : 0;
		
		return listID; 
	}
	
	/**
	 * Adds a Payload to one ofthe container's lists.       
	 * 	
	 * @param payload
	 * @throws OutOfMemoryError
	 * @throws DoneException
	 * @throws Throwable
	 */
	protected void add(Payload payload, PayloadBuilderThread thread) throws OutOfMemoryError, Throwable, DoneException
	{				
		if (isDone())
			throw new DoneException(); 
		
		try 
		{		
			/*
			 *  If the payload has already expired, just count its throughput and free
			 *  its allocated space. It never actually occupies any space in the 
			 *  container.  
			 */			 
			if (!payload.isExpired()) 
			{
				int listID;   
			
				// Atomically add the new Payload to the head of the list. 
				Payload next;					
				do 
				{			
					listID = getNextListIndex();
					
					next = payloadListHeads.get(listID); 			
					payload.setNext(next);			
				} 
				// If the head has not changed, the add succeeds. 
				while (!payloadListHeads.compareAndSet(listID, next, payload)); 								
			}
			else 
			{
				thread.addToNeverAdded(payload.getSize()); 
			}
		}
		catch (Throwable e) 
		{
			throw e; 
		}
	}
	
	/**
	 * Removes expired items from the container. Blocks until the 
	 * cleanup lock can be acquired.  
	 * 
	 * @throws DoneException
	 * @throws Throwable
	 */
	private void cleanupContainer() throws DoneException, Throwable
	{	
		if (isDone())
			throw new DoneException(); 
						 																	
		removeExpired();																								
	}
	
	/**
	 * Removes expired Payloads from all of the lists in sequential order from 0 to 
	 * NUM_PAYLOAD_LISTS-1. 
	 * 
	 * @throws DoneException
	 */
	private void removeExpired() throws DoneException
	{	
		synchronized(maintenanceThread)
		{
			for (int i = 0; i < NUM_PAYLOAD_LISTS; i++)
			{
				if (isDone())
					throw new DoneException();
				
				removeExpired(i);
			}			
		}
	}
	
	/**
	 * Iterates the given list and removes all expired Payloads. This function may be 
	 * Executed concurrently with allocation and addition of Payloads. It may not 
	 * be executed concurrently with itself for the same listID as it isn't atomic on the 
	 * next pointers, but may be executed in parallel as long as parallel instances have different list ID values. 
	 *   
	 * @throws DoneException 
	 */
	private void removeExpired(int listID) throws DoneException 
	{			
		AtomicReference<Payload> node; 		
			
		Payload payload;		
		
		Payload head = payloadListHeads.get(listID); 
		
		// If there's no second node (or no first node), there's not much to do.  
		if (head == null || head.getNext().get() == null)
		{						
			// See if the head node is expired. 
			if (head != null && head.isExpired() && payloadListHeads.compareAndSet(listID, head, head.getNext().get())) 
				removed += head.getSize();
						 
			return; 
		}
		// If there is a second node, start with it to reduce contention on the head node.
		else 
		{		
			node = head.getNext(); 
			payload = head.getNext().get();
		}
						
		while (payload != null) 
		{									
			// If the payload is expired, try to remove it. 
			if (payload.isExpired() && node.compareAndSet(payload, payload.getNext().get())) 
				removed += payload.getSize();							
			// If the Payload at the node has changed it'll be further down the list now...continue iterating.   
			else 			 
				node = payload.getNext();							

			// If the removal succeeded, node will already be the next node. 
			payload = node.get();					
		}		
	
	}

	/**
	 * Empties all lists, effectively emptying the container 
	 * 
	 * @throws DoneException
	 */
	private void removeAll() throws DoneException 
	{		
		synchronized(maintenanceThread)
		{
			for (int i = 0; i < NUM_PAYLOAD_LISTS; i++)
			{
				removeAll(i);			
			}						
		}
	}
	
	/**
	 * Empties the given list by continually removing the head node.
	 *  
	 * @param listID - the list to empty 
	 * @throws DoneException 
	 */
	private void removeAll(int listID) throws DoneException
	{			
		Payload payload = payloadListHeads.get(listID); 
		
		while (payload != null) 
		{	
			// Keep trying to remove the Payload at the head node.  
			if (payloadListHeads.compareAndSet(listID, payload, payload.getNext().get()))
				removed += payload.getSize();
			
			payload = payloadListHeads.get(listID); 
		}							
	}
	
	/**
	 * Returns true if the container has shut down  
	 * 
	 * @return
	 */
	protected boolean isDone() 
	{ 
		return done; 
	}
	
	/**
	 * Performs a nonblocking minimal shutdown by setting the done flag.  
	 */
	protected void kill()
	{		
		maintenanceThread.interrupt();
		done = true;
	}
	
	/**
	 * Starts the maintenanceThread. 
	 */
	protected void startup() 
	{
		maintenanceThread.start(); 
	}
	
	/**
	 * Sets "done" flag, shuts down maintenance thread, removes all objects from the Payload container
	 * and returns. 
	 * 
	 * All but the first call will do nothing.  
	 * 
	 * Note: this call may block indefinitely. 
	 * 
	 * @throws InterruptedException
	 */
	protected void shutdown() throws InterruptedException
	{
		synchronized(this)
		{
			if (done)
				return;
			
			done = true; 
		}
		
		// Stop maintenance
		maintenanceThread.interrupt();		
		maintenanceThread.join();
							
		try 
		{			
			removeAll();						
		}
		catch (Throwable e) 
		{			 		
			Event.FATAL_ERROR.issue(e, "in setDone()");										
		}
	}
		
	/**
	 * Gets space occupied by Payloads that have been added and not removed. 
	 * 
	 * @return
	 */
	protected long getBytesRemoved() 
	{
		return removed; 
	}

	/**
	 * The maintenance thread performs periodic cleanup of the container. 
	 * 
	 *  
	 */
	private class MaintenanceThread extends Thread 
	{				
		/**
		 * Creates a new MaintenanceThread. Call start() to begin execution. 
		 */
		public MaintenanceThread()
		{
			setDaemon(true);
		}	
						
		/**
		 * Runs maintenance routine until shutdown() is called.
		 */
		public void run() 
		{	
			// The idle period is configured per-workload 
			final double IDLE_PERIOD = workload.getWorkloadConfiguration().getMaintenancePeriod(); 			
			
			try 
			{					
				while (true) 
				{		
					if (isDone())
						return; 
					
					try 					
					{
						double startTime = Utilities.getTime(); 
												
						// Perform container cleanup. 
						cleanupContainer();
						
						// Check to see if we've been interrupted. If so, skip idle phase 		
						if (!Thread.interrupted())
						{
							// Wait for the next cleanup cycle.						 
							Utilities.idle(IDLE_PERIOD - (Utilities.getTime() - startTime));
						}						
					}
					// cleanupContainer() will throw the DoneException if the container is done. 					 
					catch (PayloadContainer.DoneException e) 
					{											
						break; 
					}
					/* 
					 * An interrupt to the MaintenanceThread will trigger an immediate cleanup 
					 * operation, unless the container is done.  
					 */
					catch (InterruptedException e) 
					{							
						// Perform cleanup or die, depending on the return value from isDone()
					}					
				}		
			} 			
			/* All other exceptions should be treated as fatal. Most 
			 * likely these are OutOfMemory errors. 
			 */
			catch (Throwable e)
			{
				Event.RUNTIME_EXCEPTION.issue(e, "In maintenance thread.");
				System.exit(-100);
			}
		}		
	}
}
