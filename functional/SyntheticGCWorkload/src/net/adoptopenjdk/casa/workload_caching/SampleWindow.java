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
package net.adoptopenjdk.casa.workload_caching;

import net.adoptopenjdk.casa.event.EventHandler;

/**
 * Holds a fixed number of samples. Allows the user to add new ones, displacing 
 * the least-recent one. Supports the computation of a sum or an average over the window.    
 *
 */
public class SampleWindow
{
	// The sample window should be smaller than the maximum size of an array by some factor. 
	static final long MAX_NUM_SAMPLES = Integer.MAX_VALUE/8; 
	
	// Don't allow windows with fewer than 2 slots. 
	static final long MIN_NUM_SAMPLES = 2;
	
	/*
	 * The sample window contains all values added to 
	 * the window that are still within the sample
	 * window time.  
	 */
	private final double[] sampleWindow;	

	// The sum of all values that are currently within the window. 
	private double sum; 	
	
	// Number of samples in the window. 
	private int count; 
	
	// The index of the last item in the window. 
	private int index;
	
	/**
	 * 
	 * 
	 * @param sampleWindowTime
	 * @param granularity
	 * @param errorHandler
	 */
	public SampleWindow(double sampleWindowTime, double granularity, EventHandler errorHandler)
	{	
		// Calculate the number of samples we need to store. 
		int numSamples = calculateNumSamples(sampleWindowTime, granularity, errorHandler);
		
		// Create an array for the samples. 
		sampleWindow = new double[numSamples];
				
		index = 0;
		
		sum = 0; 
		
		count = 0; 
	}
	
	/**
	 * Calculates the size of the sample window based on the 
	 * time it is intended to represent and the granularity. Rounds 
	 * the result up and range checks it before returning it as an 
	 * integer.     
	 * 
	 * @param sampleWindowTime
	 * @param granularity
	 * @param errorHandler
	 * @return
	 */
	private int calculateNumSamples(double sampleWindowTime, double granularity, EventHandler errorHandler)
	{
		long numSamplesLong = (long)Math.ceil(sampleWindowTime/granularity);
		
		// The sample window must not be too large. 
		if (numSamplesLong > SampleWindow.MAX_NUM_SAMPLES)		
			errorHandler.issue("Sample window is too large.");			
		
		if (numSamplesLong < SampleWindow.MIN_NUM_SAMPLES)
			errorHandler.issue("Sample window is too small.");
		
		return (int)numSamplesLong;
	}
	
	/**
	 * Circularly increments index within the range of 
	 * the sampleWindow
	 */
	public void incrementIndex()
	{
		index = (index >= sampleWindow.length - 1) ? 0 : index + 1; 
	}
	
	/**
	 * Adds the sample to the window while subtracting 
	 * the least-recently added sample. 
	 * 
	 * @param sample
	 */
	public void addSample(double sample)
	{		
		// Increase the count of samples in the window up until the window is full. 
		if (count < sampleWindow.length)
			count++; 
		
		// Add the new sample to the window sum. 
		sum += sample;		
		
		// Subtract the outgoing sample from the sum. 
		sum -= sampleWindow[index];
		
		// Overwrite the outgoing sample with the new sample. 
		sampleWindow[index] = sample;
		
		// Advance to the next sample. 
		incrementIndex();
	}
	
	/**
	 * Gets the sum of all samples in the window. 
	 * 
	 * @return
	 */
	public double getSum()
	{
		return sum; 
	}
	
	/**
	 * Gets the average of all samples within the window. 
	 * 
	 * @return 
	 */
	public double getAverage()
	{
		return (count > 0) ? sum / (double)count : 0; 
	}
}