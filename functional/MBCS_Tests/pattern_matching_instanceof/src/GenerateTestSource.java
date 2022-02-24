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

public class GenerateTestSource {

  public static void main (String[] args) {
    if (args.length < 1) System.exit(1);
    StringBuffer valid = new StringBuffer();
    for (char ch : String.join("", args).toCharArray()) {
      if (valid.length() == 0) {
        if (Character.isJavaIdentifierStart(ch)) valid.append(ch);
      } else {
        if (Character.isJavaIdentifierPart(ch)) valid.append(ch);
      }
    }
    String validString = valid.toString();

    var patternMatchingInstanceOfSource =
      """
      import java.util.*;
      import junit.framework.*;

      public class PatternMatchingInstanceOfTest extends TestCase {

        public static class %1$s {
          public int %1$s_method() {
            return 0;
          }
        }

        public void testMethod() {
          Object obj = new %1$s();
          var expected = 0;
          var result = 1;

          if (obj instanceof %1$s %1$s) {
            result = %1$s.%1$s_method();
          }

          assertEquals(expected,result);
        }
      }
      """.formatted(validString);

    System.out.print(patternMatchingInstanceOfSource);
  }
}
