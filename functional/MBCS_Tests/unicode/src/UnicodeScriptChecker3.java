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
import java.util.regex.*;

// PropertyValueAliases data is in http://www.unicode.org/Public/*/PropertyValueAliases.txt

public class UnicodeScriptChecker3 {
  public static void main(String[] args) throws Exception {
    System.out.println("Checking PropertyValueAliases");
    System.out.println("  Total "+Character.UnicodeScript.values().length+" enums are defined");

    String[] vals;
    if (args.length > 0) {
      vals = args;
    } else {
      String FS = System.getProperty("file.separator");
      String BASE = System.getenv("BASE");
      if (null == BASE) BASE = ".";
      long version = JavaVersion.getVersion();
      if (version >= 12000000L) {
        vals = new String[]{ BASE+FS+"PropertyValueAliases-11.0.0.txt" };
      } else if (version >= 11000000L) {
        vals = new String[]{ BASE+FS+"PropertyValueAliases-10.0.0.txt" };
      } else {
        vals = new String[0];
      }
    }

    for(String filename : vals) {
      File f = new File(filename);
      BufferedReader reader = new BufferedReader(new FileReader(f));
      System.out.println("  Checking "+f.getName());
      String s;
      int cnt = 0;
      while((s = reader.readLine()) != null){
        if (!s.startsWith("sc ;")) continue;
        Scanner scanner = new Scanner(s);
        scanner.findInLine("^sc ; (\\S+)\\s*;\\s(\\S+)");
        try {
            MatchResult result = scanner.match();
            if (result.groupCount() == 2) {
                String code = result.group(1);
                String value = result.group(2).toUpperCase();
                try {
                    Character.UnicodeScript script = Character.UnicodeScript.forName(code);
                    if (!value.equals(script.toString()))
                        System.err.println("Code mismatch: "+code+": "+value+" <-> "+script.toString());
                } catch (IllegalArgumentException iae) {
                    if ("Hrkt".equals(code)) {
                        System.out.println("  Missing code(expected): "+code);
                    } else {
                        System.err.println("  Missing code: "+code);
                    }
                }
                try {
                    Character.UnicodeScript script = Character.UnicodeScript.valueOf(value);
                    if (!value.equals(script.toString()))
                        System.err.println("Value mismatch: "+value+" <-> "+script.toString());
                } catch (IllegalArgumentException iae) {
                    if ("KATAKANA_OR_HIRAGANA".equals(value)) {
                        System.out.println("  Missing value(expected): "+value);
                    } else {
                        System.err.println("  Missing value: "+value);
                    }
                }
                cnt++;
            }
        } catch (IllegalStateException ise) {}
        scanner.close(); 
      }
      reader.close();
      System.out.println("  Checked "+cnt+" items");
    }
  }
}
