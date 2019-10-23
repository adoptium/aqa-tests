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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.adoptopenjdk.casa.data_structures.KeyCounter;
import net.adoptopenjdk.casa.event.EventHandler;
import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Contains structured data from the verbose GC file. 
 * 
 *  
 */
public class VerboseUtil 
{
	public static final SimpleDateFormat VERBOSE_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");					
	private static EventHandler parseError = Event.PARSE_ERROR;
			
	/**
	 * Parses a data size string consisting of a number of bytes. 
	 * 
	 * @param string
	 * @return
	 */
	public static long parseDataSize(String string)
	{
		return (long)ParseUtilities.parseDataSizeToBytes(string + "B");
	}
	
	/**
	 * Formats the given time taken from the given startTime as a 
	 * Verbose GC timestamp. 
	 * 
	 * @param time - the time (offset) since startTime 
	 * @param startTime 
	 * @return
	 */
	public static String formatTimestamp(double time, double startTime) 
	{
		long milliseconds = Math.round((time + startTime) * 1000.0);  
			
		return VERBOSE_TIMESTAMP_FORMAT.format(new Date(milliseconds));
	}
	
	/**
	 * Parses a timestamp from the verbose file int a number of seconds. 
	 * 
	 * @param string
	 * @param startTime
	 * @return a number of seconds
	 */
	public static double parseTimestamp(String string, double startTime) 
	{
		try 
		{
			return ParseUtilities.parseTime((VERBOSE_TIMESTAMP_FORMAT.parse(string).getTime()) + "ms") - startTime;
		}
		catch (NumberFormatException | ParseException e) 
		{
			parseError.issue(e);
			return 0; 
		}
	}

	/**
	 * Increments counters for events within a gc-op tag. 
	 * 
	 * @param element
	 * @param counter
	 * @return
	 */
	public static boolean countOpEvents(Element element, KeyCounter<String> counter) 
	{
		boolean updated = false; 
		
		
		ArrayList<Element> children = element.getChildren("copy-failed");
		
		// Update output on copy failed events 
		if (children.size() > 0)
			updated = true; 
		
		// Count copy-failed events by type
		for (Element child : children) 				
			counter.incrementAndGet(getCopyFailedEventType(child.getAttribute("type")).getSymbol());							
		
		return updated; 
	}
	
	/**
	 * Gets the VerboseEvent associated with the given copy-failed type value.  
	 * 
	 * @param type
	 * @return
	 */
	protected static VerboseEvent getCopyFailedEventType(String type)
	{
		if (type.equals("nursery"))
			return VerboseEvent.FAILED_FLIP;
		else if (type.equals("tenure"))
			return VerboseEvent.FAILED_TENURE;
		else
			return VerboseEvent.UNKNOWN;
	}
	
	/**
	 * Adds the warning to the counter. 
	 * 
	 * @param element
	 * @param counter
	 */
	public static void countWarning(Element element, KeyCounter<String> counter)
	{
		VerboseEvent warningType = getWarningEventType(element.getAttribute("details")); 
		
		counter.incrementAndGet(warningType.getSymbol());
	}
	
	/**
	 * Gets the VerboseEvent that the warning matches based on the 
	 * details string. Matches the whole string.    
	 * 
	 * @param details
	 * @return
	 */
	public static VerboseEvent getWarningEventType(String details)
	{
		if (details.startsWith("aborted collection due to insufficient free space"))
			return VerboseEvent.ABORT;
		else if (details.startsWith("scan cache overflow (storage acquired from heap)"))
			return VerboseEvent.SCAN_CACHE_OVERFLOW;
		else 
			return VerboseEvent.OTHER_WARNING;		
	}

	/**
	 * Increments counters related to gc-start tags. 
	 * 
	 * @param element
	 * @param counter
	 */
	public static void countGCStart(Element element, KeyCounter<String> counter) 
	{
		// Count gc-start's by type 
		if (element.getAttribute("type").equals("scavenge"))					
			counter.incrementAndGet(VerboseEvent.SCAVENGE.getSymbol());
		else if (element.getAttribute("type").equals("global"))					
			counter.incrementAndGet(VerboseEvent.GLOBAL.getSymbol());
		else 
			counter.incrementAndGet(element.getName() + "-" + element.getAttribute("type"));		
	}
	
	/**
	 * A key counter with string keys, primed with the first few keys in a fixed 
	 * order. 
	 * 
	 *  
	 */
	public static class VerboseEventCounter extends KeyCounter<String>
	{		
		public VerboseEventCounter()
		{
			super(); 
	
			/* 
			 * Prime these items in fixed order so they'll appear 
			 * in the key list in the same order.   
			 */
			get(VerboseEvent.SCAVENGE.getSymbol());
			get(VerboseEvent.GLOBAL.getSymbol());
			get(VerboseEvent.ABORT.getSymbol());
			get(VerboseEvent.FAILED_FLIP.getSymbol());
			get(VerboseEvent.FAILED_TENURE.getSymbol());
			get(VerboseEvent.PERCOLATE_COLLECT.getSymbol());
		}		
	}
}
