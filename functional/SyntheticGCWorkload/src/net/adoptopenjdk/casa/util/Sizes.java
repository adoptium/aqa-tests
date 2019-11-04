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

/**
 * Size estimates for various things 
 * 
 * Disclaimer: These sizes are estimates only, to be used for rough 
 * overhead estimations. None of the sizes given here in any way reflect 
 * a particular VM implementation. 
 * 
 *  
 *
 */
public class Sizes 
{
	public static final long LOCKWORD_SIZE = 8; 	
	public static final long OBJECTHEADER_SIZE = 16;
	
	public  static final long ALIGNMENT = 8; 
	
	public static final long MAX_ARRAY = Integer.MAX_VALUE; 
	
	public static final long MIN_OBJECT_SIZE = OBJECTHEADER_SIZE + LOCKWORD_SIZE;
	
	public static final long REFERENCE_SIZE  = 8;	
	
	// Atomic reference arrays are roughly the size of an object containing a single array 
	public static final long ATOMIC_REFERENCE_ARRAY_SIZE  = Sizes.MIN_OBJECT_SIZE + Sizes.REFERENCE_SIZE + Sizes.ARRAY_SIZE;
	
	public static final long ARRAY_SIZE  = 16;
	public static final long DOUBLE_SIZE  = 8;
	public static final long LONG_SIZE  = 8;
	
	// Size of an instance of AtomicReference<>
	public static final long ATOMIC_REFERENCE_SIZE = MIN_OBJECT_SIZE + REFERENCE_SIZE;
	public static final long INT_SIZE = 4;	
}
