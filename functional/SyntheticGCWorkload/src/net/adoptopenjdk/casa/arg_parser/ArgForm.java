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
 * Arguments come in three main forms: structured LONG and SHORT arguments and UNSTRUCTURED. 
 * 
 * This enum provides some basic functionality for working with these forms. 
 *  
 */
public enum ArgForm 
{
	/**
	 *  Just a bunch of parameters with no argument identifier
	 */
	UNSTRUCTURED
	{		
		public String getPrefix() { return ""; }
		public String getIdentifier(Arg arg) throws ArgException { return ArgForm.getIdentifier(); }								
	},
	/**
	 * Long arguments start with --
	 */
	LONG 
	{ 
		public String getPrefix() { return "--"; }
		public String getIdentifier(Arg arg) throws ArgException { return ArgForm.getIdentifier(arg.getLongForm()); }
	},
	/**
	 * Short arguments form in clusters starting with -
	 */
	SHORT 
	{ 
		public String getPrefix() { return "-"; }
		public String getIdentifier(Arg arg)  throws ArgException { return ArgForm.getIdentifier(arg.getShortForm()); }
	};
	
	public abstract String getPrefix();
	public abstract String getIdentifier(Arg arg) throws ArgException;
	
	public static String getIdentifier(String string) 
	{
		return LONG.getPrefix() + string; 
	}
	
	public static String getIdentifier(char c) 
	{
		return SHORT.getPrefix() + c; 
	}
	
	public static String getIdentifier()
	{
		return UNSTRUCTURED.getPrefix(); 
	}
}