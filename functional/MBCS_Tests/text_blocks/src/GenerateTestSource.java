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

public class GenerateTestSource {
  public static void main(String[] args) {
    String[] string_array = new String[5];
    Arrays.fill(string_array,"");
    
    for(int i = 0; i < args.length ; i++){
      string_array[i] = args[i].replaceAll("\\\\", "\\\\\\\\").toString();
    }

    System.out.println("public class TextBlocksTest {");
    System.out.println("  public static void main(String[] args) {");
    System.out.println("    var blockdata1 = \"\"\"");
    System.out.println("      " + string_array[0]);
    System.out.println("      " + string_array[1]);
    System.out.println("      " + string_array[2]);
    System.out.println("      " + string_array[3]);
    System.out.println("      " + string_array[4]);
    System.out.println("    \"\"\";");
    System.out.println("    System.out.println(blockdata1);");
    System.out.println("");    
    System.out.println("    var blockdata2 = \"\"\"");
    System.out.println("    " + string_array[0]);
    System.out.println("     " + string_array[1]);
    System.out.println("      " + string_array[2]);
    System.out.println("       " + string_array[3]);
    System.out.println("        " + string_array[4]);
    System.out.println("    \"\"\";");
    System.out.println("    System.out.println(blockdata2);");
    System.out.println("");
    System.out.println("    var blockdata3 = \"\"\"");
    System.out.println("      " + string_array[0] + "\\");
    System.out.println("      " + string_array[1] + "\\");
    System.out.println("      " + string_array[2] + "\\");
    System.out.println("      " + string_array[3] + "\\");
    System.out.println("      " + string_array[4] + "\\");
    System.out.println("    \"\"\";");
    System.out.println("    System.out.println(blockdata3);");
    System.out.println("    System.out.println(\"\");");
    System.out.println("");    
    System.out.println("    var blockdata4 = \"\"\"");
    System.out.println("      \"" + string_array[0] + "\"");
    System.out.println("      \"" + string_array[1] + "\"");
    System.out.println("      \"" + string_array[2] + "\"");
    System.out.println("      \"" + string_array[3] + "\"");
    System.out.println("      \"" + string_array[4] + "\"");
    System.out.println("    \"\"\";");
    System.out.println("    System.out.println(blockdata4);");
    System.out.println("");    
    System.out.println("    var blockdata5 = \"\"\"");
    System.out.println("      \\s" + string_array[0]);
    System.out.println("      \\s\\s" + string_array[1]);
    System.out.println("      \\s\\s\\s" + string_array[2]);
    System.out.println("      \\s\\s\\s\\s" + string_array[3]);
    System.out.println("      \\s\\s\\s\\s\\s" + string_array[4]);
    System.out.println("    \"\"\";");
    System.out.println("    System.out.println(blockdata5);");
    System.out.println("");    
    System.out.println("    var blockdata6 = \"\"\"");
    System.out.println("      " + string_array[1]);
    System.out.println("      %s");
    System.out.println("      " + string_array[2]);
    System.out.println("      " + string_array[3]);
    System.out.println("      " + string_array[4]);
    System.out.println("    \"\"\".formatted(\"" + string_array[0] + "\");");
    System.out.println("    System.out.println(blockdata6);");
    System.out.println("  }");
    System.out.println("}");
  }
}
