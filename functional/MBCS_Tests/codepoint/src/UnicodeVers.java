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

import java.util.*;
import java.io.*;

public class UnicodeVers {
    private final static String javaVersion = String.valueOf(JavaVersion.getVersion());
    private static Properties prop = new Properties();
    private static String[] versions;
    private final static String BASE;
    static {
        try (InputStream is = UnicodeVers.class.getResourceAsStream("UnicodeVers.properties");) {
            prop.load(is);
            ArrayList<String> list = new ArrayList<>();
            for (String s : prop.getProperty("versions","").split("\\s*,\\s*")) {
                list.add(s);
            }
            Collections.sort(list);
            Collections.reverse(list);
            versions = list.toArray(new String[0]);
        } catch (IOException ioe) {
            versions = new String[0];
        }
        String FS = File.separator;
        String dataPath = FS + "data" + FS;
        BASE = System.getenv("BASE") == null ?  "." + dataPath : System.getenv("BASE") + dataPath;
    }

    public static String[] getFiles(String prefix) {
        for (String s : versions) {
            if (s.compareTo(javaVersion) <= 0) {
                String[] sa = prop.getProperty(s,"").split("\\s*,\\s*");
                ArrayList<String> list = new ArrayList<>();
                for (int i = 0; i < sa.length; ++i) {
                    File file = new File(BASE, String.format("%s-%s.txt", prefix, sa[i]));
                    if (file.exists()) list.add(file.getAbsolutePath());
                }
                return list.toArray(new String[0]);
            }
        }
        return new String[0];
    }
}
