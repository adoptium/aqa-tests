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

import java.util.ArrayList;

/**
 * A set of arguments which are treated as a group. 
 * 
 *  
 */
public class ArgSet 
{
	private boolean manditory; 
	private ArrayList<Arg> args;
	private ArrayList<ArgSet> sets;	
	
	/**
	 * 
	 * 
	 * @param manditory
	 */
	public ArgSet(boolean manditory)
	{
		args = new ArrayList<Arg>(); 
		sets = new ArrayList<ArgSet>();
		this.manditory = manditory; 
	}

	/**
	 * Add an argument to the set
	 * 
	 * @param arg
	 */
	public void add(Arg arg) 
	{
		args.add(arg);
	}
	
	/**
	 * Add a subset to the set. 
	 * 
	 * @param set
	 */
	public void add(ArgSet set) 
	{
		sets.add(set);
	}

	/**
	 * Checks the manditory flag on this set. 
	 * 
	 * @return
	 */
	protected boolean isManditory() 
	{
		return manditory; 
	}	
	
	/**
	 * Gets the subsets. 
	 * 
	 * @return
	 */
	protected ArrayList<ArgSet> getSets()
	{
		return sets; 
	}
		
	/**
	 * Gets the arguments. 
	 * 
	 * @return
	 */
	protected ArrayList<Arg> getArgs()
	{
		return args; 
	}
	
	/**
	 * Executes all arg handlers and calls executeHandlers() on all child sets. 
	 * 
	 * @throws ArgException
	 */
	protected void executeHandlers() throws ArgException
	{	
		// Call handlers. 
		for (Arg arg : args) 
			arg.executeHandlers(); 									
		
		for (ArgSet set : sets) 
			set.executeHandlers(); 
	}

	/** 
	 * This set is satisfied iff: 
	 * - All manditory arguments are satisfied
	 * - All manditory sets are satisfied
	 * - If this set is not manditory or is present.   
	 * 
	 * @return
	 */
	protected boolean isSatisfied() 
	{	
		// If a manditory argument is not satisfied, this set is not satisfied. 
		for (Arg arg : args) {				
			if (arg.isManditory() && !arg.isSatisfied()) 				
				return false; 		
		}					
		
		// If a manditory set is not satisfied, this set is not satisfied. 
		for (ArgSet set : sets) {				
			if (set.isManditory() && !set.isSatisfied()) 				
				return false; 		
		}
			
		// If this set is manditory and not present, this set is not satisfied. 
		if (isManditory() && !isPresent())
			return false; 
		else 
			return true; 		
	}
	
	/**
	 * Checks to see if one of the args in the set is satisfied 
	 * or if one of the subsets is satisfied. 
	 * 
	 * @return
	 */
	private boolean isPresent()
	{
		boolean argPresent = false;		
		// If the set is empty, it is present. 
		if (args.size() == 0) 
		{
			argPresent = true;
		}
		// If any of its arguments are satisfied, it is present. 
		else 
		{			 		
			for (Arg arg : args) 
			{				
				if (arg.isSatisfied()) 
				{				
					argPresent = true; 
				}			
			}
		}
			
		boolean setSatisfied = false; 
		// If there are no child sets, a set is satisfied. 
		if (sets.size() == 0) 
		{
			setSatisfied = true; 
		}
		// If one set is satisfied, a set is satisfied. 
		else 
		{
			for (ArgSet set : sets) 
			{				
				if (set.isSatisfied()) 
				{				
					setSatisfied = true; 
				}			
			}
		}
					
		// If an argument is present or a set is satisfied, this set is present. 
		return argPresent || setSatisfied; 
	}
	
	/**
	 * Produces a human readable description of each argument in the set and 
	 * its subsets. 
	 */
	public String toString()
	{
		if (args.size() == 0 && sets.size() == 0)
			return ""; 
			
		StringBuilder stringBuilder = new StringBuilder(); 
		
		// Append arguments 
		for (Arg arg : args) 
		{
			stringBuilder.append(arg.toString());			
		}						
					
		// Append their child sets. 
		for (ArgSet set : sets) 
		{			
			stringBuilder.append(set.toString());			
		}

		return stringBuilder.toString(); 
	}
}
