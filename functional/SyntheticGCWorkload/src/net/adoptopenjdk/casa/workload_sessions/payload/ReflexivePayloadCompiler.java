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

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import javax.tools.JavaCompiler;

import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import net.adoptopenjdk.casa.util.Sizes;
import net.adoptopenjdk.casa.util.Utilities;
import net.adoptopenjdk.casa.workload_sessions.configuration.PayloadConfiguration;

/**
 * Compiles and loads dynamically generated custom classes derived 
 * from Payload with a particular size and lifetime.  
 * 
 *  
 *
 */
public class ReflexivePayloadCompiler 
{
	// This lock prevents multiple builders from attempting to compile the same Payload class at the same time.
	private static class ClassNameBasedLock { };
	private static HashMap<String, ClassNameBasedLock> classNameBasedLock; 
	private static final JavaCompiler compiler;
	
	// Initialize the compiler and className based lock. 
	static 
	{
		compiler = ToolProvider.getSystemJavaCompiler();
		classNameBasedLock = new HashMap<String, ClassNameBasedLock>();		
	}
	
	/**
	 * Thrown if there is a problem with the loaded payload class. 
	 * 
	 *  
	 *
	 */
	public static class InvalidPayloadClassException extends PayloadException 
	{
		private static final long serialVersionUID = 1L;
	
		public InvalidPayloadClassException(String message)
		{
			super(message);
		}
	}
	
	/**
	 * Thrown if there is a problem compiling the payload. 
	 * 
	 *  
	 *
	 */
	public static class PayloadCompilerException extends PayloadException  
	{
		private static final long serialVersionUID = 1L;
	
		public PayloadCompilerException(String message)
		{
			super(message);
		}
	}
	
	// The configuration we're compiling for. 
	private PayloadConfiguration payloadConfiguration; 
	
	/**
	 * Initializes a new compiler for the given payload configuration. 
	 * 
	 * @param payloadConfiguration
	 */
	public ReflexivePayloadCompiler(PayloadConfiguration payloadConfiguration) 
	{		 			
		this.payloadConfiguration = payloadConfiguration; 
	}	
	
	/**
	 * Loads (and compiles, if necessary) the Payload class, gets its constructor and reutrns it. 
	 * 
	 * @return
	 * @throws PayloadException
	 */
	public Constructor <? extends Payload> getConstructor() throws PayloadException
	{		
		if (payloadConfiguration.getClassName() == null)
			throw new PayloadException("Payload configuration returned null class name.");
		
		try 
		{
			// Attempt to compile and load the Payload class.  
			Class<? extends Payload> payloadClass = getPayloadClass();			
			if (payloadClass == null)
				throw new PayloadCompilerException("payload class is null");
			
			// Get the constructor now to avoid using heap to get it at allocation time. 
			Constructor <? extends Payload> payloadConstructor = payloadClass.getConstructor(PayloadConfiguration.class);			
						
			if (payloadConstructor == null)
				throw new PayloadCompilerException("payload constructor is null");
			
			return payloadConstructor; 
		} 
		// Unable to get the payload class. 
		catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) 
		{
			throw new PayloadCompilerException("caused by " + e.toString());			
		}
	}	
	
	/**
	 * Loads and returns Returns a Payload class. If the class is not found, it is generated and compiled.  
	 * 
	 * @return
	 * @throws ClassNotFoundException - Thrown if the class could still not be found after it was compiled. 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvalidPayloadClassException 
	 * @throws PayloadCompilerException 
	 * @throws PayloadException 
	 */	
	private Class<? extends Payload> getPayloadClass() throws ClassNotFoundException, NoSuchMethodException, SecurityException, PayloadException  
	{				
		Class<? extends Payload> loadedClass; 
		
		// Only allow one thread at a timeto attempt to get a given class. 
		synchronized (getClassNameBasedLock()) 
		{
			// Attempt to load the class. 
			try 
			{
				loadedClass = loadPayloadClass();
			}
			// If the class failed to load, try to compile it and load it. 
			catch (ClassNotFoundException e)
			{
				generateAndCompilePayloadClass(); 
				
				loadedClass = loadPayloadClass();
			}
			finally
			{
				classNameBasedLock.remove(payloadConfiguration.getClassName());
			}
		}
	
		return loadedClass; 
	}	
	
	/**
	 * Returns a lock specific to the given className
	 * 
	 * @param className
	 * @return
	 */
	private ClassNameBasedLock getClassNameBasedLock() throws PayloadException 
	{
		if (payloadConfiguration.getClassName() == null)
			throw new PayloadException("Payload configuration return null class name.");
		
		ClassNameBasedLock lock = null;
        synchronized(classNameBasedLock) 
        {
                // Hashtable.get() does null pointer check
                lock = classNameBasedLock.get(payloadConfiguration.getClassName());
                if (lock == null) {
                        lock = new ClassNameBasedLock();
                        classNameBasedLock.put(payloadConfiguration.getClassName(), lock);
                }
        }

        return lock;
	}
		
	/**
	 * Attempts to load the Payload class. 
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws PayloadException
	 */
	@SuppressWarnings("unchecked")	
	private Class<? extends Payload> loadPayloadClass() throws ClassNotFoundException, NoSuchMethodException, SecurityException, PayloadException
	{
		Class<?> loadedClass = Class.forName(payloadConfiguration.getClassName());
		
		if (loadedClass == null) {
			throw new ClassNotFoundException("Synthetic Payload class not found");  
		}			
		else if (loadedClass.getSuperclass().equals(Payload.class)) {
			return (Class<? extends Payload>) loadedClass;
		}
		else {
			throw new InvalidPayloadClassException("Loaded class " + payloadConfiguration.getClassName() + " is not a subclass of Payload"); 
		}
	}
		
	/**
	 * Attempts to generate and compile the payload class. 
	 * 
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws PayloadException
	 */
	private void generateAndCompilePayloadClass() throws ClassNotFoundException, NoSuchMethodException, SecurityException, PayloadException  
	{
		String source = generatePayloadSubclassSource();
		
		// Payload class was not found, so compile it. 			
		ArrayList<ImaginaryJavaFile> fileObjects = new ArrayList<ImaginaryJavaFile>(); 		
		fileObjects.add(new ImaginaryJavaFile(source));
		
		if (!compiler.getTask(null, null, null, null, null, fileObjects).call()) {
			throw new PayloadCompilerException("compile of payload class failed \n" + source);
		}
	}
		
	/**
	 * Generates the source for the payload class and returns it as a string. The class 
	 * will always contain only longs. 
	 * 
	 * @return
	 * @throws PayloadException
	 */
	private String generatePayloadSubclassSource() throws PayloadException
	{
		StringBuilder source = new StringBuilder();   
				 			
		source.append("public class " + payloadConfiguration.getClassName() + " extends " + Payload.class.getName() + " {");

		// Confirm that the size is large enough 
		if (payloadConfiguration.getSize() < Payload.OVERHEAD)
			throw new PayloadException("payload class of size " + Utilities.formatDataSize(payloadConfiguration.getSize()) + " would be too small. Minimum size is " + Utilities.formatDataSize(Payload.OVERHEAD));
		
		// Confirm that the size is appropriate for a reflexive type
		if (payloadConfiguration.getSize() > Payload.REFLEXIVE_CUTOFF)
			throw new PayloadException("payload class of size " + Utilities.formatDataSize(payloadConfiguration.getSize()) + " is too large for reflexive type. The maximum is " + Payload.REFLEXIVE_CUTOFF + ". Please use another payload type. ");
		
		// Calculate the space available for fields 
		long dataSize = payloadConfiguration.getSize() - Payload.OVERHEAD;
										
		// Confirm alignment 
		if (dataSize % Sizes.ALIGNMENT != 0)
			throw new PayloadException("payload class of size " + Utilities.formatDataSize(payloadConfiguration.getSize()) + " is not divisible by " + Sizes.ALIGNMENT + "."); 
		
		// Calculate the number of long fields 
		long numLongs = Math.round(Math.floor(dataSize/Sizes.LONG_SIZE));
		
		// Append long fields a1...an
		for (int i = 0; i < numLongs; i++)
			source.append("long a" + i + ";");
		
		// Append constructor 
		source.append("public " + payloadConfiguration.getClassName() + "(" + PayloadConfiguration.class.getName() + " configuration) { super(configuration); }");						
		
		source.append("}");	
		 
		return source.toString(); 
	}
	
	/**
	 * Imaginary source file used for compiling the source stirng. 
	 * 
	 *  
	 */
	private class ImaginaryJavaFile extends SimpleJavaFileObject 
	{
		final String code;				
		public ImaginaryJavaFile(String source) {		
			super(URI.create("string:///" + payloadConfiguration.getClassName().replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);			
			this.code = source;
		}
		
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}	
	}
}