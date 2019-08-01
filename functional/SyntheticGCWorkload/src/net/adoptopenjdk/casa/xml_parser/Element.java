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

import java.util.ArrayList;
import java.util.HashMap;

import net.adoptopenjdk.casa.data_structures.ListHash;
import net.adoptopenjdk.casa.event.EventHandler;

/**
 * Represents an element in an XML document.  
 * 
 *  
 */
public class Element 
{
	// The automatically generated id of this element, based on the order in which it was parsed. 
	int id; 
	
	// The name of this element 
	String name;
	
	// The location within the source file. 
	Location location; 
	
	// The parent of this element. Null indicates it is the root element. 
	Element parent; 
	
	// The hierarchy of the overall document. 
	XMLHierarchy hierarchy;	
	
	// The children, indexed by name 
	ListHash<String, Element> children;
	
	// The attributes by name 
	HashMap<String, String> attributes;
	
	// Any text enclosed in the element. 
	String text; 

	/**
	 * Creates a new element with the given name and attributes when parsed from the given 
	 * filename with the given hierarchy at the given line number.    
	 *
	 * @param name
	 * @param attributes
	 * @param location
	 * @param hierarchy
	 */
	protected Element(String name, HashMap<String, String> attributes, Location location, XMLHierarchy hierarchy)
	{
		this.id = 1; 
		this.name = name;
		this.text = null; 
		this.parent = null;
		this.location = location;  		 	
		this.hierarchy = hierarchy;
		this.attributes = attributes;
		
		this.children = new ListHash<String, Element>();
	}

	/**
	 * Gets any text contained within the element. 
	 * 
	 * @return
	 */
	public String getText()
	{
		return this.text; 
	}
	
	/**
	 * Adds text to this element. 
	 * 	
	 * @param text
	 */
	public void appendText(String text)
	{
		if (this.text == null)
			this.text = text; 
		else 
			this.text += text; 
	}
	
	/**
	 * Gets a reference to the hash map of attributes. 
	 * 
	 * @return
	 */
	public HashMap<String, String> getAttributes()
	{
		return attributes;
	}
	
	/**
	 * Gets an object with location information about the element in the source file. 
	 * 
	 * @return
	 */
	public Location getLocation()
	{
		return location; 
	}
	
	/**
	 * Gets the name of this element. 
	 * 
	 * @return
	 */
	public String getName()
	{
		return name; 
	}
	
	/**
	 * Gets the numeric ID of this element. 
	 * 
	 * @return
	 */
	public int getID()
	{
		return id; 
	}
	
	/**
	 * Sets the numeric ID of this element. 
	 * 
	 * @param id
	 */
	protected void setID(int id)
	{
		this.id = id; 
	}
	
	/**
	 * Sets the parent of this element. 
	 * 
	 * @param parent
	 */
	protected void setParent(Element parent)
	{
		this.parent = parent; 
	}
	
	/**
	 * Gets the parent of this element. 
	 * 
	 * @return
	 */
	public Element getParent()
	{
		return parent; 
	}
	
	/**
	 * Checks to see if this element is the top level element of the document. 
	 * 
	 * @return
	 */
	public boolean isTopLevel()
	{
		return hierarchy.isTopLevel(this);
	}
	
	/**
	 * Gets the symbol with the given name from the symbol table. 
	 * 
	 * @param key
	 * @return retrieves the data associated with the given string in the symbol table. Returns null if the symbol is no present.  
	 */
	public String getAttribute(String key)
	{
		return attributes.get(key);
	}
		
	/**
	 * Gets the symbol with the given name from the symbol table 
	 * and removes it. 
	 * 
	 * @param key
	 * @return same as getSymbol(), but the symbol (if found) is removed from the symbol table. 
	 */
	public String consumeAttribute(String key)
	{
		return attributes.remove(key);
	}
	
	/**
	 * Adds a child to this element. 
	 * 
	 * @param child
	 * @throws HierarchyException
	 * @throws ParseError
	 */
	protected void addChild(Element child) throws HierarchyException, ParseError
	{
		if (!hierarchy.isValidChildOf(this, child))
			throw new ParseError(child.getName() + " may not be a child of " + getName(), child);
				
		children.add(child.getName(), child);
		
		child.setID(children.size(child.getName()));		
	}
	
	/**
	 * Gets the number of children with the given name. 
	 * 
	 * @param name
	 * @return
	 */
	public int getNumChildren(String name) {
		return this.children.size(name); 
	}
	
	/**
	 * Gets the children of this element. 
	 * 
	 * @param name
	 * @return
	 */
	public ArrayList<Element> getChildren(String name)
	{
		return this.children.getList(name);					
	}
	
	/**
	 * Throws a parse error if any manditory children are missing.  
	 * 
	 * @throws ParseError
	 */
	public void checkForManditoryChildren() throws ParseError
	{		
		for (String name : hierarchy.getManditoryChildNames(this)) {
			if (getChildren(name) == null)
				throw new ParseError(getName() + " must have at least one " + name, this);			
		}	
	}
	
	/**
	 * Throws a parse error if the attribute table hasn't been emptied. 
	 * 
	 * @throws ParseError
	 */
	public void checkForUnusedAttributes() throws ParseError
	{
		if (getAttributes().size() > 0) {
			String error = "Unrecognized attributes: \n";
					
			String separator = ""; 
			
			for (String key : getAttributes().keySet()) {
				error += separator + "\"" + key + "\"";
				separator = ", ";
			}
			
			throw new ParseError(error + " in " + getName() +  " tag.", this);
		}
	}
	
	/**
	 * Warn if the symbol is present and remove it from the symbol table. 
	 * 
	 * @param key
	 * @param warning - Set an event handler to notify the user that a 
	 * deprecated attribute value has been found set in the symbol table. 
	 * @throws ParseError
	 */
	public void setAttributeDeprecated(String key, EventHandler warning) throws ParseError
	{
		if (key == null)
			throw new ParseError("key is null", this);
				
		if (attributes.get(key) != null) 
		{
			attributes.remove(key);
			if (warning != null)
				warning.issue(key + " attribute in " + getName() + " tag " + getID() + " on line " + getLocation().getLineNumber() + " is deprecated. Ignoring value."); 
		}
	}
	
	/**
	 * If the symbol table does not contain the given key, it 
	 * is added with the given value and a notice is issued. Otherwise 
	 * this method does nothing. 
	 * 
	 * @param key 
	 * @param value
	 * @param notice - issue this event type if the default is used. Don't issue if notice is null.  
	 */
	public void setDefaultForAttribute(String key, String value, EventHandler notice) throws ParseError
	{
		if (key == null)
			throw new ParseError("key is null", this);
		
		if (value == null)
			throw new ParseError("value is null", this);
				
		if (attributes.get(key) == null) 
		{
			attributes.put(key, value);
			if (notice != null)
				notice.issue(key + " attribute in " + getName() + " tag " + getID() + " on line " + getLocation().getLineNumber() + " defaulting to " + value); 
		}
	}
	
	/**
	 * Throws a descriptive ParseError if the given attribute 
	 * is not present in the symbol table. Used to assuer that 
	 * mandatory attributes are set.  
	 * 
	 * @param key - the name of the attribute which must be set. 
	 * @throws ParseError - thrown if the attribute with the given name is not set.   
	 */
	public void assertAttributeIsSet(String key) throws ParseError
	{		
		if (key == null)
			throw new ParseError("key is null", this);
		
		if (attributes.get(key) == null)
			throw new ParseError("manditory " + key + " attribute is missing in " + getName() + " tag.", this); 		
	}
}

