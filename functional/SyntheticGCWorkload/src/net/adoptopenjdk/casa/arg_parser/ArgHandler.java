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
package net.adoptopenjdk.casa.arg_parser;

/**
 * Called after the command line is parsed on every parsed 
 * argument. 
 * 
 *  
 */
public interface ArgHandler 
{	
	/**
	 * Parse parameter 0 from arg as follows
	 * 
	 * Type parameter = arg.getParameterType(0).parse(arg.getParameter(0));
	 * 
	 * Parameters can be parsed directly into their respective types. 	
	 * 
	 * @param arg
	 * @throws ArgException
	 */
	public void handle(Arg arg) throws ArgException;	
}