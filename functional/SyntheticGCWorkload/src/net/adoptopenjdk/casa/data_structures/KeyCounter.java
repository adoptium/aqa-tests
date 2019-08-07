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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A data structure which counts occurrences of each key. Keeps keys in 
 * the order they were first encountered or read. Each key is added by 
 * incrementing or getting its value. If a key is requested that isn't in the 
 * structure, its value is initialized to zero.   
 * 
 *  
 *
 * @param <K>
 */
public class KeyCounter <K> 
{
	private HashMap<K, AtomicLong> counts; 
	private ArrayList<K> keys; 
	
	public KeyCounter()
	{
		counts = new HashMap<K, AtomicLong >();
		keys = new ArrayList<K> ();
	}
	
	/**
	 * Starts the counter with the given keys in the given order 
	 * all initialized to zero. 
	 * 
	 * @param keys
	 */
	public KeyCounter(ArrayList<K> keys)
	{
		this(); 
		
		for (K key : keys)
			get(key);
	}
	
	/**
	 * Gets a set of keys either encountered or checked. 
	 * 
	 * @return
	 */
	public ArrayList<K> keySet()
	{
		return keys;
	}
	
	/**
	 * Gets the value of the given counter. 
	 * 
	 * @param key
	 * @return
	 */
	public long get(K key)
	{			
		return getAtomicLong(key).get();		 	
	}
	
	/**
	 * Atomically increments the key and returns the result 
	 * 
	 * @param key
	 * @return
	 */
	public long incrementAndGet(K key)
	{
		return getAtomicLong(key).incrementAndGet();
	}
	
	/**
	 * Gets the counter object associated with the given key 
	 * 
	 * @param key
	 * @return an AtomicLong
	 */
	private AtomicLong getAtomicLong(K key)
	{
		AtomicLong value = counts.get(key);
		
		if (value == null)
		{
			synchronized(counts) 
			{
				if (!counts.containsKey(key)) 
				{ 
					counts.put(key, new AtomicLong (0));
					keys.add(key);
				}
				
				value = counts.get(key);
			}
		}
		
		return value; 
	}
}
