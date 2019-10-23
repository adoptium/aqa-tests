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
package net.adoptopenjdk.casa.util;

import net.adoptopenjdk.casa.xml_parser.Element;
import net.adoptopenjdk.casa.xml_parser.ParseError;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Abstract class used to parse configuration values from a 
 * specially constructed ParserElement containing the tag attributes and 
 * additional metadata. 
 * 
 *  
 *
 */
public abstract class AbstractConfiguration 
{	
	private Element element; 
	
	/**
	 * Creates a new configuration from the element 
	 * 
	 * @param element
	 * @throws ParseError 
	 */
	protected AbstractConfiguration(Element element) throws ParseError 
	{	
		if (element == null)
			throw new ParseError("element is null"); 
		
		this.element = element; 		 				
	}
	
	/**
	 * Gets the filename this configuration was read from. 
	 * 
	 * @return
	 */
	public String getFilename()
	{
		return getElement().getLocation().getFilename(); 
	}
	
	/**
	 * Gets the line number this configuration was read from
	 * 
	 * @return
	 */
	public int getLineNumber()
	{
		return getElement().getLocation().getLineNumber(); 
	}
	
	/**
	 * Gets the encapsulated element. 
	 * 
	 * @return the element added when the constructor was called. 
	 */
	protected Element getElement()
	{
		return element; 
	}
		
	/**
	 * Parses a proportion from symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected double parseProportionField(String key) throws ParseError {
		double proportion; 
		
		String value = getElement().consumeAttribute(key);
		
		try 
		{
			proportion = ParseUtilities.parseProportion(value); 
		}
		catch (NumberFormatException e) 
		{ 
			throw new ParseError(key + " is invalid. " + e.getMessage(), element); 
		}
				
		return proportion; 			
	}

	/**
	 * Parses a time (in seconds) from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected double parseTimeField(String key) throws ParseError 
	{
		double time; 
		
		String value = element.consumeAttribute(key);
		
		try 
		{
			time = ParseUtilities.parseTime(value);
		}
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid: " + e.getMessage(), element);
		}
				
		return time; 
	}
	
	/**
	 * Parses an integer from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected long parseLongField(String key) throws ParseError 
	{
		String value = element.consumeAttribute(key);
		
		long number; 
		try 
		{
			number = Long.parseLong(value);
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid. Must be a unitless long. (" + e.getMessage() + ")", element); 
		}
				
		return number; 		
	}
	

	/**
	 * Parses an integer from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected int parseIntegerField(String key) throws ParseError 
	{
		String value = element.consumeAttribute(key);
		
		int number; 
		try 
		{
			number = Integer.parseInt(value);
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid. Must be a unitless integer. (" + e.getMessage() + ")", element); 
		}
				
		return number; 		
	}

	/**
	 * Parses an unsigned integer from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected int parseUnsignedIntegerField(String key) throws ParseError 
	{
		String value = element.consumeAttribute(key);
		
		int number; 
		try 
		{
			number = Integer.parseInt(value);
			if (number < 0)
				throw new NumberFormatException("must not be negative"); 
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid. (" + e.getMessage() + ")", element); 
		}
				
		return number; 		
	}
	
	
	
	/**
	 * Parses a data size value (in bytes) from the symbol table value at the given key. 
	 * 
	 * @param key
	 * @return
	 * @throws ParseError
	 */
	protected double parseDataSizeField(String key) throws ParseError
	{
		String value = element.consumeAttribute(key);
		
		double size; 
		try 
		{
			size = ParseUtilities.parseDataSizeToBytes(value);
		} 
		catch (NumberFormatException e) 
		{
			throw new ParseError(key + " is invalid: " + e.getMessage(), element); 
		}
				
		return size; 
	}
}
