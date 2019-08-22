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
 * Internally, the parser populates each element with all parameters and supplies 
 * a reference to its parent before calling elementStart(). It appends any enclosed 
 * text by the time the endElement() handler is called. 
 * 
 * The VoidElementHandler doesn't append the element's children, leaving it up to 
 * user to decide what to do with them. This behavior is useful when parsing very large 
 * files as it avoids the problem of having to store all parsed elements. If the 
 * void element handler is used, the resulting parent element will not reference any of
 * the child elements that use this handler.   
 * 
 * A handler that ignores an element. 
 * 
 *  
 */
public class VoidElementHandler implements ElementHandler 
{
	public void elementStart(Element element) throws HierarchyException, ParseError { }
	public void elementEnd(Element element) throws HierarchyException, ParseError { }	
}