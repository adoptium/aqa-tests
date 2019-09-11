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
package net.adoptopenjdk.casa.xml_parser;

/**
 * Helper functions for parsing formatted data
 * 
 *  
 *
 */
public class ParseUtilities 
{
	/**
	 * Parse a proportion from the (percent or decimal) string 
	 * 
	 * @param string
	 * @return
	 * @throws NumberFormatException
	 */
	public static double parseProportion(String string) throws NumberFormatException
	{
		if (string.charAt(string.length() - 1) == '%') {
			return Double.parseDouble(string.substring(0, string.length() - 1)) / 100;
		}
		else {
			double proportion = Double.parseDouble(string); 
			
			if (proportion < 0)
				throw new NumberFormatException("proportion must be positive.");
			
			return proportion; 
		}
	}
	
	/**
	 * Parse a time in seconds from the given formatted time. 
	 * 
	 * @param time
	 * @return
	 * @throws NumberFormatException
	 */
	public static double parseTime(String time) throws NumberFormatException 
	{
		double seconds;
		if (time.endsWith("0")) {
			seconds = Double.parseDouble(time);
			if (seconds != 0)
				throw new NumberFormatException("must be 0 or suffixed with a valid time unit");
		}
		else if (time.endsWith("ns")) {
			seconds = Double.parseDouble(time.substring(0, time.length() - 2)) / 1E9;
		}
		else if (time.endsWith("us")) {
			seconds = Double.parseDouble(time.substring(0, time.length() - 2)) / 1E6;
		}		
		else if (time.endsWith("ms")) {
			seconds = Double.parseDouble(time.substring(0, time.length() - 2)) / 1E3;
		}
		else if (time.endsWith("s")) {					
			seconds = Double.parseDouble(time.substring(0, time.length() - 1));
		}
		else if (time.charAt(time.length() - 1) == 'm') {
			seconds = Double.parseDouble(time.substring(0, time.length() - 1)) * 60;			
		}
		else if (time.charAt(time.length() - 1) == 'h') {
			seconds = Double.parseDouble(time.substring(0, time.length() - 1)) * 60 * 60;			
		}
		else if (time.charAt(time.length() - 1) == 'd') {
			seconds = Double.parseDouble(time.substring(0, time.length() - 1)) * 60 * 60 * 24;			
		}
		else {					
			throw new NumberFormatException("non-zero number must be suffixed by ns, us, ms, s, m, h or d. "); 
		}		
		
		return seconds; 
	}
	
	/**
	 * Parse a data size in bytes from the given formatted data size.   
	 * 
	 * @param size
	 * @return
	 * @throws NumberFormatException
	 */
	public static double parseDataSizeToBytes(String size) throws NumberFormatException 
	{
		if (size == null)
		{
			throw new NumberFormatException("Internal error: size is null. ");
		}
		
		if (size.endsWith("0") && Double.parseDouble(size) == 0) 
		{
			 return 0; 
		}			
		else if (size.endsWith("GB")) 
		{
			return Double.parseDouble(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
		}		
		else if (size.endsWith("MB")) 
		{
			return Double.parseDouble(size.substring(0, size.length() - 2)) * 1024 * 1024;		
		}
		else if (size.endsWith("kB")) 
		{
			return Double.parseDouble(size.substring(0, size.length() - 2)) * 1024;		
		}
		else if (size.endsWith("B")) 
		{
			return Double.parseDouble(size.substring(0, size.length() - 1));		
		}
		else 
		{
			throw new NumberFormatException("Data size must be a number suffixed by GB, MB, kB or B. "); 
		}		
	}
	
	/**
	 * Parses a data rate including a data size followed by a "/s" and returns it as a 
	 * number of bytes per second. 
	 * 
	 * @param rate
	 * @return
	 */
	public static double parseDataRateToBytesPerSecond(String rate) throws NumberFormatException
	{
		if (rate.endsWith("0")) {
			return parseDataSizeToBytes(rate);
		}
		else if (rate.endsWith("/s") || rate.endsWith("ps")) {
			return parseDataSizeToBytes(rate.substring(0, rate.length() - 2));
		}
		else {
			throw new NumberFormatException("Data rate must be a number suffixed by GB/s, MB/s, kB/s or B/s. "); 
		}			
	}
}
