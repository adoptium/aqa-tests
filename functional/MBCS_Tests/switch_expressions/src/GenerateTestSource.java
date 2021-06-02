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
    StringJoiner sj = new StringJoiner("\",\"", "\"", "\"");
    Arrays.stream(args)
          .map(line -> line.replaceAll("\\\\", "\\\\\\\\"))
          .forEach(s -> sj.add(s));
    String arrayString = sj.toString();

    System.out.println("import java.util.*;");
    System.out.println("import junit.framework.*;");
    System.out.println("");
    System.out.println("public class SwitchExpressionsTest extends TestCase {");
    System.out.println("");  
    System.out.println("  private String[] validarray = {"+arrayString+"};");
    System.out.println("");
    System.out.println("  public void testGetSwitchValue_0() {");
    System.out.println("    var expected = 0;");
    System.out.println("    for(String validstring : validarray) {");
    System.out.println("      var switchretvalue = switch(validstring) {");
    System.out.println("        case "+arrayString+":");
    System.out.println("          yield 0;");
    System.out.println("        default:");
    System.out.println("          yield 1;");
    System.out.println("      };");
    System.out.println("      assertEquals(expected,switchretvalue);");
    System.out.println("    }");
    System.out.println("  }");
    System.out.println(""); 
    System.out.println("  public void testGetSwitchValue_1() {");
    System.out.println("    var expected = 1;");
    System.out.println("    var switchretvalue = switch(\"DEFAULT\") {");
    System.out.println("      case "+arrayString+":");
    System.out.println("        yield 0;");
    System.out.println("      default:");
    System.out.println("        yield 1;");
    System.out.println("    };");
    System.out.println("    assertEquals(expected,switchretvalue);");
    System.out.println("  }");
    System.out.println("");  
    System.out.println("  public void testGetSwitchValueLabmda_0() {");
    System.out.println("    var expected = 0;");
    System.out.println("    for(String validstring : validarray) {");
    System.out.println("      var switchretvalue = switch(validstring) {");
    System.out.println("        case "+arrayString+" -> 0;");
    System.out.println("        default -> 1;");
    System.out.println("      };");
    System.out.println("      assertEquals(expected,switchretvalue);");
    System.out.println("     };");
    System.out.println("  }");
    System.out.println("");  
    System.out.println("  public void testGetSwitchValueLabmda_1() {");
    System.out.println("    var expected = 1;");
    System.out.println("    var switchretvalue = switch(\"DEFAULT\") {");
    System.out.println("      case "+arrayString+" -> 0;");
    System.out.println("      default -> 1;");
    System.out.println("    };");
    System.out.println("    assertEquals(expected,switchretvalue);");
    System.out.println("  }");
    System.out.println("}");
  }
}
