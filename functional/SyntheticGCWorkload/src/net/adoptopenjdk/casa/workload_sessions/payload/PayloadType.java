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
package net.adoptopenjdk.casa.workload_sessions.payload;

import net.adoptopenjdk.casa.util.Sizes;
import net.adoptopenjdk.casa.workload_sessions.PayloadBuilder;


/**
 * To add a new type: 
 * 
 * - add a new Payload class to this package for your payload
 * - add a new type to this enum and implement the necessary methods 
 * - update PayloadBuilder to create the new type of Payload
 * - it may also be necessary to update the parsing of the payload tag in PayloadConfiguration if your type has its own parameters  
 * 
 *  
 *
 */
public enum PayloadType 
{
	REFLEXIVE 
	{	
		public long getAlignment()
		{
			return 8; 
		}
		
		public long minSize()
		{
			return Payload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return Payload.REFLEXIVE_CUTOFF; 
		}
	}, 
	GAUSSIAN_ARRAY 
	{
		public long getAlignment()
		{
			return GaussianDistributedArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(GaussianDistributedArrayPayload.BYTES_PER_ELEMENT);						 
		}
	},
	RANDOM_ARRAY 
	{
		public long getAlignment()
		{
			return RandomArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(RandomArrayPayload.BYTES_PER_ELEMENT);				 
		}
	},
	CHAR_ARRAY 
	{
		public long getAlignment()
		{			
			return CharArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(CharArrayPayload.BYTES_PER_ELEMENT); 
		}
	},
	BYTE_ARRAY 
	{
		public long getAlignment()
		{
			return ByteArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(ByteArrayPayload.BYTES_PER_ELEMENT);
		}
	},
	INT_ARRAY 
	{
		public long getAlignment()
		{
			return IntArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(IntArrayPayload.BYTES_PER_ELEMENT);
		}

	}, 
	LONG_ARRAY 
	{
		public long getAlignment()
		{
			return LongArrayPayload.BYTES_PER_ELEMENT; 
		}
		
		public long minSize()
		{
			return ArrayPayload.OVERHEAD; 
		}
		
		public long maxSize()
		{
			return getMaxArraySize(LongArrayPayload.BYTES_PER_ELEMENT);
		}
	},
	/////////////////////////////////////////
	// Metatypes, resolved during parsing 
	AUTO 
	{
		public long getAlignment()
		{
			return 0;  
		}
		
		public long maxSize()
		{
			return 0; 
		}
		
		public long minSize()
		{
			return 0; 
		}	
	}, 
	NONE 
	{	
		public long getAlignment()
		{
			return 0;  
		}
		
		public long maxSize()
		{
			return 0; 
		}
		
		public long minSize()
		{
			return 0; 
		}					
	}; 
	
	private static long getMaxArraySize(int bytesPerElement)
	{
		long objectSizeRestriction = ArrayPayload.OVERHEAD + (Sizes.MAX_ARRAY * bytesPerElement);
		
		return (PayloadBuilder.MAX_BALANCE > objectSizeRestriction) ? (long)PayloadBuilder.MAX_BALANCE : objectSizeRestriction;  
	}
	
	/**
	 * Gets the largest possible size for this type. 
	 * 
	 * @return
	 */
	public abstract long maxSize();
	
	/**
	 * Gets the smallest allowable size for this type. 
	 * 
	 * @return
	 */
	public abstract long minSize();
	
	/**
	 * Gets the alignment for this type. 
	 * 
	 * @return
	 */
	public abstract long getAlignment(); 
}
