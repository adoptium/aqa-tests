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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Allows for the redirection and multiplexing 
 * of output between multiple dynamically added
 * and removed streams. 
 * 
 *  
 */
public class OutputMultiplexer extends OutputStream {

	private ArrayList<Node> outputNodes;
	
	/**
	 * 
	 * 
	 *  
	 */
	public class Node 
	{		
		private final OutputStream stream;
		private final boolean isConsole; 
		
		public Node(OutputStream stream, boolean isConsole)
		{
			this.stream  = stream; 
			this.isConsole = isConsole; 
		}
		
		/**
		 * Compares nodes on the basis of the stream only. 
		 * 
		 * @param node
		 * @return
		 */
		public boolean equals(Node node)
		{
			return stream.equals(node.stream);
		}
	}

	/**
	 * Instantiates an empty multiplexer. 
	 */
	public OutputMultiplexer() 
	{
	    outputNodes = new ArrayList<Node>();
	}

	/**
	 * Removes the given output stream. 
	 * 
	 * @param out
	 */
	public void remove(OutputStream out)
	{		
		outputNodes.remove(new Node(out, false));
	}
	
	/**
	 * Remove all output streams. 
	 */
	public void clear()
	{
		outputNodes.clear(); 
	}
	
	/**
	 * Removes any nodes flagged as consoles.  
	 */
	public void removeConsoles()
	{
		ArrayList<Node> newNodes = new ArrayList<Node> (); 
		
		for (Node node : outputNodes)
	    {	  
			if (!node.isConsole)
				newNodes.add(node); 	 
	    }
		
		outputNodes = newNodes;
	}
	
	/**
	 * Adds the stream and flags it with the given console flag. 
	 * 
	 * @param out
	 * @param isConsole
	 */
	public void add(OutputStream out, boolean isConsole)
	{
		this.outputNodes.add(new Node(out, isConsole));
	}
	
	/**
	 * Writes c to all outputs. Converts \r to \n. 
	 */
	public void write(int c) throws IOException 
	{
	    for (Node node : outputNodes)
	    {	    		    
	    	if (!node.isConsole && c == '\r')	    	
			   node.stream.write('\n');	    	
	    	else	    	
	    		node.stream.write(c);
	    }
	}
	
	/**
	 * Writes the bytes to the stream. If the output is not being written to 
	 * System.out or System.err, replaces \r with \n in the stream.  
	 * 
	 * @param b
	 */
	public void write(byte[] b) throws IOException
	{					
		for (Node node : outputNodes) 
		{					
			if (node.isConsole)
				node.stream.write(b);
			else
				node.stream.write(new String(b).replace('\r', '\n').getBytes());	
		}
	}

	/** 
	 * Writes b to all outputs. Converts \r to \n in nodes which aren't consoles.  
	 */
	public void write(byte[] b, int off, int len) throws IOException
	{
		for (Node node : outputNodes)
		{
			if (node.isConsole)
				node.stream.write(b, off, len);
			else 
				node.stream.write(new String(b).replace('\r', '\n').getBytes(), off, len);
		}	    
	}

	public void close() throws IOException
	{
		for (Node node : outputNodes) 			
			node.stream.close();		    
	}

	public void flush() throws IOException
	{
		for (Node node : outputNodes) 			
			node.stream.flush(); 			
	}
}