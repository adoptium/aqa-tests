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
import java.util.Map;

/**
 * A hierarchy template for the XML document. 
 * 
 *  
 */
public class XMLHierarchy 
{
	private String topLevel; 
	private HashMap<String, Children> hierarchy;
	private HashMap<String, Boolean> permissiveParents;
	
	/**
	 * Data structure which houses the allowable children of the 
	 * given element, indexed by name. 
	 * 
	 *  
	 */
	private class Children extends HashMap<String, Boolean>
	{					
		private static final long serialVersionUID = 1L;

		public Children()
		{
			super();					 
		}
		
		public void put(String name, boolean mustBePresent)
		{
			super.put(name, new Boolean(mustBePresent));
		}
		
		public String[] getManditoryChildNames()
		{
			ArrayList<String> names = new ArrayList<String>(); 
			
			for (Map.Entry<String,Boolean> entry : this.entrySet()) {				  
			  if (entry.getValue().booleanValue())
				  names.add(entry.getKey());
			}
			
			return names.toArray(new String[0]);
		}
	}
	
	/**
	 * Creates a new hierarchy with the given top level element name. 
	 * 
	 * @param topLevel
	 */
	public XMLHierarchy()
	{
		this(null); 
	}
	
	/**
	 * Creates a new hierarchy with the given top level element name. 
	 * 
	 * @param topLevel
	 */
	public XMLHierarchy(String topLevel)
	{		
		this.topLevel = topLevel;
		hierarchy = new HashMap<String, Children> ();
		permissiveParents = new HashMap<String, Boolean> ();
	}
	
	/**
	 * Determines if the given element is the top level element of 
	 * the hierarchy
	 * 
	 * @param element
	 * @return
	 */
	public boolean isTopLevel(Element element)
	{		
		if (element.getName().equals(topLevel))
			return true; 
		else 
			return false; 		
	}
	
	/**
	 * Determines if the given child is allowed to exist as a child of the 
	 * given parent.  
	 * 
	 * @param parent
	 * @param child
	 * @return true if the parent is permissive or the child is present in the parent's list of vailid children. 
	 * @throws ParseError - Thrown if the parent is absent from the hierarchy and the parent is not permissive. 
	 */
	public boolean isValidChildOf(Element parent, Element child) throws ParseError
	{
		/*
		 *  Checks to see if the parent or any of its ancestors will
		 *  allow non-declared children to be present. 
		 */		 
		if (isPermissive(parent))
			return true; 
		
		if (hierarchy.get(parent.getName()) == null)
			throw new ParseError(parent.getName() + " is absent from hierarchy", parent);
		
		return hierarchy.get(parent.getName()).containsKey(child.getName());			
	}
	
	/**
	 * Returns an array of known-valid child names for the given element. 
	 * 
	 * @param parent
	 * @return
	 */
	public String[] getManditoryChildNames(Element parent)
	{
		Children children = hierarchy.get(parent.getName());
		
		if (children != null)
			return children.getManditoryChildNames();
		else 
			return new String[0]; 
	}
	
	/**
	 * Adds a relation allowing "child" to be nested within "parent". 
	 * 
	 * @param parent
	 * @param child
	 * @param mustBePresent - If set true, relation must exist in the final document or an error will be issued, depending on implementation 
	 * @throws HierarchyException - Thrown if the child has the same name as the top-level element 
	 */
	public void addRelation(String parent, String child, boolean mustBePresent) throws HierarchyException
	{
		if (topLevel != null && child.equals(topLevel))
			throw new HierarchyException("child name matches that of the top level");
		
		if (hierarchy.get(parent) == null)
			hierarchy.put(parent, new Children());
		
		hierarchy.get(parent).put(child, mustBePresent);
	}
	
	/**
	 * Will the given element allow non-declared children. 
	 * 
	 * @param parent
	 * @return true if the given parent or its nearest ancestor is in the hierarchy and is permissive.  
	 */
	public boolean isPermissive(Element parent)
	{
		if (permissiveParents.get(parent.getName()) == null) { 
			if (parent.getParent() != null)
				return isPermissive(parent.getParent());
			else 
				return false; 
		}
		else {
			return permissiveParents.get(parent.getName()).booleanValue();
		} 
	}
	
	/**
	 * Makes the element with the given name permissive, allowing 
	 * undeclared child elements to exist within it. 
	 * 
	 * @param parent
	 */
	public void makePermissive(String parent) 
	{
		permissiveParents.put(parent, new Boolean(true));
	}
}
