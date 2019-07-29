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
 * Represents a possible command line argument. 
 * 
 */
public class Arg extends ArgSet 
{		
	// The long version of the argument, or null for unstructured type
	private String longForm; 
	// The short version of the argument, or 0 for unstructured type. 
	private char shortForm;
	// Called after the whole command line has been parsed. 
	private ArgHandler handler;	
	// For unstructured arguments, this is set at construction time. For others it is set when they are parsed. 
	private ArgForm form; 
	/* Unstructured arguments must have parameters. Long and short forms can 
	 * be parameterless, in which case these are empty arrays. Must not be null. */  
	private String[] parameters;
	// Types of parameters. Will be an empty array if there are no parameters expected. Must not be null. 
	private ArgParameterType[] parameterTypes;
	// Set when the argument has been identified on the command line. 
	private boolean parsed; 
	// A user-supplied description of what the argument does. 
	private String description; 
		
	/**
	 * Constructs an argument representing all unattributed strings on the command line. They must be together.  
	 * 
	 * @param description - Description of argument for help output. 
	 * @param manditory
	 * @param parameterTypes
	 * @param handler
	 * @throws ArgException
	 */
	public Arg(String description, boolean manditory, ArgParameterType[] parameterTypes, ArgHandler handler) throws ArgException
	{
		super(manditory);
		
		parsed = false;
		this.form = ArgForm.UNSTRUCTURED;
		this.description = description; 
		this.longForm = null; 
		this.shortForm = 0;	
		this.handler = handler;
		
		if (parameterTypes == null)
			throw new ArgDeclarationError("unstructured argument requires at least one parameter type.");
		else 
			this.parameterTypes = parameterTypes;

		this.parameters = new String[this.parameterTypes.length];
	}
	
	/**
	 * Constructs an argument with a long and short form. 
	 * 
	 * @param description - Description of argument for help output. 
	 * @param longForm
	 * @param shortForm
	 * @param manditory - Require that this argument be present provided its parent set or argument is present. 
	 * @param parameterType - null for no parameter
	 * @param handler - what to do when the argument is found
	 * @throws ArgException
	 */
	public Arg(String description, String longForm, char shortForm, boolean manditory, ArgParameterType[] parameterTypes, ArgHandler handler) throws ArgException 
	{
		super(manditory);
		
		this.description = description; 
		this.longForm = longForm; 
		this.shortForm = shortForm;
		this.handler = handler;
		// The form is not known pending parsing. 
		this.form = null;
		parsed = false;
		
		if (longForm == null)
			throw new ArgDeclarationError("Null long form");
		
		if (!(longForm).matches("^[A-Za-z][_\\-A-Za-z0-9]+$"))
			throw new ArgDeclarationError("invalid long form argument definition " + longForm);
		
		if (!("" + shortForm).matches("^[A-Za-z0-9]$"))
			throw new ArgDeclarationError("null short form " + shortForm);
		
		if (parameterTypes == null)
			this.parameterTypes = new ArgParameterType[0]; 
		else 
			this.parameterTypes = parameterTypes;
					
		this.parameters = new String[this.parameterTypes.length];			
	}
	
	/**
	 * Is this argument present and are its mandatory children present? 
	 */
	protected boolean isSatisfied() 
	{
		return (isParsed() && (!isManditory() || super.isSatisfied()));				
	}
	
	/**
	 * Is this argument a set of strings on the command line without a formal prefix? 
	 * 
	 * @return
	 */
	protected boolean isUnstructured()
	{
		if (form == null)
			return false; 
		
		return form.equals(ArgForm.UNSTRUCTURED);
	}
		
	/**
	 * Has this argument been parsed (found) 
	 * 
	 * @return
	 */
	public boolean isParsed()
	{
		if (isUnstructured())
			return parsed && (getNumParsedParameters() == getNumRequiredParameters());
		else 
			return parsed; 
	}
	
	/**
	 * Returns true if this argument has not yet been parsed or can 
	 * still receive additional parameters.
	 * 
	 *  @return true if the argument may be pared or may receive additional parameters. False if it has already been fully parsed. 
	 */
	public boolean canBeParsed()
	{			
		boolean hasStringsParameter = false; 	
		
		for (ArgParameterType type : parameterTypes)
			if (type.equals(ArgParameterType.STRINGS))
				hasStringsParameter = true;
				
		return !isParsed() || hasStringsParameter; 
	}	
	
	/**
	 * Mark this argument as parsed. 
	 * 
	 * @throws ArgException
	 */
	protected void setParsed() throws ArgException
	{
		// Make sure we haven't already parsed the argument. 
		if (parsed)
			throw new ArgParseError("argument already parsed");
		
		// Set the parsed flag. 
		parsed = true;
	}
	
	/**
	 * Mark this argument as parsed from the given form. 
	 * 
	 * @param form
	 * @throws ArgException
	 */
	protected void setParsed(ArgForm form) throws ArgException
	{
		// Set the parsed flag. 
		setParsed(); 
		
		// If the form is not null, we cannot set it again. 
		if (this.form != null)
			throw new ArgParseError("form already set.");
		
		// Make sure the parser has not passed in UNSTRUCTURED
		if (form.equals(ArgForm.UNSTRUCTURED))
			throw new ArgParseError("tried to set the form to unstructured.");
		
		this.form = form;
	}
	
	/**
	 * Gets the long form version of this argument
	 * 
	 * @return
	 * @throws ArgException
	 */
	protected String getLongForm() throws ArgException
	{
		// If the argument is unstructured, it doesn't have a long form 
		if (isUnstructured())
			throw new ArgDeclarationError("unstructured arguments have no long form.");
		
		return longForm;
	}
	
	/**
	 * Gets the short form version of this argument. 
	 * 
	 * @return
	 * @throws ArgException
	 */
	protected char getShortForm() throws ArgException
	{
		// If the argument is unstructured, it doesn't have a short form. 
		if (isUnstructured())
			throw new ArgDeclarationError("unstructured arguments have no short form.");
		
		return shortForm;
	}
	
	/**
	 * If the argument is long or short: 
	 * - Gets the form of this argument parsed from the command line 
	 * 
	 * If it is unstructured, it may or may not have been parsed yet.  
	 * 
	 * @return
	 * @throws ArgException
	 */
	protected ArgForm getForm() throws ArgException
	{
		// Make sure the argument has been parsed if it is short or long form. 
		if (form == null)
			throw new ArgDeclarationError("tried to get null form.");
		
		return form; 
	}
	
	/**
	 * Gets the number of parameters which have been set (parsed and added) so far. 
	 * 
	 * @return
	 */
	protected int getNumParsedParameters()
	{		 	
		// Count non-null parameters. 
		int count = 0; 				
		for (String parameter : parameters)
			if (parameter != null)
				count++;				
		
		return count; 
	}
	
	/**
	 * Gets the index of the next unset parameter. Doesn't do any range-checking. 
	 * 
	 * Use getNumRequiredParameters() to check the result before writing to the 
	 * returned index.  
	 * 
	 * @return
	 */
	protected int getNextParameterIndex()
	{		
		// Count parsed parameters up to the first unparsed one. 		
		int count = 0; 
		for (int i = 0; i < parameters.length; i++)
		{
			String parameter = parameters[i];
			
			// STRINGS parameters are perpetually unparsed. Return nothing past this index. 
			if (parameterTypes[i].equals(ArgParameterType.STRINGS))
				return count; 
			
			// The count of parsed parameters is also the index of the first unparsed one.
			if (parameter == null)
				return count;
			
			count++;
		}
				
		return count; 
	}
	
	/**
	 * Sets the parsed parameter, an array of strings. 
	 * 
	 * @param parameter
	 * @throws ArgException
	 */
	protected void setParameter(int i, String parameter) throws ArgException
	{ 
		// The index is out of range. 
		if (!isParameterIndexValid(i))
		{
			throw new ArgParseError("parameter index " + i + " is out of range");
		}	
		// Parameter is already set. 
		else if (isParameterSet(i))
		{	
			// If it's a strings parameter, it can be perpetually appended to. 
			if (parameterTypes[i].equals(ArgParameterType.STRINGS))
				parameters[i] = parameters[i] + " " + parameter; 
			// All other parameter types can't be set multiple times. 
			else 
				throw new ArgParseError("parameter " + i + "  for argument " + toString() + " already set.");
		}
		// The parameter is not set. Just set it. 
		else 
		{
			this.parameters[i] = parameter;
		}
	}
	
	/**
	 * Gets the parsed parameter. 
	 * 
	 * @return
	 * @throws ArgException
	 */
	public String getParameter(int i) throws ArgException
	{
		// The parameter index is not valid. 
		if (!isParameterIndexValid(i))
			throw new ArgDeclarationError("parameter " + i + " is not a valid parameter.");
		// The parameter has not yet been set.  
		if (!isParameterSet(i))
			throw new ArgDeclarationError("parameter " + i + " is not set.");
		else 
			return parameters[i]; 
	}
	
	/**
	 * Gets the expected parameter type. Throws an exception if the 
	 * index is out of range or the parameter is not set.  
	 * 
	 * @return
	 * @throws ArgException
	 */
	public ArgParameterType getParameterType(int i) throws ArgException
	{
		if (!isParameterIndexValid(i))
			throw new ArgDeclarationError("parameter " + i + " is a valid parameter.");
		else 
			return parameterTypes[i]; 
	}
		
	/**
	 * Gets the identifier for this argument (as found on the command line) 
	 * 
	 * @return
	 * @throws ArgException
	 */
	protected String getIdentifier() throws ArgException
	{		
		return getForm().getIdentifier(this);
	}
	
	

	/**
	 * Gets the number of parameters that must be present after this argument. 
	 * 
	 * @return
	 */
	protected int getNumRequiredParameters()
	{
		return parameterTypes.length;
	}
	
	/**
	 * Executes handlers on child args and sets followed by this 
	 * argument's handler. 
	 */
	protected void executeHandlers() throws ArgException, UsageException
	{
		if (isSatisfied()) {
			super.executeHandlers(); 
			
			handler.handle(this);
		}
	}
	
	/**
	 * Does a range check on the parameter index. 
	 * 
	 * @param i
	 * @return
	 */
	private boolean isParameterIndexValid(int i)
	{
		return i >= 0 && i < parameters.length && i < parameterTypes.length && parameterTypes[i] != null; 
	}
	
	/**
	 * 
	 * @param i
	 * @return
	 */
	private boolean isParameterSet(int i)
	{
		return isParameterIndexValid(i) && parameters[i] != null; 
	}
	
	/**
	 * Produces a string representing this parameter. The string includes parsed 
	 * details if the parameter has been parsed. 
	 * 
	 */
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
					
		if (isUnstructured()) 
		{												
			stringBuilder.append(" <");
			String separator = "";
			for (ArgParameterType type : parameterTypes)
			{
				stringBuilder.append(separator + type.toString());
				separator = " ";
			}
			stringBuilder.append(">");
		}
		else 
		{			
			try 
			{
				stringBuilder.append(ArgForm.LONG.getIdentifier(this) + "|" + ArgForm.SHORT.getIdentifier(this));
				
				if (getNumRequiredParameters() > 0) 
				{
					stringBuilder.append(" <");
					String separator = "";
					for (ArgParameterType type : parameterTypes)
					{
						stringBuilder.append(separator + type.toString());
						separator = " ";
					}
					stringBuilder.append(">");
				}										
			} 
			catch (ArgException e1) 
			{
				stringBuilder.append("{ERROR}");
			}									
			
		}
		
		stringBuilder.append("\n\t" + description);
		
		stringBuilder.append("\n");
		
		stringBuilder.append(super.toString());
						
		return stringBuilder.toString(); 
	}

	
}