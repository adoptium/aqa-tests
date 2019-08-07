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

import net.adoptopenjdk.casa.util.AbstractConfiguration;
import net.adoptopenjdk.casa.util.Sizes;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * 
 * 
 *  
 *
 */
class TableConfiguration extends AbstractConfiguration
{
	// Reasonable maximum for the number of rows in the imaginary table. We don't actually have to store these.  	
	public static final long MAX_ROWS = Long.MAX_VALUE/10;
	
	public static final long MIN_ROWS = 1;

	// The maximum number of items that a cache can hold.  
	public static final long MAX_CACHE_SIZE = Integer.MAX_VALUE/10;
	
	// The maximum size of a row, including overhead, as specified in the configuration file. 
	public static final long MAX_ROW_SIZE = Integer.MAX_VALUE/10;  
	
	// XML tag that defines a table configuration 
	public static final String TAG_NAME = "table"; 
	
	// The size of each row
	private int rowSize; 
	
	// The number of rows in the table
	private long numRows; 

	private double hitRate; 
	
	// The number of rows in the cache
	private int numCacheRows; 
	
	// The size of the QueryResponse structure in bytes
	private int responseSize; 
	
	// The type of cache
	private TableCacheType cacheType; 
	
	// The penalty paid for accessing an item already in cache, in seconds. 
	private double hitDelay; 
	
	// The penalty paid for accessing an item not already in the cache, in seconds  
	private double missDelay; 
	
	// The probability that a random query is an update vs. a query . 
	private double updateProbability;
	
	// The simulation that this table belongs to. 
	private CacheSimulationConfiguration parentConfiguration; 
	
	/**
	 * Instantiates a new table configuration from the given element, parsed 
	 * from the configuration file within the given cache simulation configuration. 
	 * 
	 * @param element
	 * @param parentConfiguration
	 * @throws ParseError
	 */
	public TableConfiguration(Element element, CacheSimulationConfiguration parentConfiguration) throws ParseError
	{
		super(element);
				
		this.parentConfiguration = parentConfiguration; 
		
		getElement().assertAttributeIsSet("rowSize");
		getElement().assertAttributeIsSet("hitRate");
		getElement().setDefaultForAttribute("responseSize", "128B", null);
		getElement().assertAttributeIsSet("cacheSize");
		getElement().setDefaultForAttribute("cacheType", TableCacheType.DIRECT_MAPPED_HASH.name(), null);
				
		getElement().setAttributeDeprecated("rows", Event.WARNING);
		
		getElement().setDefaultForAttribute("updates", "0%", null);		
		getElement().setDefaultForAttribute("responseSize", "128B", null);
		
		// No hit or miss delay by default. 
		getElement().setDefaultForAttribute("hitDelay", "0", null);
		getElement().setDefaultForAttribute("missDelay", "0", null);
		
		hitDelay = parseTimeField("hitDelay");
		missDelay = parseTimeField("missDelay"); 		
		
		updateProbability = super.parseProportionField("updates");
		
		responseSize =  parseResponseSize("responseSize");
		
		if (responseSize >= Integer.MAX_VALUE)
			throw new ParseError("responseSize is too large.", getElement());
		
		rowSize = parseRowSizeField("rowSize");	
		parseCacheSizeField("cacheSize");
		cacheType = parseCacheType("cacheType");
		
		hitRate = parseProportionField("hitRate");
		
		// Hit rates higher than unity make no sense. 
		if (hitRate > 1)
			throw new ParseError("Hit rates over 100% are not permitted. Please select a hit rate between 0 (exclusive) and 100% (inclusive).", getElement());
		
		// Hit rates < 0 make no sense. 
		if (hitRate <= 0)
			throw new ParseError("A hit rate of 0 is not permitted. Please select a hit rate between 0 (exclusive) and 100% (inclusive).", getElement());
						
		numRows = (long)Math.floor(numCacheRows / hitRate); 
		
		if (numRows > MAX_ROWS)
			throw new ParseError("the number of rows in the imaginary table exceeds the maximum table size of " + Utilities.formatDataSize(MAX_ROWS) + ". Please select a higher hit rate.", getElement());
		
		if (numRows <= MIN_ROWS)
			throw new ParseError("the number of rows in the imaginary table is too small. Please select a lower hit rate or use a larger cache. ", getElement());
		
		getElement().checkForUnusedAttributes(); 
	}
	
	private int parseResponseSize(String key) throws ParseError 
	{			
		double size = parseDataSizeField(key);
		
		double misalignment = size % Sizes.ALIGNMENT;
		
		if (misalignment > 0)
			size += Sizes.ALIGNMENT - (size % Sizes.ALIGNMENT); 
					
		if (size < Sizes.ALIGNMENT)
			throw new ParseError("response size must be at least " + Utilities.formatDataSize(Sizes.REFERENCE_SIZE), getElement());			
				
		if (size > Integer.MAX_VALUE)
			throw new ParseError("response size must be less than " + Utilities.formatDataSize(Integer.MAX_VALUE), getElement());
		
		return (int)size; 
	}
	
	protected CacheSimulationConfiguration getSimulationConfiguration()
	{
		return parentConfiguration; 
	}
	
	/**
	 * Parses the cacheType attribute to an element in TableCacheType 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private TableCacheType parseCacheType(String key) throws ParseError
	{
		String typeName = getElement().consumeAttribute(key).toUpperCase();
		
		TableCacheType type = TableCacheType.valueOf(typeName);
		
		if (type == null)
			throw new ParseError("Invalid cache type " + typeName, getElement());
		
		return type; 		
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	protected int getResponseElements()
	{
		return (int)Math.ceil(responseSize / (double)QueryResponse.ELEMENT_SIZE);
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	protected int getResponseSize()
	{
		return (int)responseSize;	
	}
	
	/**
	 * Gets a string representing the ID of this configuration within the 
	 * hierarchy of the configuration file. 
	 * 
	 * @return
	 */
	protected String getIDString()
	{
		return parentConfiguration.getIDString() + "." + getElement().getID() ;
	}
	
	/**
	 * 
	 * @return
	 */
	protected TableCacheType getCacheType()
	{
		return cacheType; 
	}
	
	protected int getID()
	{
		return getElement().getID(); 
	}
	
	/**
	 * Parses the filed 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private int parseRowSizeField(String key) throws ParseError
	{
		long rowSize = Math.round(parseDataSizeField("rowSize"));
		
		if (rowSize > MAX_ROW_SIZE)
			throw new ParseError(key + " exceeds the maximum row size of " + Utilities.formatDataSize(Integer.MAX_VALUE), getElement());
				
		return (int)rowSize; 		
	}
	
	/**
	 * Parses the cache size field from the table tag. Accepts a valid data size or 
	 * a proportion of the free heap at the time of parsing. 
	 * 
	 * @param key
	 * @throws ParseError
	 */
	private void parseCacheSizeField(String key) throws ParseError
	{			
		try 
		{				
			long finalCacheSize;
			try			
			{
				// GC to improve free memory estimate. 
				System.gc();
				
				// Get the free heap space in bytes
				long freeHeap = Runtime.getRuntime().freeMemory(); 
				
				double proportionalCacheSize = ParseUtilities.parseProportion(getElement().getAttribute(key)); 
								
				long cacheSizeBytes = Math.round((double)freeHeap * (proportionalCacheSize)) - getOverhead();
											
				finalCacheSize = (cacheSizeBytes/(rowSize + Row.OVERHEAD));
				
				if (finalCacheSize >= Integer.MAX_VALUE)
					throw new ParseError(key + " is too large.", getElement());							
			}
			catch(NumberFormatException e) 
			{								
				finalCacheSize = Math.round((ParseUtilities.parseDataSizeToBytes(getElement().getAttribute(key)))/(rowSize + Row.OVERHEAD)); 				
			}					
								
			if (finalCacheSize < 1)
				throw new ParseError(key + " " + numCacheRows +  " is too small. Must be at least large enough to hold one row from the table. ", getElement());
						
			if (finalCacheSize > MAX_CACHE_SIZE)
				throw new ParseError(key + " " + numCacheRows +  " exceeds the maximum cache size of " + Utilities.formatDataSize(MAX_CACHE_SIZE), getElement());
			 
			this.numCacheRows = (int)finalCacheSize; 
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid. Must be a percent or proportion of the system heap size or a valid data size (GB, MB, kB or B). ", getElement()); 
		}
		
		getElement().consumeAttribute(key);
		
	}
	
	protected double getHitRate()
	{
		return hitRate; 
	}
	
	/**
	 * Gets the configured update probability. Performed transactions 
	 * are split between queries and updates in this proportion. 
	 * 
	 * @return
	 */
	protected double getUpdateProbability()
	{
		return updateProbability; 
	}
	
	/**
	 * Gets the configured hit delay, incurred when there is a 
	 * cache hit. 
	 *
	 * @return
	 */
	protected double getHitDelay()
	{
		return hitDelay; 
	}
	
	/**
	 * Gets the configured miss delay, to be incurred when there is a 
	 * cache miss. 
	 * 
	 * @return
	 */	
	protected double getMissDelay()
	{
		return missDelay; 
	}
	
	/**
	 * Gets the size of the cache when full 
	 * 
	 * @return
	 */
	protected long getCacheDataSize()
	{		
		try 
		{
			return (numCacheRows * (rowSize + cacheType.getUnitOverhead())) + TableCache.OVERHEAD;
		} 
		catch (NoSuchFieldException | SecurityException| IllegalArgumentException | IllegalAccessException e) 
		{		
			Event.FATAL_ERROR.issue(e, "problem encountered while accessing UNIT_OVERHEAD static in cache class " + cacheType.name());
			return 0; 
		}
	}
	
	/** 
	 * Gets the number of rows allowed in the cache. 
	 * 
	 * @return
	 */
	protected long getNumRows()
	{
		return numRows; 
	}
	
	/**
	 * Gets an estimate of the overhead of this table. 
	 * 
	 * @return
	 */
	protected long getOverhead()
	{
		try 
		{
			return cacheType.getOverhead() + (getNumCacheRows() * Row.OVERHEAD);
		} 
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) 
		{
			Event.FATAL_ERROR.issue(e, "problem encountered while accessing OVERHEAD static in cache class " + cacheType.name());
			return 0; 
		}
	}
	
	/**
	 * Gets the size of a row as specified by the configuration 
	 * file. Actual row size will target this by subtracting estimated 
	 * overhead from the size of the payload array.  
	 * 
	 * @return
	 */
	protected int getRowSize()
	{
		return rowSize; 
	}
	
	/**
	 * Gets the number of elements in a row's data array, equal to the size of the 
	 * row over the element size, rounded up. 
	 * 
	 * @return
	 */
	protected int getRowNumElements()
	{
		return (int)Math.ceil(rowSize / (double)Row.ELEMENT_SIZE); 
	}
	
	/**
	 * Gets the number of rows which can be in the cache at a time. 
	 * 
	 * @return
	 */
	protected int getNumCacheRows()
	{		
		return numCacheRows; 
	}
}