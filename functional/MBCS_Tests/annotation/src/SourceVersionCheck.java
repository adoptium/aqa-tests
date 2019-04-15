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
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

public class SourceVersionCheck {
    static public void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java Version expectedFile");
            System.exit(1);
        }
        boolean flag = false;
        final String target = "AnnotationProcessor";
        Path sourcePath = Paths.get(target+"_org.java");
        String[] expectedResults = {};

        String javaPath = System.getProperty("java.home");
        if (javaPath.endsWith("jre")) {
            javaPath = javaPath.substring(0, javaPath.length() - 3);
        }
        if (!javaPath.endsWith(File.separator)) {
            javaPath += File.separator;
        }
        javaPath += "bin" + File.separator;

        try {
            expectedResults = Files.lines(Paths.get(args[0]),
                                          Charset.defaultCharset())
                                   .toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(SourceVersion sv: SourceVersion.values()) {
            // Check for RELEASE_6 and later
            if (sv.ordinal() < SourceVersion.RELEASE_6.ordinal()) continue;

            // Start
            System.out.println("execute javac with processor option with " + sv);
            String targetFile = target+"_" + sv.toString()+".java";

            // Replace annotation processor
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(
                                     new FileWriter(new File(targetFile)))))
            {
                Files.lines(sourcePath, Charset.defaultCharset())
                        .map(v -> v.replaceAll("%%VERSION%%", sv.toString()))
                        .forEach(s -> pw.println(s));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // Execute javac with annotation
                String[] command = {javaPath + "javac", targetFile};
                Runtime runtime = Runtime.getRuntime();
                Process p = runtime.exec(command, null, null);
                int ret = p.waitFor();
                if (ret != 0) {
                    System.err.println("Error: javac got an error");
                    System.exit(-1);
                }

                String[] cmdProcessor = { javaPath +"javac",
                            "-processor", target + "_" + sv.toString(),
                            "AnnotatedTest.java"};
                p = runtime.exec(cmdProcessor, null, null);
                ret = p.waitFor();
                if (ret != 0) {
                    System.err.println("Error: javac -processor got an error");
                    System.exit(-1);
                }
                InputStream is = p.getInputStream();
                BufferedReader br =
                    new BufferedReader(new InputStreamReader(is));
                String[] result = br.lines().toArray(String[]::new);
                br.close();
                is.close();
                compareResult(expectedResults, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean compareResult(String[] l1, String[] l2) {
        boolean result = true;
        int len1 = l1.length;
        int len2 = l2.length;
        int i = 0;
        for (; i < Math.min(len1, len2); i++) {
            if (!l1[i].equals(l2[i])) {
                System.err.println(i+"< "+l1[i]);
                System.err.println(i+"> "+l2[i]);
                result = false;
            }
        }
        if (len1 > len2) {
            result = false;
            for (; i < len1; i++) {
                System.err.println(i+"< "+l1[i]);
            }
        } else if (len1 < len2) {
            result = false;
            for (; i < len2; i++) {
                System.err.println(i+"> "+l2[i]);
            }
        }
        return result;
    }
}
