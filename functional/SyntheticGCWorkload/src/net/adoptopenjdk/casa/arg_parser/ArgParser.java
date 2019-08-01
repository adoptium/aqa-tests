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
 * A parser for command line arguments. 
 * 
 * Example usage: 
 * 
 * try 
 *		{
 *			ArgParser argParser = new ArgParser(true);
 *			
 *			argParser.addArg(new Arg(true, ArgParameterType.STRING, 
 *				new ArgHandler()
 *				{			
 *					public void handle(Arg arg) throws ArgException 
 *					{				
 *						filename = arg.getParameterType().parse(arg.getParameter());							
 *					}
 *				}
 *			, "verbose gc filename"));
 *			
 *			argParser.parse(args);		
 *		} 
 *		catch (ArgException e) 
 *		{			
 *			Event.PARSE_ERROR.issue(e);	
 *		}
 * 
 *  
 */
public class ArgParser 
{	
	private Args args; 
	
	private ArgSet userArgs; 
	
	/**
	 * Creates a new parser with only a -h/--help argument.  
	 * 
	 * @param manditory - User must supply an argument 
	 * @throws ArgException
	 */
	public ArgParser(boolean manditory) throws ArgException
	{
		args = new Args(manditory);
		userArgs = new ArgSet(false);
		args.add(userArgs);
		// Add the help argument. 
		args.add(new Arg("Prints usage information.", "help", 'h', false, null, 
			new ArgHandler()
			{			
				public void handle(Arg arg) throws UsageException 
				{				
					throw new UsageException(ArgParser.this.toString()); 							
				}
			}
		));			
	}	
	
	/**
	 * Adds the given Arg to the userArgs set. 
	 * 
	 * @param arg
	 * @throws ArgException
	 */
	public void addArg(Arg arg) throws ArgException
	{
		userArgs.add(arg);
	}
	
	/**
	 * Parses the given command line arguments, calling handlers for each argument found. 
	 * 
	 * @param argStrings
	 * @throws ArgException
	 */
	public void parse(String[] argStrings) throws ArgException, UsageException 
	{
		
		args.buildArgTable();
		
		for (int i = 0; i < argStrings.length; i++) 
		{
			String argString = argStrings[i];
			
			// Check to see if it's short, long or a string 
			ArgForm form = getArgForm(argString); 
								
			// Parse the cluster into one or more arguments
			Arg[] argCluster = parseArgCluster(argString, form);
											
			// Find arguments that require a parameters in the cluster.  
			Arg[] argsRequiringParameter = findArgsRequiringParameters(argCluster);
				
			// Verify that there are not multiple arguments requiring parameters in the cluster. 
			if (argsRequiringParameter.length > 1)
			{
				throw new ArgParseError(argsRequiringParameter.length + " args in cluster " + argString + " require a parameters.");
			}			
			// Look ahead to find parameters 				 			
			else if (argsRequiringParameter.length == 1) 
			{
				// Grab the 0th and only arg requiring a parameter. 
				Arg arg = argsRequiringParameter[0];
									
				// Check the number of required parameters. 
				int requiredParameters = arg.getNumRequiredParameters(); 
				
				// If the form is UNSTRUCTURED, take the current argString as the first parameter. 
				if (form.equals(ArgForm.UNSTRUCTURED)) 
				{
					arg.setParameter(arg.getNextParameterIndex(), argString);					
					requiredParameters--; 
				}				
				
				// Make sure there are enough things left on the command line to satisfy the number of required parameters. 
				if (argStrings.length <= i + requiredParameters)
				{ 					
					throw new ArgParseError("Argument " + arg.getIdentifier() + " expects " + arg.getNumRequiredParameters() + " parameters");
				}
				
				// Parse the rest of the parameters. 												
				while (requiredParameters > 0)
				{
					String parameter = argStrings[i+1]; 
					
					// Found next argument while trying to satisfy parameter. 
					if (isArg(parameter)) 
					{											
						throw new ArgParseError("Argument " + arg.getIdentifier() + 
								" expects " + arg.getNumRequiredParameters() + 
								(arg.getNumRequiredParameters() == 1?" parameters" :" parameter"));
					}
										
					arg.setParameter(arg.getNextParameterIndex(), parameter);	
					i++;
					requiredParameters--;
				}
			}										
		}		
	
		args.executeHandlers();		
	}
	
	/**
	 * Returns true if the string begins with -
	 * 
	 * @param string
	 * @return
	 */
	private boolean isArg(String string)
	{
		return string.startsWith("-"); 		
	}
	
	/**
	 * Returns true if the string begins with --
	 * 
	 * @param string
	 * @return
	 */
	private boolean isLongArg(String string)
	{
		return string.startsWith("--");			
	}
	
	/**
	 * Gets a sub-array of the given array containing only
	 * arguments which require a parameter.  
	 * 
	 * @param args
	 * @return
	 */
	private Arg[] findArgsRequiringParameters(Arg[] args)
	{			
		ArrayList<Arg> argsRequiringParameter = new ArrayList<Arg>(); 
		
		for (Arg arg : args) 
			if (arg.getNumRequiredParameters() > 0) 
				argsRequiringParameter.add(arg);
		
		return argsRequiringParameter.toArray(new Arg[0]);  
	}
	
	/**
	 * Gets the form of the given argument string. 
	 * 
	 * @param argString
	 * @return
	 */
	private ArgForm getArgForm(String argString)
	{		
		// Starts with --
		if (isLongArg(argString)) 
			return ArgForm.LONG;
		// Starts with -
		else if (isArg(argString)) 			
			return ArgForm.SHORT;					
		// Doesn't start with -
		else 
			return ArgForm.UNSTRUCTURED;	
	}
	
	/**
	 * Parses an argument (-a, --all, string) or cluster of short arguments (-acd) and returns an array of 
	 * the arguments found.  
	 * 
	 * @param argString
	 * @param form
	 * @return
	 * @throws ArgException
	 */	
	private Arg[] parseArgCluster(String argString, ArgForm form) throws ArgException
	{
		Arg[] argCluster = null; 
		
		// 2 -'s in front of arg string. 
		if (form.equals(ArgForm.LONG)) 
		{
			argCluster = new Arg[1];
			Arg arg = args.matchArg(ArgForm.getIdentifier(argString.substring(2)), form);																		
			argCluster[0] = arg;
		}
		// No - in front of arg string, parse as string.  
		else if (form.equals(ArgForm.UNSTRUCTURED)) 
		{
			argCluster = new Arg[1];				
			Arg arg = args.matchArg(ArgForm.getIdentifier(), form);
			argCluster[0] = arg; 
		}
		// 1 - in front of arg string, parse a string of short args.  
		else if (form.equals(ArgForm.SHORT)) 
		{
			argCluster = new Arg[argString.length() - 1];
			
			for (int j = 1; j < argString.length(); j++) 
			{					
				Arg arg = args.matchArg(ArgForm.getIdentifier(argString.charAt(j)), form);								
				
				argCluster[j-1] = arg;
			}
		}
		
		return argCluster; 
	}
	
	/**
	 * Returns usage string. 
	 */
	public String toString()
	{
		return "" + args.toString(); 
	}
}
