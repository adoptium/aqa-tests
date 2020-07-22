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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.jar.*;

import javax.tools.*;
import javax.tools.JavaCompiler.*;
import javax.tools.JavaFileObject.*;
import java.lang.reflect.*;

public class UrlClassClose{
    private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private int errCnt = 0;
    protected static final String packageNameTemplate = "com.example.%s.pkg";

    UrlClassClose(String jarName, String className, String[] values) throws MalformedURLException {
        System.out.println("jar file name is "+jarName+", class name is "+className);
        URL url = new URL("file:"+jarName);
        String[] result = new String[values.length];
        for(int i=0 ; i<values.length ; i++){
            comp(jarName, className, values[i]);
            String packageName = String.format(packageNameTemplate, className);
            try {
                Thread.sleep(100);
                URLClassLoader loader = new URLClassLoader(new URL[] {url});
                Class<?> cl = Class.forName(packageName+"."+className, true, loader);
                Runnable foo = (Runnable) cl.getConstructor().newInstance();
                System.out.println("\nThe following is foo.run() result./ Count:"+ (i+1) +" times.");
                foo.run();
                Method method = foo.getClass().getDeclaredMethod(className+"_method", (Class<?>[])null);
                method.invoke(foo, (Object[])null);
                Field field = foo.getClass().getDeclaredField(className+"_field");
                result[i] = (String)field.get(foo);
                loader.close();
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for(int i=0 ; i<values.length ; i++){
           if (!values[i].equals(result[i])) {
               System.out.println("Expected result is \""+values[i]+"\", but \""+result[i]+"\"");
               errCnt++;
           }
        }
    }

    public int getErrCnt() {
        return errCnt;
    }

    public static void main(String[] args) throws Exception {
        int errCnt = 0;

        if (0 == args.length) {
            System.err.println("Usage: java UrlClassClose val1 [val2...]");
            System.exit(1);
        }
        for(int i=0 ; i<args.length ; i++){
            String name = checkst(args[i]);
            if(name.length() > 0) {
                String jarName = name + ".jar";
                String className = name;
                UrlClassClose url_class = new UrlClassClose(jarName, className, args);
                errCnt += url_class.getErrCnt();
            } else {
                continue;
            }
        }
        System.out.println("Test is " + (errCnt == 0 ? "passed." : "failed."));
    }

    public static String checkst(String line){
        char ch;
        StringBuffer valid = new StringBuffer();

        for(int j = 0; j < line.length(); j++) {
            ch = line.charAt(j);
            if (valid.length() == 0) {
                if (Character.isJavaIdentifierStart(ch)) valid.append(ch);
            } else {
                if (Character.isJavaIdentifierPart(ch)) valid.append(ch);
            }
        }
        return valid.toString();
    }    

    public void comp(String jarName, String className, String string){
        ArrayList<JavaFileObject> compilationUnit = new ArrayList<JavaFileObject>();
        string = string.replaceAll("\\\\","\\\\\\\\");
        String javaSource = "package "+String.format(packageNameTemplate, className)+";\n" +
            "public class " + className + " implements Runnable {" +
            "public final static String st_result = \"" + string + "\";" +
            "public String "+className+"_field;" +
            "public void "+className+"_method (){" +
                "System.out.println("+className+"_field);" +
            "}" +
            "public void run(){" +
                className+"_field = st_result;" +
            "}" +
        "}";
        compilationUnit.add(new StringJavaFileObject(className, javaSource));
        ClassFileManager fileManager = new ClassFileManager(compiler);
        CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnit);
        task.call();
        File outJarFile = new File(jarName);
        try {
            JarOutputStream jarOutStream = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(outJarFile)));
            for(String k : fileManager.getKeys()) {
                JarEntry entry = new JarEntry(k.replaceAll("\\.","/")+".class");
                jarOutStream.putNextEntry(entry);
                byte[] classData = fileManager.get(k).getBytes();
                jarOutStream.write(classData);
                jarOutStream.closeEntry();
            }
            jarOutStream.finish();
            jarOutStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   class StringJavaFileObject extends SimpleJavaFileObject {
        private String source;
    
        public StringJavaFileObject(String name, String source) {
            super(URI.create("string:///"+ name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return source;
        }
    }

    class JavaClassObject extends SimpleJavaFileObject {        
        private ByteArrayOutputStream bos = new ByteArrayOutputStream();

        JavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }
        @Override
        public OutputStream openOutputStream() throws IOException {
            return bos;
        }
        public byte[] getBytes() {
            return bos.toByteArray();
        }
    }

    class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private Map<String, JavaClassObject> classObjects = new HashMap<String, JavaClassObject>();

        public ClassFileManager(JavaCompiler compiler) {
            super(compiler.getStandardFileManager(null, null, null));
        }
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            JavaClassObject fileObject = new JavaClassObject(className, kind);
            classObjects.put(className, fileObject);
            return fileObject ;
        }
        public String[] getKeys() {
            return classObjects.keySet().toArray(new String[0]);
        }
        public JavaClassObject get(String k) {
            return classObjects.get(k);
        }
    }
}
