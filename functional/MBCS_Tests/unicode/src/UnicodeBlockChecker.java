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

// Unicode Block data is in http://www.unicode.org/Public/*/Blocks*.txt

public class UnicodeBlockChecker {
  public static void main(String[] args) throws Exception {
    Hashtable<String, String> blockTable = new Hashtable<String, String>();
    blockTable.put("greek and coptic", "GREEK");
    blockTable.put("cyrillic supplement", "CYRILLIC SUPPLEMENTARY");
    blockTable.put("combining diacritical marks for symbols", "COMBINING MARKS FOR SYMBOLS");

    String[] vals;
    if (args.length > 0) {
      vals = args;
    } else {
      vals = UnicodeVers.getFiles("Blocks");
    }

    for(String filename : vals) {
      File f = new File(filename);
      BufferedReader reader = new BufferedReader(new FileReader(f));
      System.out.println("Checking "+f.getName());
      String s;
      int cnt = 0;
      int undefined = 0;
      while((s = reader.readLine()) != null){
        if (s.length() == 0) continue;
        if (s.startsWith("#")) continue;
        Scanner scanner = new Scanner(s);
        scanner.findInLine("([0-9a-fA-F]+)\\.\\.([0-9a-fA-F]+);\\s(.+)$");
        MatchResult result = scanner.match();
        if (result.groupCount() == 3) {
          int startCode = Integer.parseInt(result.group(1),16);
          int endCode = Integer.parseInt(result.group(2),16);
          String blockName = result.group(3).replace('-', ' ');
          if (blockTable.containsKey(blockName.toLowerCase(Locale.ENGLISH)))
            blockName = blockTable.get(blockName.toLowerCase(Locale.ENGLISH));
          for (int i=startCode; i<=endCode; i++) {
            String name = (Character.UnicodeBlock.of(i)==null ? "UNASSIGNED" : Character.UnicodeBlock.of(i)).toString().replace('_', ' ');
            if (!blockName.equalsIgnoreCase(name))
              System.err.printf("%x; [%s]<->[%s]%n", i, blockName, name);
            cnt++;
            if (Character.isDefined(i) == false) undefined++;
          }
        }
        scanner.close(); 
      }
      reader.close();
      System.out.println("  Checked "+cnt+" characters");
      System.out.println("  Undefined "+undefined+" characters");
    }
  }
}
