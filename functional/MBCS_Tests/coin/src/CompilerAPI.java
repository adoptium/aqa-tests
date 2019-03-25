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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class CompilerAPI {
    String className;
    String source;

    public CompilerAPI(String className, String source) {
	this.className = className;
	this.source = source;

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager 
            = compiler.getStandardFileManager(null, null, null);

        List<? extends JavaFileObject> fileobjs
            = createJavaFileObjects();

        JavaCompiler.CompilationTask task 
            = compiler.getTask(null,
                               fileManager, 
                               null, 
                               null, 
                               null, 
                               fileobjs);
        task.call();

        try {
            fileManager.close();
        } catch (IOException ex) {
        }
    }

    private List<? extends JavaFileObject> createJavaFileObjects() {
        JavaSourceFromString fileobj 
            = new JavaSourceFromString(className, source);
 
        List<JavaSourceFromString> fileobjs
            = new ArrayList<>();
        fileobjs.add(fileobj);
 
        return fileobjs;
    }

    class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
                super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE);
                this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
        }
    }

    public static String[] convIdentifier(String[] lines) {
	ArrayList<String> list = new ArrayList<>();

	for (String line: lines) {
	    StringBuilder valid = new StringBuilder();

	    for(int i = 0; i < line.length(); i++) {
	        char ch = line.charAt(i);
	        if (valid.length() == 0) {
	            if (Character.isJavaIdentifierStart(ch)) valid.append(ch);
	        } else {
		    if (Character.isJavaIdentifierPart(ch)) valid.append(ch);
	        }
	    }
	    if (valid.length() > 0) {
		list.add(valid.toString());
	    }
	}
	return list.toArray(new String[list.size()]);
    }

    public static void main(String[] args) {
	String className = "Hello";
        String source =
              "public class "+className+" {\n"
            + "    public static void main(String[] args) {\n"
            + "        System.out.println(\"Hello, World!\");\n"
            + "    }"
            + "}";
 
        new CompilerAPI(className, source);
    }
}

