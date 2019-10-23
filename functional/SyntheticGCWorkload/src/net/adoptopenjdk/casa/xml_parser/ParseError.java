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
 * A parse error is thrown whenever the input XML file doesn't conform to the 
 * established format.  
 * 
 *  
 *
 */
public class ParseError extends Exception 
{
	private String message;				
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructs a new ParseError with location information from the given element. 
	 * 
	 * @param message
	 * @param element
	 */
	public ParseError(String message, Element element) 
	{
		this.message = ""; 
		
		if (element != null)
			this.message += element.getLocation().getFilename() +": line " + element.getLocation().getLineNumber() + ":" + element.getLocation().getColumnNumber() + ": <" + element.getName() + " ...> tag: "; 
		
		if (message != null)
			this.message += message;								
	}
	
	/**
	 * Constructs a new ParseError with the given message and information from the given locator. 
	 * 	
	 * @param message
	 * @param locator
	 */
	public ParseError(String message, Location location) 
	{
		this(location.getFilename() + ": " + location.getLineNumber() + ":" + location.getColumnNumber() + ": " + message, (Element)null);					
	}
	
	/**
	 * Constructs a parse error with no location information
	 * 
	 * @param message
	 */
	public ParseError(String message)
	{
		this(message, (Element)null);
	}
	
	/**
	 * Constructs a new ParseError with location information from the element.  
	 * 	
	 * @param element
	 */
	public ParseError(Element element) 
	{
		this((String)null, element);
	}
	
	/**
	 * Constructs a new ParseError with the given message location information from the given element. 
	 * 	
	 * @param message
	 * @param element
	 */
	public ParseError(Throwable t, Element element) 
	{
		this(t.getMessage(), element);				
	}
	
	/**
	 * Produces an error string showing the message and location information, if available.   
	 */
	public String toString()
	{
		return message; 
	}
	
	/**
	 * Returns a string representation of this parse error. 
	 */
	public String getMessage()
	{
		return toString(); 
	}	
}
