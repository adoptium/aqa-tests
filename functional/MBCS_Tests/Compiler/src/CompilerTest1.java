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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;
import java.util.*;

public class CompilerTest1 {

	public static void main(String[] args) {

		//File file =  new File(args[0] + ".java");
		File file =  new File(args[0]);
		int i = args[0].lastIndexOf(".");
		String st = args[0].substring(0,i).replaceAll("^.*/","");
			
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager filemanager = compiler.getStandardFileManager(
				null, null, null);
		Iterable<? extends JavaFileObject> units = filemanager
			.getJavaFileObjects(file);
		CompilationTask task = compiler.getTask(null, filemanager, null, null,
				null, units);
		task.call();
		
		System.out.println(args[0].replaceAll("^.*/","") + " was compiled.");
		System.out.println("Launching java " + st + "...");


		try {
			Class cls = Class.forName(st);
			Method method = cls.getMethod("main",
					new Class[] { String[].class });
			method.invoke(cls.newInstance(), new Object[] { new String[0] });

			filemanager.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

}
