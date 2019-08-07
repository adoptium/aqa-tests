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

import java.util.ArrayList;

import net.adoptopenjdk.casa.util.AbstractConfiguration;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.util.format.Alignment;
import net.adoptopenjdk.casa.util.format.CellException;
import net.adoptopenjdk.casa.util.format.RowException;
import net.adoptopenjdk.casa.util.format.Table;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;

/**
 * 
 * 
 *  
 *
 */
class CacheSimulationConfiguration extends AbstractConfiguration
{
	protected static final String TAG_NAME = "cacheSimulation";
	
	private ArrayList<TableConfiguration> tableConfigurations;
	
	// The start time, given in seconds from application startup. 
	private double startTime;
	
	// The length of time, in seconds, that the simulation should be allowed to run for after its start time. 
	private double duration;
	
	// The number of threads which will be performing operations on the cache. 
	private int numThreads; 
	
	/**
	 * 
	 * 
	 * @param element
	 * @throws ParseError
	 */
	protected CacheSimulationConfiguration(Element element) throws ParseError
	{
		super(element); 
		
		tableConfigurations = new ArrayList<TableConfiguration>();
		
		getElement().setDefaultForAttribute("startTime", "0", null);
		getElement().assertAttributeIsSet("duration");
		getElement().assertAttributeIsSet("numThreads");
		
		duration = parseTimeField("duration");
		startTime = parseTimeField("startTime");
		numThreads = parseNumThreads("numThreads");
		
		getElement().checkForUnusedAttributes(); 
	}
	
	/**
	 * Gets a string representing the configuration's position in the config file. In the case 
	 * of cacheSimulation items this is simply the sequential ID of the simulation. 
	 * 
	 * @return
	 */
	protected String getIDString()
	{
		// Cache simulations are the highest level we bother to assign an ID to. 
		return getElement().getID() + ""; 
	}
	
	/**
	 * Gets the ID of the parsed element. 
	 * 
	 * @return
	 */
	protected int getID()
	{
		return getElement().getID();
	}
	
	/**
	 * Gets the number of worker threads, parsed from the config file 
	 * 
	 * @return
	 */
	protected int getNumThreads()
	{
		return numThreads; 
	}
	
	/**
	 * Gets the duration of the simulation in seconds, parsed from config file. 
	 * 
	 * @return
	 */
	protected double getDuration()
	{
		return duration; 
	}
	
	/**
	 * Adds the given table configuration 
	 * 
	 * @param tableConfiguration
	 */
	protected void addTableConfiguration(TableConfiguration tableConfiguration)
	{
		tableConfigurations.add(tableConfiguration); 			
	}
	
	/**
	 * Gets the number of tables present in this configuration. 
	 * 
	 * @return
	 */
	protected int getNumTables()
	{
		return tableConfigurations.size(); 
	}
	
	/**
	 * Gets an ArrayList of all table configurations 
	 * 
	 * @return
	 */
	protected ArrayList<TableConfiguration> getTableConfigurations() 
	{
		return tableConfigurations; 
	}
	
	/**
	 * Gets the relative end time of the simulation. Equal to the 
	 * sum of its start time and duration  
	 * 
	 * @return
	 */
	protected double getEndTime()
	{
		return startTime + getDuration(); 
	}
	
	/**
	 * Gets the relative start time of the simulation, taken from the 
	 * start time of the application, specified by the startTime parameter
	 * on the simulationConfiguration    
	 * 
	 * @return
	 */
	protected double getStartTime()
	{
		return startTime; 
	}
	
	/**
	 * Parses the number of threads specified in the configuration file. This may be 
	 * a number or the word "auto"
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	private int parseNumThreads(String key) throws ParseError
	{
		String value = getElement().consumeAttribute(key);
		
		// Choose the number of threads based on the size of the machine. 
		if (value.toUpperCase().equals("AUTO"))
		{			
			int availableProcessors = Runtime.getRuntime().availableProcessors(); 
						
			return availableProcessors;			 
		}
		else 
		{			
			try 
			{
				int numThreads = Integer.parseInt(value);
				
				// Reject negative numbers 
				if (numThreads <= 0)
					throw new NumberFormatException(); 
				
				return numThreads; 
			} 
			catch (NumberFormatException e) 
			{
				throw new ParseError(key + "=\""+ value +"\" is invalid. Must be a positive integer or auto. ", getElement()); 
			}
		}
	}
	
	/**
	 * Appends a string describing this configuration to the given builder. 
	 * 
	 * @param builder
	 */
	public void toString(StringBuilder builder)
	{		
		try 
		{
			// SIMULATION CONFIGURATION 
			
			Alignment[] alignment = new Alignment[] {Alignment.LEFT, Alignment.RIGHT};				
			Table table = new Table(); 							
			table.addHeadings(table.new Headings(
				new String[] {"CACHE SIMULATION", getIDString() + ""}, 
				new int[] {24, 20}, 
				alignment, getID() == 1 ? true : false, false
			));
			table.addRow(table.new Row(new String[] {"Start time", Utilities.formatTime(getStartTime())}, alignment));
			table.addRow(table.new Row(new String[] {"End time", Utilities.formatTime(getEndTime())}, alignment));
			table.addRow(table.new Row(new String[] {"Duration", Utilities.formatTime(getDuration())}, alignment));			
			table.addRow(table.new Row(new String[] {"Worker Threads", getNumThreads() + ""}, alignment));
			table.appendToStringBuilder(builder);			
			
			builder.append("\n");
			
			// TABLE CONFIGURATIONS 
			
			table = new Table(); 
			table.addHeadings(
				table.new Headings(
					new String[] {"Table", "Rows", "Row Size", "Hit %", "Cache Rows (Size)", "Response", "Update %", "Hit", "Miss", "Cache Type"}, 
					new int[] {6, 8, 8, 6, 17, 8, 8, 6, 6, 20}, 
					Alignment.LEFT
				)
			);
			
			// Print table configurations 
			for (TableConfiguration tableConfiguration : tableConfigurations) 
			{
				table.addRow(
					table.new Row
					(
						new String[]
						{
							tableConfiguration.getIDString(),
							tableConfiguration.getNumRows() + "", 
							Utilities.formatDataSize(tableConfiguration.getRowSize()),
							Utilities.formatProportionAsPercent(tableConfiguration.getHitRate(), 3), 
							tableConfiguration.getNumCacheRows() + " (" + Utilities.formatDataSize(tableConfiguration.getCacheDataSize()) + ")",
							Utilities.formatDataSize(tableConfiguration.getResponseSize()),
							Utilities.formatProportionAsPercent(tableConfiguration.getUpdateProbability(), 3),
							Utilities.formatTime(tableConfiguration.getHitDelay()),
							Utilities.formatTime(tableConfiguration.getMissDelay()),
							tableConfiguration.getCacheType().toString()
						}, 
						Alignment.RIGHT
					)
				);					
			}
			
			table.addLine();			
			table.appendToStringBuilder(builder);						
		} 
		catch (RowException | CellException e) 
		{
			Event.FATAL_ERROR.issue(e);
		}		
	}
	
	/**
	 * Produces a string detailing the configuration by calling toString(StringBuilder) 
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder(); 
		
		toString(builder);
		
		return builder.toString(); 
	}
}