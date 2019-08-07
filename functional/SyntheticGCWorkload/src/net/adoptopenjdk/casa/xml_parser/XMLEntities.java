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

import java.util.HashMap;
import java.util.Stack;

/**
 * Decodes XML entity codes for the XMLParser 
 * 
 *  
 *
 */
public class XMLEntities 
{
	/*
	 * The entity table is populated by the static initializer. It contains 
	 * name->value pairs of entity names and their decoded values. 
	 */
	private static final HashMap<String, String> ENTITY_TABLE; 
	
	static 
	{
		/*
		 * Parallel arrays for defining the names and decoded values of 
		 * entities. Supporting only the 5 XML special characters   
		 */
		final String[] ENTITY_NAMES  = {"amp", "lt", "gt", "quot", "apos"};
		final String[] ENTITY_VALUES = {"&",   "<",  ">",  "\"",   "'"};
		
		ENTITY_TABLE = new HashMap<String, String>();
		
		// Populate the entity table with name->value pairs. 
		for (int i = 0; i < ENTITY_NAMES.length; i++)		
			ENTITY_TABLE.put(ENTITY_NAMES[i], ENTITY_VALUES[i]);		
	}
	
	/**
	 * Looks up the given entity name in the entity 
	 * table. Returns the resulting symbol, or null 
	 * if the entity is not present. If the name is 
	 * null, null is returned.  
	 * 
	 * The entity name may not include the & or ;
	 * 
	 * @param name - the entity name. (eg: for "&lt;" the name will be "lt" and "<" will be returned.) 
	 * @return The decoded entity or null if not present. 
	 */
	public static String get(String name)
	{
		if (name == null)
			return null; 
		
		return ENTITY_TABLE.get(name);
	}
	
	/**
	 * Parses the given string, decoding and replacing all recognized 
	 * XML entities. A ParseError is thrown if any entity is invalid 
	 * or if a special charactar is encountered.   
	 * 
	 * @param string
	 * @param location
	 * @return
	 * @throws ParseError
	 */
	public static String convertEntities(String string, Location location) throws ParseError
	{
		Stack<StringBuilder> stack = new Stack<StringBuilder>();
		stack.push(new StringBuilder());
		
		boolean inEntity = false; 
		
		// Walk the string and decode any entities. 
		for (int i = 0; i < string.length(); i++)
		{
			char c = string.charAt(i);
			
			// Check for special characters 
			switch(c)
			{
				// & is not allowed when we are already parsing an entity 
				case '&': if (!inEntity) break; 
				// If we find any un-encoded special characters, we're throw a ParseError. 
				case '<': 
				case '>': 
				case '"':
				case '\'':
					throw new ParseError("Unexpected special charactar " + c + " in string.", location);					
			}
			
			// If we've parsed an &, we're in the inEntity state until we parse the matching ; 
			if (inEntity)
			{		
				// Parse the entity name. On ; pop the name from the stack and look it up. 
				switch(c)
				{					
					// ; found, try to decode the entity
					case ';':	
						/*
						 * The entity name will be the last thing on the 
						 * stack. Pop it, since it won't be part of the 
						 * final string.  
						 */
						String name = stack.pop().toString();

						// Look up the entity in the table. 
						String value = get(name);
						
						// Entity not present in entities table. 
						if (value == null)
							throw new ParseError("Unknown XML entity: " + name, location); 
						
						// No string present to append to...this is not possible under normal circumstances  
						if (stack.isEmpty())
							throw new ParseError("Internal error: stack empty in entity.", location);
						
						// Append the decoded entity to the string
						stack.peek().append(value);
						
						// Leave the entity parsing state 
						inEntity = false;
						
						break; 
					default: 
						stack.peek().append(c);
				}
			}
			// If we're not in an entity, append characters to the string until we hit a & 
			else 
			{
				// Copy string until we hit an &, then start processing the entity
				switch(c)
				{					
					// & means we state-change to entity processing. 
					case '&':
						inEntity = true; 
						stack.push(new StringBuilder()); 
						break; 
					default: 
						stack.peek().append(c);
				}
			}
		}
		
		// Pop the end of the completed string 
		StringBuilder builder = stack.pop();
		
		// Prepend any other strings on the stack to form the final string. 
		while (!stack.isEmpty())
		{
			stack.peek().append(builder.toString());									
			builder = stack.pop();
		}
		
		return builder.toString(); 
	}
}
