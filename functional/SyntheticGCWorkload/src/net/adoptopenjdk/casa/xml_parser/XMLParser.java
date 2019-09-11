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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.adoptopenjdk.casa.event.EventHandler;

/**
 * A flexible XML parser  
 * 
 * TODO: Implement XML entity support 
 * 
 *  
 */
public class XMLParser 
{
	private XMLHierarchy hierarchy; 
	private EventHandler parseError;  	
	private HashMap<String, ElementHandler> elementHandlers; 	
	
	private boolean isError; 
	
	/**
	 * Constructs a new parser that uses the given error handler for 
	 * parse errors. 
	 * 
	 * @param filename
	 * @param hierarchy
	 * @param parseError
	 */
	public XMLParser(XMLHierarchy hierarchy, EventHandler parseError)
	{					
		this.hierarchy  = hierarchy; 
		this.parseError = parseError;
		isError = false; 
		this.elementHandlers = new HashMap<String, ElementHandler>(); 
	}	
	
	/**
	 * Returns true if there was an error during parsing. 
	 * 
	 * @return
	 */
	public boolean getErrorStatus()
	{
		return isError; 
	}
	
	/**
	 * Adds a handler for the given element name. The handler has methods which are 
	 * called both when an opening tag is parsed and then the corresponding 
	 * closing tag is parsed.
	 * 
	 * The elementStart implementation is responsible for adding the element as a 
	 * child of its parent element, if this behavior is desired. Optionally, it can 
	 * be omitted so that the elements, once parsed, can be garbage collected.  
	 * 
	 * @param elementName
	 * @param handler
	 */
	public void setElementHandler(String elementName, ElementHandler handler) 
	{
		elementHandlers.put(elementName, handler);
	} 
		
	/**
	 * Parses XML from the given stream with the given name. 
	 * 
	 * @param stream
	 * @param streamName
	 */
	public void parse(InputStream stream, String streamName)
	{
		try 			
		{
			LexicalAnalizer lex = new LexicalAnalizer();			
			lex.lex(stream, streamName, new Parser());
		} 
		catch (ParseError | IOException e) 
		{
			parseError.issue(e, "failed to parse " + streamName); 
		}		
	}
	
	/**
	 * Parses XML from the given file. 
	 * 
	 * @param filename
	 */
	public void parse(String filename)
	{
		try 
		{
			parse(new FileInputStream(filename), filename);
		} 
		catch (FileNotFoundException e) 
		{
			parseError.issue(e);
		} 	
	}
	
	/**
	 * Tokenizes the document and calls the parser for each recognized token.
	 * 
	 * TODO: Doesn't recognize entities yet. 
	 * 
	 *  
	 */
	private class LexicalAnalizer 
	{
		private static final String TAG_TOKEN = "[<]([^<>]*)[>]";
		private static final String COMMENT_TOKEN = "[!][-]{2}(.*)[-]{2}";
		private static final String NAME_TOKEN = "[A-Za-z][A-Za-z0-9_-]*";		
		private static final String ATTRIBUTE_VALUE_TOKEN = "[^\"]*";
		private static final String ATTRIBUTE_EQUALS_TOKEN = "[ ]*[=][ ]*"; 	
		private static final String ATTRIBUTE_TOKEN = "(" + NAME_TOKEN + ")" + ATTRIBUTE_EQUALS_TOKEN +  "[\"]" + "(" + ATTRIBUTE_VALUE_TOKEN + ")" + "[\"]";
		
		/**
		 * Tokenizes the file, parsing each token using the given parser. 
		 * 
		 * @param file
		 * @param parser
		 * @throws IOException
		 * @throws ParseError
		 */
		private void lex(InputStream stream, String filename, Parser parser) throws IOException, ParseError
		{			 						
			int lineNumber = 1; 
			int columnNumber = 1;
				
			StringBuilder chars = new StringBuilder(); 							
			Stack<StringBuilder> stack = new Stack<StringBuilder>(); 
			Stack<XMLParserState> state = new Stack<XMLParserState>();  
	
			// Start in the DEFAULT state
			state.push(XMLParserState.DEFAULT); 
			
			// Read the stream character by character 
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			int i;		
			while((i = reader.read()) != -1) 
			{
				if (Thread.interrupted()) 
				{
					Thread.currentThread().interrupt(); 
					break; 
				}

				char c = (char) i;
				
				// Ignore carriage returns 
				if (c == '\r')
					continue;
				
				Location location = new Location(filename, lineNumber, columnNumber);
				
				// Count lines and columns 
				if (c == '\n')
				{
					lineNumber++; 
					columnNumber = 0;
				}
				else 
				{
					columnNumber++;
				} 			
				
				// Validate the stack. 
				if (stack.isEmpty() && !state.peek().equals(XMLParserState.DEFAULT))
					throw new ParseError("Internal error: stack empty in " + state.peek().name() + " state.", location);
				else if (!stack.isEmpty() && state.peek().equals(XMLParserState.DEFAULT))							
					throw new ParseError("Internal error: stack not empty in " + state.peek().name() + " state.", location);
				
				/*
				 *  If we're currently parsing a comment, we 
				 *  just look for --> and ignore everything else.  
				 */
				if (state.peek().equals(XMLParserState.COMMENT))
				{											
					stack.peek().append(c);
					
					// Look for the --> and end the comment if we find it. 
					if (stack.peek().toString().endsWith("-->"))
					{
						// leave the COMMENT state. 
						state.pop(); 
						// Discard the parsed comment. 
						stack.pop();
					}												
				}
				// Entity state: entities are validated in the lexer but are not elaborated until parsing. 
				else if (state.peek().equals(XMLParserState.ENTITY))
				{										
					switch (c)
					{
						// < and > are not allowed within an entity (or string) 
						case '>':
						case '<':
						case '"':							
							throw new ParseError("Unexpected " + c + " in entity; expected ';' ", location);						
						case ';': 	
						{
							// The entity name will be the last thing on the stack
							String name = stack.peek().toString();
							
							// Look up the entity name to validate it  
							String value = XMLEntities.get(name);
							
							// Invalid entities are detected here 
							if (value == null)								
								throw new ParseError("Invalid entity: " + name, location);
							
							// Append the ; as it will be needed to parse the entity later. 
							stack.peek().append(c);
							// Leave the ENTITY state. 
							state.pop(); 						
							break;
						}
						default:
							// Append chars, assuming they are part of the entity name. 
							stack.peek().append(c);
							break;						
					}					
					
				}
				// If we're in a string, process entities and exit when we parse the end quote 
				else if (state.peek().equals(XMLParserState.STRING) || state.peek().equals(XMLParserState.STRING_SINGLE_QUOTED))
				{ 	
					stack.peek().append(c);
																
					final char quoteChar = state.peek().equals(XMLParserState.STRING_SINGLE_QUOTED) ? '\'' : '"';
					
					switch (c)
					{														
						case '>':
						case '<':												
							throw new ParseError("Unexpected " + c + " in string; expected '" + quoteChar + "' ", location);						 
						case '&':
							// Enter entity 
							stack.push(new StringBuilder());								
							state.push(XMLParserState.ENTITY); 
							break; 	
						// Detect the end quote and leave the quote state. 
						case '\'':
						case '"':
							if (c == quoteChar)
								state.pop(); 
					}												
				} 				
				// We've seen a < but haven't yet seen the >, so we're in a tag. 
				else if (state.peek().equals(XMLParserState.TAG))
				{						
					switch (c) 
					{	
						case '<':						
							throw new ParseError("unexpected " + c + " within tag, expected >", location);					
						// Found the end of a tag; parse it. 
						case '>':	
						{								
							// Append the > to the tag. 
							stack.peek().append(c);
							
							// Pop the end of the tag from the stack. 
							StringBuilder builder = stack.pop(); 							
														
							// Prepend everything else on the stack into the final tag by appending the current builder to the next item on the stack.  
							while (!stack.isEmpty())
							{
								// Append this builder to the next builder on the stack down and make that the current builder.  
								stack.peek().append(builder.toString());									
								builder = stack.pop();
							}
							
							// Parse the tag. 
							parseToken(builder.toString(), location, parser);
							
							// leave the TAG state. 
							state.pop(); 
							
							break;
						}	
						// Detect comments, if this tag is a comment, we leave the TAG state and enter the COMMENT state.  
						case '-':
						{								
							stack.peek().append(c);
							
							if (stack.peek().toString().equals("<!--"))
							{
								// Change to the state to the COMMENT state  
								state.pop(); 								
								state.push(XMLParserState.COMMENT);
							}
														
							break;
						}
						// Starting quote (" or ') of a string. Push a new builder, and enter the appropriate String state.   
						case '"':
						case '\'':
							stack.push(new StringBuilder());
							stack.peek().append(c);						
							state.push(c == '"' ? XMLParserState.STRING : XMLParserState.STRING_SINGLE_QUOTED);						
							break; 
						case '\n':
						case '\t':
							// Newlines and tabs within tags get changed to spaces 
							stack.peek().append(" ");
							break; 
						default:	
							// Append any other text to the tag. 
							stack.peek().append(c);
							break; 
					}
				}
				// The default state represents anything not in a tag. 
				else if (state.peek().equals(XMLParserState.DEFAULT))
				{					
					switch (c) 
					{		
						case '>':						
							throw new ParseError("unexpected  " + c + "", location);						
						case '<':
						{														
							/*
							 *  We hit an opening <. We need to parse any loose text 
							 *  before proceeding to the TAG state.
							 */
							parseToken(trimChars(chars.toString()), location, parser);
							chars = new StringBuilder();												
							
							// Enter the TAG state. 
							state.push(XMLParserState.TAG);
							stack.push(new StringBuilder());
							stack.peek().append(c);
							break;
						}											
						default:
							// Append chars
							chars.append(c);  					
					}
				}
				else 
				{				
					throw new ParseError("Unexpected state: " + state.peek().name(), location);				
				}
			}
		}
		
		/**
		 * Parses a single XML attribute, unescaping the value field. 
		 * 
		 * @param attribute
		 * @param location
		 * @param attributes
		 * @throws ParseError
		 */
		private void parseAttribute(String attribute, Location location, HashMap<String, String> attributes) throws ParseError
		{
			Matcher matcher = Pattern.compile(ATTRIBUTE_TOKEN).matcher(attribute);
				
			if (matcher.find()) 
			{
				String name  = matcher.group(1); 
				String value = XMLEntities.convertEntities(matcher.group(2), location); 
				
				attributes.put(name, value);
			}
			else 
			{
				throw new ParseError("Failed to parse attribute from " + attribute, location);
			} 
		}
		
		/**
		 * Parses 0 or more attributes from the given string. 
		 * 
		 * @param string
		 * @param location
		 * @param parser
		 * @return
		 * @throws ParseError
		 */
		private HashMap<String, String> parseAttributes(String string, Location location, Parser parser) throws ParseError
		{
			HashMap<String, String> attributes = new HashMap<String, String>();
			
			Matcher matcher = Pattern.compile("(" + ATTRIBUTE_TOKEN + ")").matcher(string);								 			
			while(matcher.find()) 						
				parseAttribute(matcher.group(1), location, attributes);
			
			return attributes; 
		}
		
		/**
		 * Determines if the given string containing the contents of a tag represents 
		 * a "single tag", or one which does not require a matching end tag.  
		 * 
		 * @param string
		 * @return
		 */
		private boolean isSingleTag(String string)
		{
			// If the tag ends with a /, it's a single tag. 
			return Pattern.compile("([/]{1})$").matcher(string).find();
		}
		
		/**
		 * Parses a start tag's contents including name and 
		 * attributes. 
		 * 
		 * @param string
		 * @param location
		 * @param parser
		 * @throws ParseError
		 */
		private void parseStartTag(String string, Location location, Parser parser) throws ParseError
		{		
			Matcher matcher = Pattern.compile("^(" + NAME_TOKEN + ")([^<>]*)$").matcher(string);
			
			if (matcher.find())
			{													
				String name = matcher.group(1); 							
				parser.startElement(name, parseAttributes(matcher.group(2), location, parser), location);
										 			
				if (isSingleTag(string)) 				
					parser.endElement(name, location);				
			}
			else 
			{
				throw new ParseError("Failed to parse start tag from " + string, location);
			}											
		}
		
		/**
		 * Parses an XML tag from the given string containing the chars between the &lt; and &gt;  
		 * 
		 * @param string
		 * @param location
		 * @param parser
		 * @throws ParseError
		 */
		private void parseTag(String string, Location location, Parser parser) throws ParseError
		{		
			Matcher meta = Pattern.compile("^[?]([^<>]*)[?]$").matcher(string);
			Matcher start = Pattern.compile("^([^/<>][^<>]*)$").matcher(string);				
			Matcher end = Pattern.compile("^[/]([^<>]*[^/])$").matcher(string);
			Matcher comment = Pattern.compile("^" + COMMENT_TOKEN + "$").matcher(string);
			
			if (meta.matches()) 
			{
				//String content = metaTag.group(1);
				// TODO: handle meta tag
			}
			else if (comment.matches())
			{
				// Ignore comments. 
			}
			else if (start.find())
			{	
				String content = start.group(1);
							
				parseStartTag(content, location, parser);
			}
			else if (end.find()) 			
			{		
				String content = end.group(1);	
				
				Matcher matcher = Pattern.compile("^(" + NAME_TOKEN + ")$").matcher(content);
				
				if (matcher.find())
				{
					String name = matcher.group(1);
					
					parser.endElement(name, location);
				}
				else 
				{
					throw new ParseError("Invalid end tag " + string, location); 
				}
			}			
			else 
			{
				throw new ParseError("Unrecognised tag " + string, location); 
			}
		}
		
		/**
		 * Trims any leading and trailing whitespace from the chars. If nothing is left, 
		 * return null. 
		 * 
		 * @param chars
		 * @return
		 */
		private String trimChars(String chars)
		{
			Matcher matcher = Pattern.compile("^[\\s]*([\\S][.]*)[\\s]*$").matcher(chars);
			
			if (matcher.find()) 
			{
				return matcher.group(1);
			}
			else 
			{
				return null; 
			}
		}
		
		/**
		 * Parses the given entity as either a tag or chars. 
		 * 
		 * @param entity
		 * @param location
		 * @param parser
		 * @throws IOException
		 * @throws ParseError
		 */
		private void parseToken(String entity, Location location, Parser parser) throws IOException, ParseError
		{								
			if (entity == null)
				return; 
			
			Matcher matcher = Pattern.compile("^" + TAG_TOKEN + "$").matcher(entity);
				
			if (matcher.find())
			{
				parseTag(matcher.group(1), location, parser);
			}
			else 
			{				
				parser.characters(entity, location);
			}			
		}
	}
	
	/**
	 * Handles parsing the structure of an XML file.     
	 * 
	 *  
	 *
	 */
	public class Parser
	{ 											
		private Stack<Element> elementStack;
		
		/**
		 * Creates a new parser handler with an element stack. 
		 */
		public Parser() 
		{
			elementStack = new Stack<Element> (); 			
		}
				
		/**
		 * Gets the handler associated with the given element name. If no 
		 * handler has been associated, returns the default handler. 
		 * 
		 * @param elementName
		 * @return
		 */
		private ElementHandler getElementHandler(String elementName)
		{
			ElementHandler handler = elementHandlers.get(elementName); 
			
			if (handler == null) 
				return new DefaultElementHandler();			
			else 			
				return handler; 
		}
	       
		/**
		 * Gets the element on the top of the stack (without popping it) or null. 
		 * 
		 * @return an Element or null
		 */
		private Element getParent()
		{
			Element parent = null;
    		if (!elementStack.isEmpty())
    			parent = elementStack.peek(); 
    		
    		return parent; 
		}
		
	    /**
	     * Handles the start of each element. 
	     * 
	     * @param name
	     * @param attributes
	     * @param location
	     */
	    public void startElement(String name, HashMap<String, String> attributes, Location location) 
	    {		   	    		    
	    	try         	        
	    	{	    			    		    			    		        		        	    			    			    		    	
	    		// Get the parent element from the top of the stack. 
	    		Element parent = getParent();
	    		
	    		// Construct a new element. 
	    		Element element = new Element(name, attributes, location, hierarchy);
	    			
	    		// Top level elements can have no parent. 
	    		if (parent == null) 
	    		{
	    			if (!hierarchy.isTopLevel(element))
	    				throw new ParseError(element.getName() + " tag may not be present at the top level.", element);
	    		}
	    		else 
	    		{
	    			if (hierarchy.isTopLevel(element)) 
	    				throw new ParseError(element.getName() + " may only be present at the top level", element);
	    			
	    			if (!hierarchy.isValidChildOf(parent, element))	        				        		
	    				throw new ParseError(element.getName() + " must not be nested in " + parent.getName(), element);	    			
	    			
	    			element.setParent(parent);
	    		}	    			    		
	    					    		    		 	  
	    		// Call element start handler. 
	    		getElementHandler(element.getName()).elementStart(element);
	    		
	    		// Stack the element. 
	    		elementStack.push(element);	    		    
	        }	    	
	    	catch (Throwable e)
	    	{
	    		parseError.issue(e);
	    	}
	    }
	        
	    /**
	     * Handles any text within the document. 
	     */
	    public void characters(String chars, Location location) 
	    {
	    	try 
	    	{
	    		// Resolve the text's parent element. 
	    		Element parent = getParent();     	
		    			    			    			    	 
		    	if (parent == null)
		    		throw new ParseError("found text not belonging to any element: "+ chars, location);
		    	
		    	// Append the text to its parent.
		    	parent.appendText(XMLEntities.convertEntities(chars, location));		    	 		    			    				    
		    }	    	
	    	catch (Throwable e)
	    	{
	    		parseError.issue(e);
	    	}
	    }
	    
	    /**
	     * Handles the end of each element. 
	     */
	    public void endElement(final String name, Location location)
	    {		    		    
	    	try
	    	{   	   
	    		// Pop an unclosed element off the stack 
	    		Element element = null;
	    		if (!elementStack.isEmpty())
	    			element = elementStack.pop(); 

	    		// Make sure there was an element on the top of the stack. 
	    		if (element == null)
	    			throw new ParseError(name + " closing but nothing to close.", location); 
	    	        	        	
	    		// Make sure the element has had its manditory children parsed. 
	    		element.checkForManditoryChildren(); 

	    		// Make sure we're not closing the top level element with other elements still on the stack 
	    		if (element.isTopLevel() && !elementStack.isEmpty())
	    			throw new ParseError(element.name + " is top level but unclosed elements remain. ", element);    			    			    			    
	    			    
	    		// Call the element's end handler. 
	    		getElementHandler(element.getName()).elementEnd(element);
	    	}
	    	catch (Throwable e)
	    	{
	    		parseError.issue(e); 
	    	}
	    }	    	                                     	       	                       	   
	}
}
