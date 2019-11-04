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

import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.HierarchyException;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.VoidElementHandler;
import net.adoptopenjdk.casa.xml_parser.XMLHierarchy;
import net.adoptopenjdk.casa.xml_parser.XMLParser;

/**
 * Produces a string representation of the events in the verbose file. 
 * 
 *  
 */
public class VerboseTimeline 
{
	private double startTime; 
	private double lastTime;
	private long minute;
	
	private StringBuilder timeline; 
	
	/**
	 * 
	 * 
	 * @param filename
	 */
	public VerboseTimeline(String filename)
	{		
		timeline = new StringBuilder();
		minute = 0; 
		
		XMLHierarchy hierarchy = new XMLHierarchy("verbosegc");
		hierarchy.makePermissive("verbosegc");
																						
		final XMLParser parser = new XMLParser(hierarchy, Event.PARSE_ERROR);
		parser.setElementHandler("initialized", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{
				startTime = VerboseUtil.parseTimestamp(element.getAttribute("timestamp"), 0);				
			}
		});
		
		parser.setElementHandler("gc-start", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{		
				processTimestamp(element.getAttribute("timestamp"));
				
				if (element.getAttribute("type").equals("global"))
					timeline.append(VerboseEvent.GLOBAL);	
				else if (element.getAttribute("type").equals("scavenge"))
					timeline.append(VerboseEvent.SCAVENGE);
				else 
					timeline.append(VerboseEvent.UNKNOWN);
			}
		});
		
		parser.setElementHandler("concurrent-aborted", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{															
				timeline.append(VerboseEvent.CONCURRENT_ABORTED);				
			}
		});
		
		parser.setElementHandler("percolate-collect", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{		
				processTimestamp(element.getAttribute("timestamp"));
									
				timeline.append(VerboseEvent.PERCOLATE_COLLECT);				
			}
		});

		
		parser.setElementHandler("warning", new VoidElementHandler() 
		{
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{
				timeline.append(VerboseUtil.getWarningEventType(element.getAttribute("details")));
			}
		});
		
		parser.setElementHandler("copy-failed", new VoidElementHandler() 
		{
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{
				timeline.append(VerboseUtil.getCopyFailedEventType(element.getAttribute("type")));				
			}
		});
		
		parser.setElementHandler("gc-op", new VoidElementHandler() 
		{				
			public void elementEnd(Element element) throws HierarchyException, ParseError 
			{						
				processTimestamp(element.getAttribute("timestamp"));								 						
			}
		});
			
		 		
		parser.parse(filename);																		
	}
	
	/**
	 * Processes the timestamp, updating lastTime and appending a 
	 * minute marker each time the minute changes. 
	 * 
	 * @param timestamp
	 */
	private void processTimestamp(String timestamp)
	{			
		lastTime = VerboseUtil.parseTimestamp(timestamp, startTime);
							
		long minutes = Math.round(Math.floor(lastTime / 60));
			
		if (minutes > minute) {
			minute = minutes;
			timeline.append("|");
		}
	
	}
	
	/**
	 * Gets a string representation of the events in the verbose file. 
	 * 
	 * @return 
	 */
	public String toString()
	{
		return timeline.toString(); 
	}
}
