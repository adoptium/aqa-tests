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
package net.adoptopenjdk.casa.data_structures;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple atomic mutex written using AtomicBoolean. 
 * 
 *  
 *
 */
public class AtomicMutex 
{	
	private volatile AtomicBoolean lock;  
	
	/**
	 * Creates a new instance. The instance is unlocked by default. 
	 */
	public AtomicMutex()
	{
		lock = new AtomicBoolean(false); 
	}
	
	/**
	 * Creates a new instance which is locked iff 
	 * the "locked" parameter is true.  
	 * 
	 * @param locked
	 */
	public AtomicMutex(boolean locked)
	{
		lock = new AtomicBoolean(locked); 
	}
	
	/**
	 * Attempt to get the mutex. Returns false if the mutex is already taken. 
	 * 
	 * @return
	 */
	public boolean acquire()
	{
		return lock.compareAndSet(false, true);				
	}
	
	/**
	 * Blocks until the lock has been acquired using a spin lock. 
	 */
	public void blockingAcquire()
	{
		while (!acquire());		
	}
	
	/**
	 * Unconditionally release the mutex. 
	 */
	public void release()
	{
		lock.set(false);				
	}
	
	/**
	 * Tries to release the lock. Returns false if the lock is not set. 
	 * 
	 * @return
	 */
	public boolean releaseIfSet()
	{
		return lock.compareAndSet(true, false);
	}
	
	/**
	 * Returns true if the lock is locked. 
	 * 
	 * @return
	 */
	public boolean isLocked()
	{
		return lock.get(); 
	}
}
