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

import java.io.File;
import java.io.IOException;

import net.adoptopenjdk.casa.arg_parser.Arg;
import net.adoptopenjdk.casa.arg_parser.ArgException;
import net.adoptopenjdk.casa.arg_parser.ArgHandler;
import net.adoptopenjdk.casa.arg_parser.ArgParameterType;
import net.adoptopenjdk.casa.arg_parser.ArgParser;

/**
 * Verbose parser application which offers a variety of features for parsing verbose GC files. 
 * 
 *  
 */
public class VerboseGCComparator 
{
	// Options from the command line 
	private String filename1;
	private String filename2;
	private String[] events;
	private int[] detectionThresholds; 
	private double[] passThresholds; 	
	
	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String args[])
	{
		try 
		{
			new VerboseGCComparator(args);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses verbose GC according to the given arguments. 
	 * 
	 * @param args
	 * @throws IOException
	 */
	public VerboseGCComparator(String args[]) throws IOException
	{				
		// Parse the command line arguments . 
		parseArgs(args); 
				
		// Validate arguments 
		
		// Expect 2 filenames 
		if (filename1 == null || filename2 == null)
			Event.FATAL_ERROR.issue("Must supply 2 input files");
		
		// Expect some events to monitor 
		if (events == null || events.length <= 0)
			Event.FATAL_ERROR.issue("Must supply events to monitor");
		
		if (passThresholds == null)
			Event.FATAL_ERROR.issue("Must supply pass thresholds");
		
		if (detectionThresholds == null)		
			detectionThresholds = new int[passThresholds.length]; // Java arrays init to 0.
				
		if (events.length != detectionThresholds.length || events.length != passThresholds.length)
			Event.FATAL_ERROR.issue("Must supply the same number of thresholds (detection or pass) as events.");
					
		if (!new File(filename1).isFile())
			Event.FATAL_ERROR.issue("Original file " + filename1 + " does not exit.");
		
		if (!new File(filename2).isFile())
			Event.FATAL_ERROR.issue("Auto file " + filename2 + " does not exit.");
				
		// Parse verbose files 
		VerboseSummary original = new VerboseSummary(filename1);		
		VerboseSummary slackAuto = new VerboseSummary(filename2, original);
						
		System.out.println(VerboseEvent.getLegendString());
		System.out.println("---------------------------------------------------------");
		System.out.println(filename1 + ": "); 
		System.out.println(new VerboseTimeline(filename1));
		System.out.println(original);
		System.out.println("---------------------------------------------------------"); 
		System.out.println(filename2 + ": "); 
		System.out.println(new VerboseTimeline(filename2));
		System.out.println(slackAuto); 
		System.out.println("---------------------------------------------------------");
		
		System.out.println("Test results: ");		
		boolean pass = true; 		
		for(int i = 0; i < events.length; i++)
		{
			String eventSymbol = events[i];			
			boolean eventPass = true; 
			
			if 
			(
				(slackAuto.getCounterValue(eventSymbol) > detectionThresholds[i]) 
				&& (slackAuto.getCounterValue(eventSymbol) > original.getCounterValue(eventSymbol) * passThresholds[i])
			)
			{
				pass = eventPass = false; 										
			}
			
			VerboseEvent event = VerboseEvent.getBySymbol(eventSymbol);
			double ratio = ((double)slackAuto.getCounterValue(eventSymbol)/original.getCounterValue(eventSymbol));			
//			System.out.println(event.getDescription() + " (" + eventSymbol + "): " + (eventPass ? "passed" : "failed"));
			System.out.println(event.getDescription() + " (" + eventSymbol + "): before "+ original.getCounterValue(eventSymbol) + ", after " + slackAuto.getCounterValue(eventSymbol) +", ratio " + ratio + ((ratio > passThresholds[i])? " > " : " <= ") + passThresholds[i] + " --> " + (eventPass ? "passed" : "failed"));
		}		
		System.out.println("=========================================================");
					
		if (!pass)
		{			
			Event.FAILURE.issue(); 
		}		
		else 
		{			
			Event.SUCCESS.issue(); 
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
			
			Arg filenameArg = new Arg(
				"Two files to compare", 
				true, 
				new ArgParameterType[] {ArgParameterType.STRING,ArgParameterType.STRING}, 
				new ArgHandler()
				{			
					public void handle(Arg arg) throws ArgException 
					{				
						filename1 = arg.getParameterType(0).parse(arg.getParameter(0));
						filename2 = arg.getParameterType(1).parse(arg.getParameter(1));
					}
				}
			);

			
			Arg eventsArg = new Arg(
				"Supply a comma separated list of events to monitor (eg: -e x,p)", 
				"events",
				'e',
				true, 
				new ArgParameterType[] {ArgParameterType.STRING_ARRAY}, 
				new ArgHandler()
				{			
					public void handle(Arg arg) throws ArgException 
					{				
						events = arg.getParameterType(0).parse(arg.getParameter(0));						
					}
				}
			);
			
			
			eventsArg.add(
				new Arg(
					"Supply minimum values required to regard event as present (eg: -d 1,1)", 
					"detection_threshold",
					'd',
					false, 
					new ArgParameterType[] {ArgParameterType.INTEGER_ARRAY}, 
					new ArgHandler()
					{			
						public void handle(Arg arg) throws ArgException 
						{				
							detectionThresholds = arg.getParameterType(0).parse(arg.getParameter(0));						
						}
					}
				)
			);
			
			eventsArg.add(
					new Arg(
						"Supply maximum proportion of occurrences of event in file 1 to allow in file 2 (eg: -p 0.20,0.20)", 
						"pass_threshold",
						'p',
						true, 
						new ArgParameterType[] {ArgParameterType.DOUBLE_ARRAY}, 
						new ArgHandler()
						{			
							public void handle(Arg arg) throws ArgException 
							{				
								passThresholds = arg.getParameterType(0).parse(arg.getParameter(0));						
							}
						}
					)
				);
			
			
			filenameArg.add(eventsArg);								
			argParser.addArg(filenameArg);
			
			argParser.parse(args);		
		} 
		catch (ArgException e) 
		{			
			Event.BAD_ARGUMENTS.issue(e);	
		}	
	}	
}