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

// Scripts data is in http://www.unicode.org/Public/*/ucd/Scripts.txt

public class UnicodeScriptChecker {
  static int cnt = 0;
  private static void checker(String startStr, String endStr, String name) {
    int startCode = Integer.parseInt(startStr,16);
    int endCode   = Integer.parseInt(endStr,16);
    name = name.toUpperCase();
    Character.UnicodeScript script1 = Character.UnicodeScript.valueOf(name);
    if (!name.equals(script1.toString()))
      System.err.printf("[%s]<->[%s]%n", name, script1.toString());
    for (int i=startCode; i<=endCode; i++) {
      Character.UnicodeScript script2 = Character.UnicodeScript.of(i);
      if (!script1.equals(script2))
        System.err.printf("%x; [%s]<->[%s]%n", i, script1.toString(), script2.toString());
      cnt++;
    }
  }
  public static void main(String[] args) {
    String[] vals;
    if (args.length > 0) {
      vals = args;
    } else {
      String FS = System.getProperty("file.separator");
      String BASE = System.getenv("BASE");
      if (null == BASE) BASE = ".";
      long version = JavaVersion.getVersion();
      if (version >= 13000000L) {
        vals = new String[]{ BASE+FS+"Scripts-12.1.0.txt" };
      } else if (version >= 12000000L) {
        vals = new String[]{ BASE+FS+"Scripts-11.0.0.txt" };
      } else if (version >= 11000000L) {
        vals = new String[]{ BASE+FS+"Scripts-10.0.0.txt" };
      } else {
        vals = new String[0];
      }
    }

    for(String filename : vals) {
      File f = new File(filename);
      try(BufferedReader reader = new BufferedReader(new FileReader(f))) {
        System.out.println("Checking "+f.getName());
        String s;
        cnt = 0;
        while((s = reader.readLine()) != null){
          if (s.length() == 0) continue;
          if (s.startsWith("#")) continue;
          try (Scanner scanner = new Scanner(s)) {
            scanner.findInLine("^([0-9a-fA-F]+)\\s+;\\s(\\S+)");
            MatchResult result = scanner.match();
            if (result.groupCount() == 2) {
            checker(result.group(1),result.group(1),result.group(2));
            }
            continue;
          } catch (IllegalStateException ise) {}
          try (Scanner scanner = new Scanner(s)) {
            scanner.findInLine("^([0-9a-fA-F]+)\\.\\.([0-9a-fA-F]+)\\s+;\\s(\\S+)");
            MatchResult result = scanner.match();
            if (result.groupCount() == 3) {
              checker(result.group(1),result.group(2),result.group(3));
            }
            continue;
          } catch (IllegalStateException ise) {}
        }
        System.out.println("  Checked "+cnt+" characters");
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }
}
