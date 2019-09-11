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

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * A collection of user-defined arguments 
 * 
 *  
 */
public class Args extends ArgSet
{
	// Hashes all argument identifiers to their Arg instances. 
	private HashMap<String, Arg> argTable;	 
		
	/**
	 * 
	 * 
	 * @param manditory - Does the program require at least one argument to be supplied? 
	 */
	protected Args (boolean manditory)
	{
		super(manditory);		
		argTable = new HashMap<String, Arg>();			
	}	
	
	/**
	 * Matches the found argument to an expected argument in the arg table. 
	 * 
	 * @param identifier
	 * @param form
	 * @return
	 * @throws ArgException
	 */
	protected Arg matchArg(String identifier, ArgForm form) throws ArgException
	{		
		if (form.equals(ArgForm.UNSTRUCTURED) && !identifier.equals(ArgForm.getIdentifier()))
			throw new ArgParseError("invalid identifier for argument: " + identifier);
					
		Arg arg = argTable.get(identifier); 
		
		// Make sure the argument is present. 
		if (arg == null)
			throw new UnrecognizedArgException("invalid argument " + identifier);
			
		// Check to see if the argument can receive additional parameters. 
		if (!arg.canBeParsed())
			throw new ArgParseError((identifier.equals("") ? "<unstructured argument>" : identifier) + " already parsed");
		
		// If the argument hasn't already been parsed, we can set its form and parsed flag.  
		if (!arg.isParsed())
		{
			if (arg.isUnstructured())		
				arg.setParsed();
			else 
				arg.setParsed(form);
		}
		
		return arg; 
	}	
	
	/**
	 * Executes the handlers on all arguments which have been parsed. 
	 */
	protected void executeHandlers() throws ArgException, UsageException
	{	
		if (!isSatisfied())
			throw new ArgParseError("One or more manditory arguments are absent");
		
		super.executeHandlers();
	}
	
	/**
	 * Traverse the arg tree and place args in the arg table. All arguments 
	 * are place in the table using to their short and long forms, or 
	 * if UNSTRUCTURED, are indexed by an empty string.  
	 * 
	 * @throws ArgException
	 */
	protected void buildArgTable() throws ArgException
	{
		ConcurrentLinkedQueue<ArgSet> queue = new ConcurrentLinkedQueue<ArgSet>(); 
		
		queue.offer(this);
		
		while (!queue.isEmpty()) 
		{
			ArgSet set = queue.poll(); 
			
			for (Arg arg : set.getArgs()) 
			{	
				queue.offer(arg);
			
				if (!arg.isUnstructured()) 
				{							
					addArgForm(arg, ArgForm.LONG);
					addArgForm(arg, ArgForm.SHORT);
				}
				else 
				{ 
					addArgForm(arg, ArgForm.UNSTRUCTURED);
				}				
			}
			
			for (ArgSet subset : set.getSets()) 
			{	
				queue.offer(subset);
			}
		}
	}
	
	/**
	 * Adds the argument form if it is not already present in the table. 
	 * 
	 * @param arg
	 * @param form
	 * @throws ArgException
	 */
	private void addArgForm(Arg arg, ArgForm form) throws ArgException
	{			
		String identifier = form.getIdentifier(arg);
				
		if (isPresent(identifier))
			throw new ArgDeclarationError("Attempted to add duplicate arg " + identifier);
		
		argTable.put(form.getIdentifier(arg), arg);		
	}
	
	/**
	 * Check to see if the given argument identifier is present in the arg table. 
	 * 
	 * @param identifier
	 * @return
	 */
	private boolean isPresent(String identifier)
	{
		return argTable.get(identifier) != null;
	}	
}
