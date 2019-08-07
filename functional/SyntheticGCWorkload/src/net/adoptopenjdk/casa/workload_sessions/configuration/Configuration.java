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
package net.adoptopenjdk.casa.workload_sessions.configuration;

import net.adoptopenjdk.casa.event.EventHandler;
import net.adoptopenjdk.casa.util.AbstractConfiguration;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.HierarchyException;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.VoidElementHandler;
import net.adoptopenjdk.casa.xml_parser.XMLHierarchy;
import net.adoptopenjdk.casa.xml_parser.XMLParser;

/**
 * Encapsulates workload configurations and attributes contained in 
 * a "configuration" tag; these are global settings which apply to 
 * all workloads in the configuration. This class's Element 
 * represents the head of the AST.  
 * 
 *  
 *
 */
public class Configuration extends AbstractConfiguration 
{		
	public final static String TAG_NAME = "configuration";	
	private static Configuration configuration;	
	
	/**
	 * Factory method which parses the configuration from the given file. 
	 * 
	 * @param filename
	 * @param parseError
	 * @param notice
	 * @param warning
	 * @return
	 */
	public static Configuration parseFrom(String filename, EventHandler parseError, final EventHandler notice, final EventHandler warning)
	{		
		configuration = null; 
		
		try 
		{
			XMLHierarchy hierarchy = new XMLHierarchy(Configuration.TAG_NAME); 		
			hierarchy.addRelation(Configuration.TAG_NAME, WorkloadConfiguration.TAG_NAME, true);
			hierarchy.addRelation(WorkloadConfiguration.TAG_NAME, PayloadSetConfiguration.TAG_NAME, true);
			hierarchy.addRelation(PayloadSetConfiguration.TAG_NAME, PayloadSetConfiguration.TAG_NAME, false);
			hierarchy.addRelation(PayloadSetConfiguration.TAG_NAME, PayloadConfiguration.TAG_NAME, false);
			
			XMLParser parser = new XMLParser(hierarchy, parseError); 						
			parser.setElementHandler(Configuration.TAG_NAME, new VoidElementHandler() 
				{
					public void elementEnd(Element element) throws HierarchyException, ParseError 
					{
						configuration = new Configuration(element, notice, warning);
					}
				}
			); 
						 		
			parser.parse(filename);
			
			if (configuration == null)
				parseError.issue("no configuration parsed");													
		} 
		catch (HierarchyException e) 
		{
			parseError.issue(e);
		}
		
		return configuration; 	
	}
	
	// Attributes 
	private final double statusUpdatePeriod; 
	private final double maxDuration; 	
	
	// Event handlers 
	private final EventHandler notice;
	private final EventHandler warning;
	
	/**
	 * Constructs a new configuration from the given element. 
	 * 
	 * @param element - the AST
	 * @throws ParseError
	 */
	public Configuration (Element element, EventHandler notice, EventHandler warning) throws ParseError
	{
		super(element);
		
		this.notice = notice; 
		this.warning = warning; 
		
		// Warn if the configuration contains no workloads. 
		warning.issue(getElement().getNumChildren(WorkloadConfiguration.TAG_NAME) == 0, "no " + WorkloadConfiguration.TAG_NAME + " are present");
		
		// Set deprecated attributes. 
		getElement().setAttributeDeprecated("numRepetitions", warning); // Repetition is no longer supported.
		
		// Set defaults 
		getElement().setDefaultForAttribute("maxDuration", "5m", notice);
		getElement().setDefaultForAttribute("statusUpdatePeriod", "1s", null);  // Update every second.
		
		// Parse attributes
		this.maxDuration = parseTimeField("maxDuration");
		this.statusUpdatePeriod = parseTimeField("statusUpdatePeriod");
				
		if (this.maxDuration <= 0)
			throw new ParseError("Configuration duration must be greater than 0.", element);
		
		// Ensure we've consumed all symbols. 
		getElement().checkForUnusedAttributes();	
	}
	
	/**
	 * Parses and returns all workload configurations. 
	 * 
	 * @return
	 * @throws ParseError
	 */
	public WorkloadConfiguration[] getWorkloadConfigurations() throws ParseError
	{
		WorkloadConfiguration[] configurations = new WorkloadConfiguration[getNumWorkloads()];
		
		Element[] elements = getElement().getChildren(WorkloadConfiguration.TAG_NAME).toArray(new Element[0]);
		
		for (int i = 0; i < getNumWorkloads(); i++)
		{
			Element element = elements[i];
			
			if (!element.getName().equals(WorkloadConfiguration.TAG_NAME))
				throw new ParseError("Invalid " + WorkloadConfiguration.TAG_NAME + " element.", element);
					
			configurations[i] = new WorkloadConfiguration(element, this, notice, warning);
		}
		
		return configurations; 
	}
		
	/**
	 * Returns the period, in seconds, for which the 
	 * status update thread should remain idle between updates. 
	 * 
	 * @return
	 */
	public double getStatusUpdatePeriod() 
	{ 
		return statusUpdatePeriod; 
	}
	
	/**
	 * Gets the number of workloads under this configuration. 
	 * 
	 * @return
	 */
	public int getNumWorkloads()
	{
		return getElement().getNumChildren(WorkloadConfiguration.TAG_NAME); 
	}
	
	/**
	 * Gets the maximum time, in seconds, that this benchmark is allowed to run 
	 * for as specified by the user.   
	 * 
	 * @return
	 */
	public double getMaxDuration()
	{
		return maxDuration; 
	}	
}
