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
package net.adoptopenjdk.casa.util.format;

import net.adoptopenjdk.casa.util.Utilities;

/**
 * A simple generic ASCII progress bar. 
 * 
 *  
 */
public class ProgressBar 
{
	// Show a percent figure to the right. 
	private final boolean SHOW_PERCENT; 
	
	// Show the time that's passed/total time? 
	private final boolean SHOW_ELAPSED_TIME;
	
	// Print each update on a new line? (ugly) 
	private final boolean USE_NEWLINES;
	
	// Number of segments in the completed progress bar. 
	private final int LENGTH; 
	
	// Value represented by the completed bar. 
	private double scale; 
	
	// Time the process started 
	private final double START_TIME; 
	
	/**
	 * 
	 * 
	 * 
	 * @param startTime - the time represented by 0, in seconds. 
	 * @param length - the number of charactars representing a full bar 
	 * @param scale - the value represented by a full bar
	 * @param showPercent
	 * @param showElapsedTime 
	 * @param useNewlines
	 */
	public ProgressBar(double startTime, int length, double scale, boolean showPercent, boolean showElapsedTime, boolean useNewlines)
	{
		this.SHOW_PERCENT = showPercent; 
		this.SHOW_ELAPSED_TIME = showElapsedTime;
		this.USE_NEWLINES = useNewlines; 
		
		this.LENGTH = length; 
		this.scale = scale;
		this.START_TIME = startTime; 
	}
	
	/**
	 * Creates a new progress bar that shows percent and elapsed time. 
	 * 
	 * @param startTime
	 * @param length
	 * @param scale
	 */
	public ProgressBar(double startTime, int length, double scale)
	{
		this.SHOW_PERCENT = true; 
		this.SHOW_ELAPSED_TIME = true;
		this.USE_NEWLINES = false; 
		
		this.LENGTH = length; 
		this.scale = scale;
		this.START_TIME = startTime; 
	}
	
	/**
	 * Updates the scale of the progress bar to reflect a new 
	 * expected scale. (eg: the estimated duration has changed)  
	 * 
	 * @param scale
	 */
	public void setScale(double scale)
	{
		this.scale = scale; 
	}
	
	/**
	 * Update the progress bar to the given value/scale and returns 
	 * a new string. For standard console output, this string should 
	 * be printed without being followed by a newline.   
	 * 
	 * @param newValue
	 * @return
	 */
	public String update(final double newValue)
	{					
		final double elapsedTime = Utilities.getTime() - START_TIME;
		
		final double proportion = newValue/scale; 		
		
		final int completedSegments = proportion <= 1
				? (int)(proportion * (double)LENGTH) 
				: LENGTH;
		final double estimatedDuration = proportion <= 1
				? elapsedTime * (1/proportion)
				: scale; 
				
		StringBuilder builder = new StringBuilder(); 
				
		builder.append(USE_NEWLINES?"":"\r"); 

		builder.append("|");
		
		if (SHOW_ELAPSED_TIME)
			builder.append(" " + Utilities.formatTime(elapsedTime) + "/" + Utilities.formatTime(estimatedDuration) + " |");
		
		for (int j = 0; j < completedSegments-1; j++)
			builder.append("=");
		
		builder.append(">");
		
		for (int j = completedSegments; j < LENGTH; j++)
			builder.append(" ");
		
		builder.append("|");
		
		if (SHOW_PERCENT)
			builder.append(" " + Math.round((completedSegments/(double)LENGTH)  * 100) + "%");
		
		builder.append(USE_NEWLINES?"\n":"     ");
		
		return builder.toString(); 
	}
}
