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
import java.util.*;

public class MainStarter {
    public static void main(String[] args) throws Exception {
        String javaExec = System.getProperty("java.home")+
            System.getProperty("file.separator")+"bin"+
            System.getProperty("file.separator")+"java";
        String classPath = System.getProperty("java.class.path");
        for(File f : (new File(System.getenv("BASE")+System.getProperty("file.separator")+"ext")).listFiles()) {
            if (f.isFile() && f.getCanonicalPath().endsWith(".jar")) {
                classPath += System.getProperty("path.separator");
                classPath += f.getCanonicalPath();
            }
        }

        String classExec = "Main";

        System.out.println("Default ...");
        ProcessBuilder pb = new ProcessBuilder(javaExec, "-classpath", classPath, classExec);
        Process p = pb.start();
        java.io.InputStream is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");

        System.out.println("JRE ...");
        pb = new ProcessBuilder(javaExec, "-classpath", classPath, "-Djava.locale.providers=JRE", classExec);
        p = pb.start();
        is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");

        /*
        System.out.println("HOST ...");
        pb = new ProcessBuilder(javaExec, "-classpath", classPath, "-Djava.locale.providers=HOST", classExec);
        p = pb.start();
        is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");
        */

        System.out.println("CLDR ...");
        pb = new ProcessBuilder(javaExec, "-classpath", classPath, "-Djava.locale.providers=CLDR", classExec);
        p = pb.start();
        is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");

        System.out.println("SPI ...");
        pb = new ProcessBuilder(javaExec, "-classpath", classPath, "-Djava.locale.providers=SPI", classExec);
        p = pb.start();
        is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");

        System.out.println("CLDR,JRE ...");
        pb = new ProcessBuilder(javaExec, "-classpath", classPath, "-Djava.locale.providers=CLDR,JRE", classExec);
        p = pb.start();
        is = p.getInputStream();
        while (true) {
            int c = is.read();
            if (c == -1) {
                is.close();
                break;
            }
            System.out.print((char)c);
        }
        System.out.println(0 == p.waitFor() ? "Done" : "Error");
    }
}
