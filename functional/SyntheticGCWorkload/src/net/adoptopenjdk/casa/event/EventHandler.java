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
package net.adoptopenjdk.casa.event;

/**
 * Interface for a global event handler. 
 * 
 *  
 *
 */
public interface EventHandler
{			
	////////////////////////////////////
	// Unconditional issue() prototypes  
	////////////////////////////////////
	
	/**
	 * Trigger the event with no message printed. 
	 * 
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(); 
	
	/**
	 * Trigger the event, issuing the given message. Returns true 
	 * if the event was triggered. 
	 * 
	 * @param message
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(String message);
	
	/**
	 * Trigger the event, issuing the given throwable
	 * 
	 * @param e - If fatal, a stack trace is printed from e. If not, its message is added to the printed output.
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(Throwable e);
	
	/**
	 * Trigger the event, issuing the given throwable and message. 
	 * 
	 * @param e - If fatal, a stack trace is printed from e. If not, its message is added to the printed output. 
	 * @param message
	 * @return true if the event was triggered but this call.  
	 */
	abstract boolean issue(Throwable e, String message);
	
	//////////////////////////////////
	// Conditional issue() prototypes  
	//////////////////////////////////
	
	/**
	 * Trigger the event if the condition is true, issuing the given 
	 * message. Returns true if the event was triggered by this call. 
	 * 
	 * @param condition - The event will not be triggered and false will be returned if the condition is false. 
	 * @param message
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(boolean condition, String message);
	
	/**
	 * Trigger the event if the condition is true, issuing the given 
	 * throwable. Returns true if the event was triggered by this call. 
	 *
	 * @param condition - The event will not be triggered and false will be returned if the condition is false. 
	 * @param e - If fatal, a stack trace is printed from e. If not, its message is added to the printed output.
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(boolean condition, Throwable e);
	
	/**
	 * Trigger the event if the condition is true, issuing the given 
	 * message and throwable. Returns true if the event was triggered. 
	 * 
	 * @param condition - The event will not be triggered and false will be returned if the condition is false. 
	 * @param e - If fatal, a stack trace is printed from e. If not, its message is added to the printed output.
	 * @param message
	 * @return true if the event was triggered but this call. 
	 */
	abstract boolean issue(boolean condition, Throwable e, String message);
}
