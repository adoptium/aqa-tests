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
package net.adoptopenjdk.casa.verbose_gc_parser;

import java.util.ArrayList;

import net.adoptopenjdk.casa.data_structures.KeyCounter;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.HierarchyException;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.VoidElementHandler;
import net.adoptopenjdk.casa.xml_parser.XMLHierarchy;
import net.adoptopenjdk.casa.xml_parser.XMLParser;

/**
 * Parses a summary of the given verbose file. Provides various 
 * counters for things like globals, scavenges, percolates and aborts.  
 * 
 *  
 */
public class VerboseSummary
{	
	private KeyCounter<String> counter;  		
	private String commandLine;
	private long concurrentSlack; 
	private long totalThroughput; 
	
	/**
	 * 
	 * 
	 * @param filename
	 */
	public VerboseSummary(String filename)
	{
		this(filename, null); 
	}
	
	/**
	 * Parses the verbose file, taking its initial counter key set from the given summary. 
	 * 
	 * @param filename
	 * @param previous - The previous summary from which the initial key list is extracted. 
	 */
	public VerboseSummary(String filename, VerboseSummary previous)
	{	
		if (previous != null) counter = new KeyCounter<String> (previous.getKeys());
		else                  counter = new VerboseUtil.VerboseEventCounter(); 				

		XMLHierarchy hierarchy = new XMLHierarchy("verbosegc");
		hierarchy.makePermissive("verbosegc");
																						
		final XMLParser parser = new XMLParser(hierarchy, Event.PARSE_ERROR);
		
		// Grab information from the initialized element, including the command line arguments. 
		parser.setElementHandler("initialized", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{													
				VerboseSummary.this.commandLine = parseCommandLine(element);
				
				concurrentSlack = 0; 
				totalThroughput = 0; 
				
				Element vmargsElement = 
						element.
						getChildren("vmargs").
						get(0);
				
				// Iterate over arguments to find the concurrentSlack value. 
				for (Element vmargElement : vmargsElement.getChildren("vmarg")) 
				{					
					String name = vmargElement.getAttribute("name");
					
					// Parse out the concurrent slack parameter. 
					String prefix = "-Xgc:concurrentSlack="; 
					if (name.startsWith(prefix)) 
					{
						String concurrentSlackString = name.substring(prefix.length(), name.length());
						
						if (concurrentSlackString.equals("auto"))
						{
							concurrentSlack = -1;
						}
						else 
						{ 
							try 
							{
								concurrentSlack = Long.parseLong(concurrentSlackString);
							}
							catch (NumberFormatException e)
							{
								concurrentSlack = -2;
							}
						}
					}
				}
			}
		});
		
		// Count concurrent-aborted occurrences. 
		parser.setElementHandler("concurrent-aborted", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{															
				counter.incrementAndGet(VerboseEvent.CONCURRENT_ABORTED.getSymbol());			
			}
		});
		
		// Count up warnings by "details" field contents. 
		parser.setElementHandler("warning", new VoidElementHandler() 
		{
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{
				VerboseUtil.countWarning(element, counter);
			}
		});
		
		// Sum up all "totalBytes" fields in allocation-stats tags 
		parser.setElementHandler("allocation-stats", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{									
				totalThroughput += VerboseUtil.parseDataSize(element.getAttribute("totalBytes"));							
			}
		});
			
		// Count the gc-start...this gives us the type of GC. 
		parser.setElementHandler("gc-start", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{									
				VerboseUtil.countGCStart(element, counter);			
			}
		});
		
		// Count the percolate collect 
		parser.setElementHandler("percolate-collect", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{		
				counter.incrementAndGet(VerboseEvent.PERCOLATE_COLLECT.getSymbol());		
			}
		});

		// Count elements in the gc-op 
		parser.setElementHandler("gc-op", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{																	
				VerboseUtil.countOpEvents(element, counter);	
			}
		});
		
		parser.parse(filename);																			
	}
	
	/**
	 * Gets the value of the given counter. 
	 * 
	 * @param key
	 * @return
	 */
	public long getCounterValue(String key)
	{
		return counter.get(key);
	}
	
	/**
	 * Parses the command line arguments in the given initialized 
	 * element to a space-separated string. 
	 * 
	 * @param element
	 * @return
	 */
	private String parseCommandLine(Element element)
	{
		StringBuilder builder = new StringBuilder(); 
		Element vmargsElement = 
				element.
				getChildren("vmargs").
				get(0);
		
		for (Element vmargElement : vmargsElement.getChildren("vmarg"))
			builder.append(" " + vmargElement.getAttribute("name"));
				
		return builder.toString(); 
	}
	
	/**
	 * Gets the command line parsed from the verbose GC file. 
	 * 
	 * @return
	 */
	public String getCommandLine()
	{
		return commandLine; 
	}
		
	/**
	 * Gets the total throughput as a formatted string. 
	 * 
	 * @return
	 */
	public String getThroughputString()
	{
		return Utilities.formatDataSize(totalThroughput);
	}
	
	/**
	 * Gets the value of the concurrent slack option parsed from the command line as a string. 
	 * 
	 * @return
	 */
	public String getConcurrentSlackString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append(concurrentSlack + ""); 
		
		return builder.toString();		
	}
	
	/**
	 * The the keys, meaning the names of the values counted during parsing. 
	 * 
	 * @return
	 */
	public ArrayList<String> getKeys()
	{
		return counter.keySet(); 			
	}
	
	/**
	 * Produces a tabular representation of the summary including header. 
	 */
	public String toString()
	{
		return getHeader() + "\n" + toString(getKeys());
	}
	
	/**
	 * Produces a string consisting of the values of the keys given, tab separated. 
	 * 
	 * @param keys - the key set used for headers 
	 * @return
	 */
	public String toString(ArrayList<String> keys)
	{	
		StringBuilder builder = new StringBuilder(); 
			
		String separator = ""; 
		
		for (String key : keys)
		{			
			builder.append(separator + counter.get(key));			
			separator = "\t";
		}
		
		builder.append (separator + getConcurrentSlackString());  
		builder.append (separator + getThroughputString()); 
		
		return builder.toString(); 
	}
	
	/** 
	 * Produces a table header for the summary's endogenous key set. 
	 * 
	 * @return
	 */
	public String getHeader()
	{		
		StringBuilder builder = new StringBuilder(); 
		
		for (String key : getKeys())
			builder.append(key + "\t");
		
		builder.append(VerboseEvent.SLACK + "\t");
		
		builder.append(VerboseEvent.THROUGHPUT + "\t");
								
		return builder.toString();
	}
	
}
