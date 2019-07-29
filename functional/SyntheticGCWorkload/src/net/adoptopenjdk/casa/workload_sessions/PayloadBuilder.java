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
package net.adoptopenjdk.casa.workload_sessions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;
import net.adoptopenjdk.casa.workload_sessions.payload.ByteArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.CharArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.GaussianDistributedArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.IntArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.LongArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.Payload;
import net.adoptopenjdk.casa.workload_sessions.payload.PayloadException;
import net.adoptopenjdk.casa.workload_sessions.payload.PayloadType;
import net.adoptopenjdk.casa.workload_sessions.payload.RandomArrayPayload;
import net.adoptopenjdk.casa.workload_sessions.payload.ReflexivePayloadCompiler;
import net.adoptopenjdk.casa.xml_parser.ParseUtilities;

/**
 * Responsible for delivering Payload objects the the container. 
 * 
 *  
 */
public class PayloadBuilder 
{	
	/*
	 * Must be less than Double.MAX_VALUE - payload.getSize()
	 *  
	 * All payloads will be less than this.  
	 */
	public static final double MAX_BALANCE = ParseUtilities.parseDataSizeToBytes("1GB");
	
	// The constructor for the payloadClass. 
	private Constructor <? extends Payload> payloadConstructor; 
		
	private volatile double balance;

	// Associated configuration representing a <payload ...> tag. 
	private final PayloadConfiguration payloadConfiguration;	
	
	private final PayloadContainer container; 
	
	private Workload workload; 
	
	/**
	 * Returns a new instance of PayloadBuilder for the given configuration. Compiles 
	 * Payload class if required and loads it.  
	 * 	 
	 * @param payloadConfiguration	
	 */
	public PayloadBuilder(PayloadConfiguration payloadConfiguration, PayloadBuilderSet set, Workload workload)
	{					
		balance = 0;
		
		this.container = set.getContainer();  
		this.workload = workload;
		
		Event.ASSERTION.issue(payloadConfiguration == null, "payloadConfiguration is null");
						
		this.payloadConfiguration = payloadConfiguration;		
		
		// If we're using a reflexive Payload, compile/load it and get its constructor. 
		if (payloadConfiguration.getPayloadType().equals(PayloadType.REFLEXIVE)) 
		{			
			try 
			{
				ReflexivePayloadCompiler compiler = new ReflexivePayloadCompiler(payloadConfiguration);
				payloadConstructor = compiler.getConstructor(); 
			}
			catch (PayloadException e) 
			{
				Event.FATAL_ERROR.issue(e, "initializing payload class.");
			}
		}
		// Otherwise, get the constructor from the payload class. 
		else 
		{	
			try 
			{
				switch (payloadConfiguration.getPayloadType())
				{				
					case CHAR_ARRAY:     
						payloadConstructor = CharArrayPayload.class.getConstructor(PayloadConfiguration.class); 
						break; 
					case BYTE_ARRAY:     
						payloadConstructor = ByteArrayPayload.class.getConstructor(PayloadConfiguration.class); 
						break; 
					case LONG_ARRAY:     
						payloadConstructor = LongArrayPayload.class.getConstructor(PayloadConfiguration.class);
						break; 
					case INT_ARRAY:      
						payloadConstructor = IntArrayPayload.class.getConstructor(PayloadConfiguration.class);
						break; 
					case GAUSSIAN_ARRAY: 
						payloadConstructor = GaussianDistributedArrayPayload.class.getConstructor(PayloadConfiguration.class);
						break; 
					case RANDOM_ARRAY:   
						payloadConstructor = RandomArrayPayload.class.getConstructor(PayloadConfiguration.class);
						break; 
					default:
						Event.FATAL_ERROR.issue("Unimplemented payloadType: " + payloadConfiguration.getPayloadType().toString());
						payloadConstructor = null; 					 			
				}	
			}
			catch (NoSuchMethodException | SecurityException e)
			{
				Event.FATAL_ERROR.issue(e, "getting payload constructor.");
			}							
		}
	}
	
	/**
	 * Gets the current balance for the builder. This will be transferred 
	 * into each worker thread 
	 * 
	 * @return
	 */
	protected double getBalance()
	{
		return balance; 
	}
	
	/**
	 * Gets this builder's container, common to all buidlers in a set. 
	 * 
	 * @return
	 */
	protected PayloadContainer getContainer()
	{
		return container; 
	}
	
	/**
	 * Increases this builder's balance by the given amount multiplied 
	 * by its weight. 
	 * 
	 * @param payment	
	 */	
	protected void pay(double payment)
	{	
		//Event.ASSERTION.issue(payment < 0, "tried to pay builder a negative amount");
		
		if (payment > 0) 
		{
			double amount = (payment * payloadConfiguration.getProportionOfAllocation());
			
			balance += amount; 
		}		
	}
			
	/**
	 * Attempts to allocate an instance of this builder's payload in the container.
	 * 
	 * @param thread
	 * @return
	 * @throws Throwable
	 */
	protected long process(PayloadBuilderThread thread) throws Throwable
	{		
		try 
		{					
			// Do the allocation				
			Payload payload = payloadConstructor.newInstance(payloadConfiguration);
								
			// Add it to the container. 
			container.add(payload, thread);
				 
			// Return the actual size for accounting. 
			return payload.getSize();  						
		}	
		// Unwrap InvocationTargetException 
		catch (InvocationTargetException e)
		{										
			throw e.getTargetException();					
		}			
		// The container is done. 
		catch (PayloadContainer.DoneException | InterruptedException e)
		{
			thread.interrupt(); 
			return 0; 
		}
		// Couldn't allocate 
		catch (OutOfMemoryError e)
		{	
			try 
			{
				Event.RUNTIME_EXCEPTION.issue(e, "allocation failed. Container: " + container + "." + 
						" Free heap: " + Utilities.formatDataSize(Runtime.getRuntime().freeMemory()) + ". Request: " + Utilities.formatDataSize(PayloadBuilder.this.getPayloadConfiguration().getSize()));
				return 0;
			}			
			// Couldn't form or issue error message. 
			catch (Throwable t)
			{	
				throw t; 				
			}
		}
		// Something bad happened.    
		catch (Throwable e)
		{			
			throw e; 			
		}		
	}
	
	/**
	 * Returns the payload configuration being used by this 
	 * builder. 
	 * 
	 * @return
	 */
	protected PayloadConfiguration getPayloadConfiguration()
	{
		return payloadConfiguration; 
	}
		
	
	/**
	 * Gets an estimate of the space this builder's payload will occupy after the peak time.  
	 * 
	 * @return a number of bytes 
	 */
	protected double getExpectedPeakUsage()
	{								
		double workloadDuration = workload.getWorkloadConfiguration().getDuration();
		double maintenancePeriod = workload.getWorkloadConfiguration().getMaintenancePeriod(); 		
		 
		// Estimate the maximum number of payloads delivered by this builder which will be allocated at one time and multiply it by the size. 
		return payloadConfiguration.getMaxConcurrentPayloads(maintenancePeriod, workloadDuration) * payloadConfiguration.getSize();				
	}
		
	/**
	 * Estimates when this builder's payloads will occupy the most space. 
	 * 
	 * This will be less than the end time if nonzero. If zero, the payload will never allocate. 
	 * 
	 * @return a time in seconds from the start of the workload 
	 */
	protected double getExpectedPeakUsageTime()
	{			
		double workloadDuration = workload.getWorkloadConfiguration().getDuration();
		double maintenancePeriod = workload.getWorkloadConfiguration().getMaintenancePeriod(); 
		
		// Estimate the time at which the first payload from this builder may be freed.		
		return payloadConfiguration.getTimeToFirstDecay(maintenancePeriod, workloadDuration) - payloadConfiguration.getAllocationPeriod();							
	}
	
}
