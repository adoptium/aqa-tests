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

/**
 * Symbols used for abbreviated summaries of verbose output. 
 * 
 *  
 *
 */
public enum VerboseEvent 
{
	SCAVENGE            ("Scavenge", "."), 
	GLOBAL              ("Global", ","),
	FAILED_FLIP         ("Failed flip", "f"),
	FAILED_TENURE       ("Failed tenure", "F"), 
	PERCOLATE_COLLECT   ("Percolate collect", "p"),
	CONCURRENT_ABORTED  ("Concurrent aborted", "c"),
	MINUTE              ("Minute marker", "|"), 
	ABORT               ("Warning: aborted collection due to insufficient free space", "x"),
	SCAN_CACHE_OVERFLOW ("Warning: scan cache overflow (storage acquired from heap)", "s"),
	OTHER_WARNING       ("Other warning", "X"),
	THROUGHPUT          ("Total throughput", "T"),
	SLACK               ("Concurrent slack (-1 = auto)", "S"),
	UNKNOWN             ("Unknown event", "?"); 
		
	private String description; 
	private String symbol; 

	/**
	 * Creates a new verbose event with a human readable description 
	 * and a compact symbol 
	 * 
	 * @param description
	 * @param symbol
	 */
	private VerboseEvent (String description, String symbol)
	{
		this.description = description; 
		this.symbol = symbol; 
	}
	
	/**
	 * Gets a compact symbolic representation of the event. 
	 * 
	 * @return
	 */
	public String getSymbol()
	{
		return symbol; 
	}
	
	/**
	 * Gets a human readable description of the event 
	 * 
	 * @return
	 */
	public String getDescription()
	{
		return description; 
	}
	
	/**
	 * Gets the symbol 
	 * 
	 * @return 
	 */
	public String toString()
	{
		return symbol; 
	}
	
	/**
	 * Gets a human readable description of each symbol. 
	 * 
	 * @return
	 */
	public static String getLegendString()
	{
		StringBuilder builder = new StringBuilder(); 
		
		builder.append("Legend: ");

		for (VerboseEvent item : VerboseEvent.values())
			builder.append("\n" + item.getSymbol() + " = " + item.getDescription());

		return builder.toString(); 
	}
	
	/**
	 * 
	 * @return
	 */
	public static VerboseEvent getBySymbol(String symbol)
	{
		for (VerboseEvent item : VerboseEvent.values())
		{
			if (item.getSymbol().equals(symbol))
				return item; 
		}
			
		return UNKNOWN; 
	}
}