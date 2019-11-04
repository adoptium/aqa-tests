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
package net.adoptopenjdk.casa.data_structures;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An atomic double implementation based on AtomicLong 
 * 
 *  
 *
 */
public class AtomicDouble 
{
	private volatile AtomicLong data; 
	
	/**
	 * Creates a new instance with the given initial value. 
	 * 
	 * @param initialValue
	 */
	public AtomicDouble(double initialValue)
	{
		data = new AtomicLong(Double.doubleToLongBits(initialValue));
	}
	
	/**
	 * Atomically sets the double to the newValue if its current value is equal to the expected value. 
	 * 
	 * Returns true if the value was set, false if it was not.
	 * 
	 * @param expected
	 * @param newValue
	 * @return
	 */
	public boolean compareAndSet(double expected, double newValue)
	{
		return data.compareAndSet(Double.doubleToLongBits(expected), Double.doubleToLongBits(newValue));
	}
		
	/**
	 * Atomically gets the value of this double. 
	 * 
	 * @return
	 */
	public double get()
	{
		return Double.longBitsToDouble(data.get());
	}
	
	/**
	 * Atomically sets the value of this double. 
	 * 
	 * @return
	 */
	public void set(double newValue)
	{
		data.set(Double.doubleToLongBits(newValue)); 
	}	
	
	/**
	 * Atomically adds the given value to this AtomicDouble. Will limit 
	 * the resulting number to the given saturation point.  
	 * 
	 * @param value
	 * @param saturationPoint - the maximum allowed value.
	 * @return true if the addition saturated, false if it didn't;  
	 */
	public boolean addWithSaturation(double value, double saturationPoint)
	{
		double currentValue, newValue; 
		boolean additionSaturated = false; 
		
		do 
		{
			currentValue = get(); 	
			
			/*
			 *  If adding the new value will cross the saturation point, use the saturation point.
			 */
			if (value > 0 && currentValue >= 0 
					&& (value > saturationPoint || value > (saturationPoint - currentValue)))
			{
				newValue = saturationPoint;
				additionSaturated = true; 
			}
			else
			{
				newValue = currentValue + value;
				additionSaturated = false;
			}
		} 
		while (!compareAndSet(currentValue, newValue));	
		
		return additionSaturated; 
	}
	
	/**
	 * Atomically adds the given value to this AtomicDouble.  
	 * 
	 * @param value
	 */
	public void add(double value)
	{
		double currentValue, newValue; 
		
		do 
		{
			currentValue = get(); 				
			newValue = currentValue + value;
		}
		while (!compareAndSet(currentValue, newValue));	
	}
}
