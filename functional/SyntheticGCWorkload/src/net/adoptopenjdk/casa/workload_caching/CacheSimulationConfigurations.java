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

import net.adoptopenjdk.casa.xml_parser.DefaultElementHandler;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.HierarchyException;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.XMLHierarchy;
import net.adoptopenjdk.casa.xml_parser.XMLParser;

/**
 * Encapsulates a list of cache simulation configurations in a given 
 * configuration file. 
 * 
 *  
 */
public class CacheSimulationConfigurations
{
	private ArrayList<CacheSimulationConfiguration> configurations; 
	
	/**
	 * Parses configurations from the given file. Call 
	 * getConfigurations() to get them. 
	 * 
	 * @param filename
	 * @throws ParseError 
	 */
	protected CacheSimulationConfigurations (String filename) throws ParseError
	{
		configurations = new ArrayList<CacheSimulationConfiguration> (); 
			
		XMLHierarchy hierarchy = new XMLHierarchy("configuration"); 		
		try 
		{
			hierarchy.addRelation("configuration", CacheSimulationConfiguration.TAG_NAME, true);
			hierarchy.addRelation(CacheSimulationConfiguration.TAG_NAME, TableConfiguration.TAG_NAME, true);
		} 
		catch (HierarchyException e) 
		{
			Event.PARSE_ERROR.issue(e);
		}		
																									
		final XMLParser parser = new XMLParser(hierarchy, Event.PARSE_ERROR);
			
		parser.setElementHandler(CacheSimulationConfiguration.TAG_NAME, new DefaultElementHandler() 
		{				
			public void elementStart(Element element) throws HierarchyException, ParseError 
			{			
				super.elementStart(element);				
			}
			
			public void elementEnd(Element element) throws HierarchyException, ParseError
			{				
				super.elementEnd(element);
								
				CacheSimulationConfiguration simulationConfiguration = new CacheSimulationConfiguration(element);
				
				ArrayList<Element> children = element.getChildren("table");
				
				for (Element child : children)
					simulationConfiguration.addTableConfiguration(new TableConfiguration(child, simulationConfiguration));
				
				configurations.add(simulationConfiguration);
			}			
		});
			
		parser.setElementHandler(TableConfiguration.TAG_NAME, new DefaultElementHandler() {});
		
		parser.parse(filename);
		
		if (parser.getErrorStatus())
			throw new ParseError("Failed to parse configuration file " + filename);
	}
	
	/**
	 * Gets and array list of all configurations parsed from the given file. 
	 * 
	 * @return
	 */
	protected ArrayList<CacheSimulationConfiguration> getConfigurations()
	{
		return configurations; 
	}
	
}
