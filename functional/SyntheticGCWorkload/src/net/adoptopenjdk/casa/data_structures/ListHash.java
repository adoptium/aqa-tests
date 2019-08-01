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
import java.util.Set;

/**
 * A structure which hashes keys to lists of items. All items are 
 * also stored in a primary list so that the structure may be 
 * iterated over.
 * 
 * Useful for parser symbol tables. 
 * 
 *  
 *
 * @param <K>
 * @param <V>
 */
public class ListHash <K, V> extends ArrayList<V>  
{
	private static final long serialVersionUID = 1L;
	private HashMap<K, ArrayList<V>> hash; 
	
	/**
	 * Constructs a new list hash. 
	 */
	public ListHash()
	{
		super();  
		hash = new HashMap<K, ArrayList<V>>(); 
	}
	
	/**
	 * Adds the item to the null bucket and the primary list. 
	 */
	public boolean add(V item)
	{
		this.add(null, item);	
		return true; 
	}
	
	/**
	 * Adds the item to the null bucket and the primary list at the given index. 
	 */	
	public void add(int index, V item)
	{
		super.add(index, item);
		getList(null).add(item);
	}
			
	/**
	 * Adds the item to the given bucket and the primary list. 
	 * 
	 * @param key
	 * @param item
	 */
	public void add(K key, V item)
	{	
		super.add(item);
		getList(key).add(item);	
	}
	
	/**
	 * Gets the list contained within the given bucket. Adds a 
	 * new empty list to the bucket if it has not yet been 
	 * initialized and returns the new list. 
	 * 
	 * @param key
	 * @return
	 */
	public ArrayList<V> getList(K key)
	{
		if (!hash.containsKey(key)) 
			hash.put(key, new ArrayList<V>());			 
				
		return hash.get(key);
	}
	
	/**
	 * Gets the item with the given index in the given bucket. 
	 * 
	 * @param key
	 * @param index
	 * @return
	 */
	public V get(K key, int index)
	{
		return getList(key).get(index);
	}
	
	/**
	 * Gets the item at the given index in the primary list. 
	 */
	public V get(int index)
	{
		return super.get(index);
	}
	
	/**
	 * Checks to see if a bucket exists with the given key. 
	 * 
	 * @param key
	 * @return
	 */
	public boolean containsKey(K key)
	{
		return hash.containsKey(key);
	}
	
	/**
	 * Gets the keys for all buckets. 
	 * 
	 * @return
	 */
	public Set<K> keySet()
	{
		return hash.keySet();
	}
	
	/**
	 * Gets the size of the given bucket. If the bucket does not exist, 
	 * an empty bucket will be added by getList(). 
	 * 	
	 * @param key
	 * @return
	 */
	public int size(K key)
	{	
		return getList(key).size(); 
	}
	
	/**
	 * Gets the size of the primary list containing all 
	 * items that have been added. 
	 */
	public int size()
	{
		return super.size(); 
	}
}
