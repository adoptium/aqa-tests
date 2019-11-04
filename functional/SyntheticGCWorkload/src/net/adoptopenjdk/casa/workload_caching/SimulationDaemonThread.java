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

import net.adoptopenjdk.casa.util.Utilities;

/**
 * A thread that monitors a simulation and 
 * 
 *  
 *
 */
class SimulationDaemonThread extends Thread 
{
	private CacheSimulation  simulation;  

	public SimulationDaemonThread(CacheSimulation simulation)
	{
		this.simulation  = simulation; 
	}

	public void run()
	{					
		try						
		{	
			synchronized (this)
			{			
				// Wait until the simulation's start time. 
				Utilities.idle(simulation.getConfiguration().getStartTime());
								
				// Start the simulation 
				simulation.start();									

				// Check for interrupts 
				if (Thread.interrupted())
				{														
					Event.INTERRUPT.issue(); 
					return; 
				}
				
				// Wait the duration of the simulation 
				try 
				{
					Utilities.idle(simulation.getConfiguration().getDuration());								
				}
				// If the daemon thread is interrupted, the simulation. 
				catch (InterruptedException e)
				{																									
					 
				}													
				
				// Shutdown the simulation, printing summary 
				try 
				{																
					simulation.shutdown();
					simulation = null; 
				}
				catch (InterruptedException e)
				{
					
				}
			}
		}
		catch (InterruptedException e)
		{
			// Startup or shutdown was interrupted.
			simulation.kill(); 							
		}
	}
}