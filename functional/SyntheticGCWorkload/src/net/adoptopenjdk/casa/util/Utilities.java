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
package net.adoptopenjdk.casa.util;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Random;

/**
 * General utility functions used throughout the benchmark 
 * 
 *  
 */
public class Utilities 
{		
	protected static final Random randomInstance = new Random();
	
	/**
	 * Rounds the given double to the given number of decimal places. 
	 * 
	 * @param number
	 * @param decimalPlaces
	 * @return
	 */
	public static double roundDouble(double number, int decimalPlaces)
	{
		return Math.round((number * Math.pow(10,decimalPlaces)))/Math.pow(10.0,decimalPlaces);
	}
	
	/**
	 * The same functionality as formatDataSize(double), but accepts a long.   
	 * 
	 * @param bytes
	 * @return
	 */
	public static String formatDataSize(long bytes)
	{
		return formatDataSize((double) bytes); 
	}
	
	/**
	 * Formats the given proportion as a percent suffixed with the "%" sign. 
	 * 
	 * @param proportion - a number representing a proportion or ratio
	 * @return the proportion formatted as a percent, including the sign. 
	 */
	public static String formatProportionAsPercent(double proportion, int significantDigits)
	{	
		 
		return  String.format("%." + significantDigits + "g", proportion * (double)100.0) + "%";
	}
	
	
	public static String formatTime(double time)
	{
		return formatTime(time, 3);
	}
	
	
	/**
	 * Formats the given time (in seconds) using the most appropriate 
	 * units and returns the formatted string including the unit symbol. 
	 * 
	 * @param time - time in seconds, nanosecond precision. 
	 * @return a string with a number rounded to three significant figures, suffixed by a time unit. No unit will be appended if the time is 0.  
	 */
	public static String formatTime(double time, int significantFigures)
	{		
		// Pick a unit that fits the time. 
		if (time < 1E-9)
			return "0"; 
		else if (time < 1E-6)
			return String.format("%." + significantFigures + "g", time*1E9) + "ns";
		else if (time < 1E-3)
			return String.format("%." + significantFigures + "g", time*1E6) + "us";		
		else if (time < 1)
			return String.format("%." + significantFigures + "g", time*1E3) + "ms";
		else if (time < 60)
			return String.format("%." + significantFigures + "g", time) + "s"; 
		else if (time/60 < 60)
			return String.format("%." + significantFigures + "g", time/60) + "m";
		else if (time/60/60 < 24)
			return String.format("%." + significantFigures + "g", time/60/60) + "h";			
		else 
			return String.format("%." + significantFigures + "g", time/60/60/24) + "d";
	}
	
	
	
	/**
	 * Formats the given data size (in bytes) in an appropriate binary unit and 
	 * returns it as a string with the proper suffix. 
	 * 
	 * @param bytes - data size to be formatted, in bytes.  
	 */
	public static String formatDataSize(double bytes)
	{
		StringBuilder builder = new StringBuilder();
		
		Double size = bytes;
		
		/*
		 * Keep dividing by 1024 until the resulting number is under 
		 * 1000; 1000 rather than 1024 avoids scientific notation.
		 * 
		 * The number of divisions is capped at 9. The resulting 
		 * number will be a round binary data unit.  
		 */
		int divisions = 0;				
		while (size >= 1000 && divisions < 9) 
		{
			size /= 1024; 
			divisions++; 
		}
		
		builder.append(String.format("%.3g", size));  
		
		/*
		 * Choose the unit based on the number of times we divided
		 * by 1024.  
		 */
		switch (divisions) {
			case 0: break; 
			case 1: builder.append('k'); break; 
			case 2: builder.append('M'); break; 
			case 3: builder.append('G'); break;
			case 4: builder.append('T'); break;
			case 5: builder.append('P'); break;
			case 6: builder.append('E'); break;
			case 7: builder.append('Z'); break;
			case 8: builder.append('Y'); break;
			default: return "[format error]"; 
		}
		
		builder.append('B');
		
		return builder.toString(); 
	}

	/**
	 * Returns the formatted data size followed by "/s"
	 * 
	 * @param dataRate
	 * @return
	 */
	public static String formatDataRate(double dataRate) {
		return formatDataSize(dataRate) + "/s";
	}
	
	/**
	 * Gets the current time in seconds as a double down to the nanosecond. This 
	 * is not the system time and can only be used to compute relative times.
	 *
	 * @return current time in seconds to the precision of System.nanoTime()
	 */
	public static double getTime()
	{
		return (System.nanoTime() / 1E9);
	}
	
	/**
	 * Gets the current absolute system time in seconds, accurate
	 * to the millisecond. 
	 * 
	 * @return current time in seconds to the precision of System.currentTimeMillis()
	 */
	public static double getAbsoluteTime()
	{
		return (System.currentTimeMillis() / 1E3); 
	}
	
	/**
	 * Sleep for the given amount of time, rounded to the 
	 * nearest nanosecond.
	 * 
	 * Throws an InterruptedException if interrupted.
	 * 
	 * If the sleepTime is less than or equal to zero, this 
	 * method returns immediately.  
	 * 
	 * @param sleepTime - sleep time in seconds. 
	 * @throws InterruptedException - thrown if Thread.sleep is interrupted. 
	 */
	public static void idle(double sleepTime) throws InterruptedException
	{ 	
		if (sleepTime > 0) 
		{
			// Get the number of whole milliseconds 
			long millis = getMillis(sleepTime);
						
			// Get the nanoseconds from the remainder. 
			int nanos  = getNanos(sleepTime, millis);					
			
			// Both numbers must be nonnegative and one of them must be nonzero. 
			if (millis >= 0 && nanos >= 0 && (millis > 0 || nanos > 0))			
				Thread.sleep(millis, nanos);
		}
	}
	
	/**
	 * Upon acquiring the monitor, waits the given period of time 
	 * OR until monitor.notify() is called OR until interrupted. 
	 * 
	 * Returns immediately if the time is not greater than zero. 	
	 * 
	 * @param monitor
	 * @param time
	 * @throws InterruptedException
	 */
	public static void wait(Object monitor, double time) throws InterruptedException
	{
		if (time > 0) 
		{
			// Get the number of whole milliseconds 
			long millis = getMillis(time);
						
			// Get the nanoseconds from the remainder. 
			int nanos  = getNanos(time, millis);					
			
			// Both numbers must be nonnegative and one of them must be nonzero. 
			if(millis >= 0 && nanos >= 0 && (millis > 0 || nanos > 0))
			{
				synchronized(monitor)
				{
					monitor.wait(millis, nanos);
				}
			}
		}
	}
	
	/**
	 * Returns the rounded number of nanoseconds remaining when all whole 
	 * milliseconds are subtracted off. 
	 * 
	 * @param time
	 * @return
	 */
	public static int getNanos(double time)
	{
		return getNanos(time, getMillis(time));
	}
	
	/**
	 * Returns the rounded number of nanoseconds when the given number 
	 * of milliseconds are subtracted. The result will be truncated to the 
	 * size of an integer after rounding. 
	 * 
	 * @param time
	 * @param millis
	 * @return
	 */
	public static int getNanos(double time, long millis)
	{
		// Get the nanoseconds from the remainder after the millis have been removed. 
		return (int)Math.round((time * 1E9) - (millis * 1E6));	
	}
	
	/**
	 * Returns the number of whole milliseconds composing the 
	 * time. 
	 * 
	 * @param time
	 * @return
	 */
	public static long getMillis(double time)
	{
		// Get the number of whole milliseconds. 
		return (long)Math.floor(time * 1E3);
	}
	
	/**
	 * Generates a Gaussian-distributed random data size according to the given 
	 * variance and radius, between the given min and max and conforming to the
	 * supplied alignment.   
	 * 
	 * @param mean
	 * @param variance
	 * @param radius
	 * @param min
	 * @param max
	 * @param alignment
	 * @return
	 */
	public static long generateGaussianDataSize(long mean, double variance, long radius, long min, long max, long alignment)
	{	
		if (variance == 0 || radius == 0)
			return mean; 
		
		do 
		{
			// Generate a possible size. 
			long candidateSize = Math.round((randomInstance.nextGaussian() * variance + (double)mean)/(double)alignment) * alignment;
			
			// If it fits the radius, use it. If not, try again. 
			if (
				 
				   candidateSize >= min
				&& candidateSize <= max
			)
			{
				return candidateSize; 
			}
		} while (true);
	}
	
	/**
	 * Generates a random data size between the given min and max and 
	 * conforming to the given alignment. 
	 * 
	 * @param min
	 * @param max
	 * @param alignment
	 * @return
	 */
	public static long generateRandomDataSize(long min, long max, long alignment)
	{	
		// Check that the range actually includes room for randomness. If it doesn't, return the min. 
		if (max - min <= 0)
			return min; 
		
		do 
		{
			// Generate a size. 
			long candidateSize = Math.round((randomInstance.nextDouble() * (max - min) + (double)min)/(double)alignment) * alignment;
			
			// If it fits within the min and max, use it. Otherwise, reject it and try again. 
			if (
				 
				   candidateSize >= min
				&& candidateSize <= max
			)
			{
				return candidateSize; 
			}
		} while (true);
	}

	/**
	 * Generates a flat random time around the given mean out to the givne radius and greater than zero. 
	 * 
	 * @param mean
	 * @param radius
	 * @return
	 */
	public static double generateRandomTime(double mean, double radius)
	{	
		if (radius == 0)
			return mean; 
		
		do 
		{
			// Generate a lifespan 
			double candidateLifespan = (randomInstance.nextDouble() * (radius * 2)) + (mean - radius);
			
			// If it fits in the radius, return it. Otherwise, try again. 
			if (
				   candidateLifespan > 0 
				&& candidateLifespan >= mean - radius
				&& candidateLifespan <= mean + radius
			)
			{
				return candidateLifespan; 
			}
		} while (true);
	}
	
	/**
	 * Generates a Gaussian-distributed random time around the given mean accoring to the given 
	 * variance and out to the given radius. If the radius intersects zero, the 
	 * distribution will no longer be normal or centered around the mean.   
	 * 
	 * @param mean
	 * @param variance
	 * @param radius
	 * @return
	 */
	public static double generateGaussianTime(double mean, double variance, double radius)
	{	
		if (variance == 0 || radius == 0)
			return mean; 
		
		do 
		{
			// Generate a lifespan 
			double candidateLifespan = randomInstance.nextGaussian() * variance + mean;

			// If it fits in the radius, return it. Otherwise, try again. 
			if (
				   candidateLifespan > 0 
				&& candidateLifespan >= mean - radius
				&& candidateLifespan <= mean + radius
			)
			{
				return candidateLifespan; 
			}
		} while (true);
	}
	
	/**
	 * Gets the VM's command line arguments 
	 */
	public static String printVMArgs()
	{
		//Print out JVM arguments for reference 
		List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();		    
		return inputArguments.toString();    
	}

	
}
