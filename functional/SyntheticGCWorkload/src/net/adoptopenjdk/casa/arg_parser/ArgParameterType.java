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
package net.adoptopenjdk.casa.arg_parser;

/**
 * Parsing and identification of parameter types present after 
 * an argument. 
 * 
 *  
 */
public enum ArgParameterType 
{
	/**
	 * A single int 
	 */
	INTEGER 
	{				
		@SuppressWarnings("unchecked")
		public Integer parse(String string) 
		{
			return new Integer(Integer.parseInt(string));
		}
	}, 
	/**
	 *  Multiple comma seaparated ints 
	 */
	INTEGER_ARRAY
	{
		@SuppressWarnings("unchecked")
		public int[] parse(String string) 
		{				
			String[] strings = string.split(",");
			
			int[] ints = new int[strings.length];
						
			for (int i = 0; i < strings.length; i++)
				ints[i] = Integer.parseInt(strings[i]);
			
			return ints; 
		}
	},		
	/**
	 *  Multiple comma separated doubles 
	 */
	DOUBLE_ARRAY
	{
		@SuppressWarnings("unchecked")
		public double[] parse(String string) 
		{				
			String[] strings = string.split(",");
			
			double[] doubles = new double[strings.length];
						
			for (int i = 0; i < strings.length; i++)
				doubles[i] = Double.parseDouble(strings[i]);
			
			return doubles; 
		}
	},	
	/**
	 *  An array of string, comma separated. 
	 */
	STRING_ARRAY
	{
		@SuppressWarnings("unchecked")
		public String[] parse(String string) 
		{				
			String[] strings = string.split(",");
											
			return strings; 
		}
	},
	/**
	 *  A single string present on the command line. May be used 
	 * repeatedly for a bespoke number of strings. 
	 */
	STRING
	{
		@SuppressWarnings("unchecked")
		public String parse(String string) 
		{
			return string;  
		}
	},	
	/**
	 * Multiple strings present on the command line, no limit on length 
	 */
	STRINGS
	{
		@SuppressWarnings("unchecked")
		public String[] parse(String string) 
		{			
			return string.split(" ");  
		}			
	};
	
	public abstract <T> T parse(String string);	
}