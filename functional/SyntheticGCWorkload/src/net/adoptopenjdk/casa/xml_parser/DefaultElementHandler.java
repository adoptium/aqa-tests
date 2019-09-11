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
 * Internally, the parser populates each element with all parameters, supplies 
 * a reference to its parent and appends any enclosed text by the time the 
 * endElement() handler is called.  
 *  
 * The default handler simply adds each element to its parent element's 
 * list of children. This is useful if you want to build an AST of elements
 * and have access to an element's children when its endElement() method is 
 * called.  
 * 
 * If this behavior is not required, use the VoidElementHandler. 
 * 
 *  
 */
public class DefaultElementHandler extends VoidElementHandler 
{	
	/**
	 * This implementation adds the element as a child of its parent.  
	 */
	public void elementStart(Element element) throws HierarchyException, ParseError 
	{
		// Add the element to its parent, forming an AST. 
		if (!element.isTopLevel() && element.getParent() != null)
			element.getParent().addChild(element);
	}	
}
