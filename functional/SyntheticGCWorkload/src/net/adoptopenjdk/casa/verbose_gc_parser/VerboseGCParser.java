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
package net.adoptopenjdk.casa.verbose_gc_parser;

import java.io.IOException;
import java.util.ArrayList;

import net.adoptopenjdk.casa.arg_parser.Arg;
import net.adoptopenjdk.casa.arg_parser.ArgException;
import net.adoptopenjdk.casa.arg_parser.ArgHandler;
import net.adoptopenjdk.casa.arg_parser.ArgParameterType;
import net.adoptopenjdk.casa.arg_parser.ArgParser;

/**
 * Verbose parser application which offers a variety of features for parsing verbose GC files. 
 * 
 *  
 *
 */
public class VerboseGCParser 
{
	 
	
	public static void main(String args[])
	{	
		try 
		{							
			new VerboseGCParser(args);	
		} 
		catch (IOException e) 
		{
			Event.FATAL_ERROR.issue(e);
		} 		
	}
	
	// Options from the command line 
	private String[] filenames;		 
	private boolean printSummary; 	
	private boolean printTimeline; 
	private boolean showOptions;
	
	/**
	 * Parses verbose GC according to the given arguments. 
	 * 
	 * @param args
	 * @throws IOException
	 */
	public VerboseGCParser(String args[]) throws IOException
	{			
		filenames   = null;		 	
		printSummary = false; 
		printTimeline    = false; 
		showOptions = false; 	
		
		// Parse the command line. 
		parseArgs(args); 
		
		parseMultipleFiles(filenames);		
	}
		
	/**
	 * Parses an array of files 
	 * 
	 * @param filenames
	 */
	public void parseMultipleFiles(String[] filenames)
	{						
		ArrayList<VerboseSummary> summaries = new ArrayList<VerboseSummary> (); 
		
		System.out.println(VerboseEvent.getLegendString());
		
		VerboseSummary summary = null;
		
		// Print timelines, command lines, and pre-process summaries. 
		for (String filename : filenames)
		{		
			if (printTimeline) 
				System.out.println(filename + "\n" + new VerboseTimeline(filename));
						 		
			if (printSummary || showOptions) 
			{
				summary = new VerboseSummary(filename, summary); // Accumulate keys 
				summaries.add(summary);
				
				if (showOptions)
					System.out.println(summary.getCommandLine());						
			}
		}
			
		// Print all summaries in a single table. 
		if (summary != null)
		{						
			// Grab accumulated keys from last summary. 
			ArrayList<String> keys = summary.getKeys(); 
																		
			System.out.println(summary.getHeader()); 
			
			for (VerboseSummary thisSummary : summaries) 					
				System.out.println(thisSummary.toString(keys));  									
		}				
	}
	
	/**
	 * Parses the given command line arguments 
	 * 
	 * @param args
	 */
	private void parseArgs(String args[])
	{
		try 
		{
			ArgParser argParser = new ArgParser(true);
			
			Arg filenameArg = new Arg("Supply one or more verbose GC filenames", true, new ArgParameterType[] {ArgParameterType.STRINGS}, 
				new ArgHandler()
				{			
					public void handle(Arg arg) throws ArgException 
					{				
						filenames = arg.getParameterType(0).parse(arg.getParameter(0));							
					}
				}
			);
					
			filenameArg.add(
				new Arg("Produce a summary of each verbose file. ", "summary", 's', false, null, 
					new ArgHandler()
					{			
						public void handle(Arg arg) throws ArgException 
						{								
							printSummary = true; 							
						}
					}
				)
			);
			
			filenameArg.add(
				new Arg("Show a timeline for each verbose file", "timeline", 't', false, null, 
					new ArgHandler()
					{			
						public void handle(Arg arg) throws ArgException 
						{								
							printTimeline = true; 							
						}
					}
				)
			);
			
			filenameArg.add(
				new Arg("Show options supplied to the Java VM", "vm_options", 'o', false, null, 
					new ArgHandler()
					{			
						public void handle(Arg arg) throws ArgException 
						{								
							showOptions = true; 							
						}
					}
				)
			);
			
			argParser.addArg(filenameArg);
			
			argParser.parse(args);		
		} 
		catch (ArgException e) 
		{			
			Event.BAD_ARGUMENTS.issue(e);	
		}	
	}	
}