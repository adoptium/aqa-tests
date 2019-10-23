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

import java.util.concurrent.atomic.AtomicInteger;

class StringFIFO
{
	// A FIFO buffer. 
	private String[] fifo;
	
	private int readLocation; 
	private int writeLocation;
	private AtomicInteger count; 		
	
	public StringFIFO(int size)
	{
		fifo = new String[size];
		 
		readLocation = 0; 
		writeLocation = 0; 
		count = new AtomicInteger(0); 
	}
	
	public String poll()
	{	
		synchronized (fifo)
		{
			if (count.get() > 0)
			{
				String readString = fifo[readLocation];
				fifo[readLocation] = null; 
				incrementReadLocation(); 
				count.decrementAndGet(); 
				return readString; 
			}
			else 
			{
				return null; 
			}
		}
	}
	
	private boolean write(String string)
	{
		synchronized(this)
		{
			if (count.get() < fifo.length)
			{
				fifo[writeLocation] = string; 
				incrementWriteLocation(); 
				count.incrementAndGet(); 
				return true; 
			}
			else 
			{
				return false; 
			}
		}
	}
	
	/**
	 * Blocks until a string is read. 
	 * 
	 * @param string
	 * @throws InterruptedException 
	 * @throws NumberFormatException 
	 */
	public void put(String string) throws InterruptedException
	{
		synchronized(this)
		{
			while (!write(string))
			{
				if (Thread.interrupted())
					throw new InterruptedException(); 
				
				//Utilities.idle(ParseUtilities.parseTime("1us")); 				
			}
		}
	}
	
	private void incrementReadLocation()
	{
		readLocation = (readLocation >= fifo.length - 1) ? 0 : readLocation + 1; 
	}
	
	private void incrementWriteLocation()
	{
		writeLocation = (writeLocation >= fifo.length - 1) ? 0 : writeLocation + 1; 
	}
}