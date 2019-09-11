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
 * Handler interface for XMLParser
 * 
 *  
 */
public interface ElementHandler 
{
	/**
	 * Action to be performed when an element opening tag is encountered. Element will 
	 * be populated with attributes and will be assigned a parent. Child elements will 
	 * not yet have been parsed and any enclosed text will not yet be appended. 
	 * 
	 * Use the DefaultElementHandler for child elements (or implement elementEnd in a 
	 * DefaultElementHandler with a call to super.elementEnd(element)) to append child 
	 * elements.   
	 * 
	 * @param element
	 * @throws HierarchyException
	 * @throws ParseError
	 */
	public void elementStart(Element element) throws HierarchyException, ParseError;
	
	/**
	 * Action to be performed when an element closing tag is encountered.
	 * 
	 * At the time of call a reference to the element's parent, and any attributes 
	 * will have been added. 
	 * 
	 * @param element
	 * @throws HierarchyException
	 * @throws ParseError
	 */
	public void elementEnd(Element element) throws HierarchyException, ParseError;	
}
