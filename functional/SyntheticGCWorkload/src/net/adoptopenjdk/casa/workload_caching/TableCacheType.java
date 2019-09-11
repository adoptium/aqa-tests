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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Maps the type of cache the user requests to a particular implementation of TableCache
 * 
 *  
 *
 */
public enum TableCacheType 
{
	FULLY_ASSOCIATIVE(FullyAssociativeTableCache.class), 
	DIRECT_MAPPED_HASH(DirectMappedConcurrentHashMapTableCache.class), 
	DIRECT_MAPPED_ARRAY(DirectMappedTableCache.class),
	DIRECT_MAPPED(DirectMappedConcurrentHashMapTableCache.class); 
	
	// The class associated with this cache type. 
	private Class<? extends TableCache> tableCacheClass; 
	
	/**
	 * 
	 * 
	 * @param tableCacheClass
	 */
	private TableCacheType(Class<? extends TableCache> tableCacheClass)
	{
		this.tableCacheClass = tableCacheClass; 
	}

	/**
	 * Gets the cache class that this type resolves to. 
	 * 
	 * @return
	 */
	public Class<? extends TableCache> getTableCacheClass()
	{
		return tableCacheClass; 
	}
	
	/**
	 * Gets the value of the OVERHEAD static in the cache class.  
	 * 
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public long getOverhead() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Field overheadField = getTableCacheClass().getField("OVERHEAD");
		
		overheadField.setAccessible(true);
		
		long overhead = overheadField.getLong(null);
		
		return overhead; 				
	}
	
	/**
	 * Gets the value of the UNIT_OVERHEAD static field in the cache class 
	 * 
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public long getUnitOverhead() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Field overheadField = getTableCacheClass().getField("UNIT_OVERHEAD");
		
		overheadField.setAccessible(true);
		
		long overhead = overheadField.getLong(null);
		
		return overhead; 				
	}
	
	/**
	 * Gets the constructor for the TableCache implementation associated with this 
	 * cache type
	 * 
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public Constructor<? extends TableCache>  getCacheConstructor() throws NoSuchMethodException, SecurityException 
	{
		return tableCacheClass.getConstructor(Integer.class, TableConfiguration.class, CacheSimulation.class);
	}
	
	/**
	 * Gets a new instance of the table TableCache implementation associated with the 
	 * given configuration 
	 * 
	 * @param tableConfiguration
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static TableCache getNewCache(int id, TableConfiguration tableConfiguration, CacheSimulation simulation) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException 
	{
		return tableConfiguration.getCacheType().getCacheConstructor().newInstance(id, tableConfiguration, simulation);
	}
	
	public String toString()
	{
		return name().toUpperCase(); 
	}
}
