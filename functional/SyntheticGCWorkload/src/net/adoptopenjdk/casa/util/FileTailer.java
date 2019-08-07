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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import net.adoptopenjdk.casa.event.EventHandler;

/**
 * Follows a file for changes and writes those changes to the stream. 
 * 
 * Always starts at the beginning of the file and writes each new change from the last 
 * read position. 
 * 
 * The function of this class is identical to "tail -f -n infinity" 
 * 
 *  
 */
public class FileTailer extends Thread
{
	private PipedInputStream stream;						
	private BufferedWriter writer;
	private long charsRead; 
	private BufferedReader reader;
	private String filename; 
	private double period; 
	
	private EventHandler error;
	
	/**
	 * Creates a stream and appends it with all bytes appended to the given file. The stream 
	 * may be retrieved with the getInputStream() function. The tailer is a Thread 
	 * and may be stopped with a call to its interrupt() method.  
	 * 
	 * @param filename - Filename to tail 
	 * @param period - Period to wait between reads of the file. 
	 * @param error - handler for exceptional events. 
	 * @throws IOException
	 */
	public FileTailer(String filename, double period, EventHandler error) throws IOException
	{
		this.error = error; 
		this.filename = filename;
		this.period = period;
		charsRead = 0;
		
		stream = new PipedInputStream();						
		writer = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(stream)));
		reader = null; 						
		
		setDaemon(true); 
		start(); 
	}
	
	/**
	 * Gets a stream which will be written to with everything 
	 * contained within the file on startup and everything appended to it thereafter. 
	 * 
	 * @return an InputStream that can be read with a buffered reader & input stream reader. The InputStream is a PipedInputStream.  
	 */
	public InputStream getInputStream()
	{
		return stream; 
	}
	
	/**
	 * Tails file until interrupted. 
	 */
	public void run()
	{		
		while (true) 
		{					
			try 
			{
				// Wait for the next read cycle 
				Utilities.idle(period);
				
				// Die on interrupt 
				if (Thread.interrupted())
					break;
				
				// Open the file and skip ahead to the next char 
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));				
				reader.skip(charsRead);							
				
				// Read from the next char after the last one read. 
				int c; 
				while ((c = reader.read()) != -1) 
				{	
					// Write each char out to the stream  
					writer.write(c);
					// Keep track of position. 
					charsRead++; 		
				}
				
				// Close the file until the next read cycle 
				reader.close();
				
				// Push out any bytes written. 
				writer.flush(); 
			}
			catch (InterruptedException e) 
			{
				break; 
			}
			catch (Throwable e) 			
			{
				error.issue(e);
			}								
		}			
	}
}
