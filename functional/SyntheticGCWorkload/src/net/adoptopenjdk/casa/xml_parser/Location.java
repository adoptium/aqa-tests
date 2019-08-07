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
 * Represents a location in a source file. 
 * 
 *  
 */
public class Location 
{
	// The line number, usually from 1. 
	private int lineNumber; 
	
	// The column number, usually from 1. 
	private int columnNumber; 
	
	// The filename, usually as given on the command line. 
	private String filename; 
	
	public Location (String filename, int lineNumber, int columnNumber)
	{
		this.filename = filename; 
		this.lineNumber = lineNumber; 
		this.columnNumber = columnNumber; 	
	}
	
	public String getFilename()
	{
		return filename; 
	}
	
	public int getLineNumber()
	{
		return lineNumber; 
	}
	
	public int getColumnNumber()
	{
		return columnNumber; 
	}
}
