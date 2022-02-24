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

    var recordTestSource =
      """
      import java.util.*;
      import junit.framework.*;

      public class RecordTest extends TestCase {
        record %1$s_r(%1$s %1$s) {
          public %1$s_r {
            if (%1$s.getIntVal() < 1) {
              %1$s.setIntVal(10);
            }
          }

          public %1$s_r() {
            this(new %1$s(100));
          }

          public %1$s_r %1$s_method() {
            return new %1$s_r(new %1$s(1));
          }
        }

        public static class %1$s {
          private int n;

          public %1$s(int n) {
            this.n = n;
          }

          public int getIntVal() {
            return this.n;
          }

          public void setIntVal(int n) {
            this.n = n;
          }
        }

        public void testMethod() {
          var recordInstance1 = new %1$s_r(new %1$s(0));
          var recordInstance2 = new %1$s_r();

          var expected = 10;
          var result = recordInstance1.%1$s().getIntVal();
          assertEquals(expected,result);

          expected = 100;
          result = recordInstance2.%1$s().getIntVal();
          assertEquals(expected,result);

          expected = 1;
          result = recordInstance1.%1$s_method().%1$s().getIntVal();
          assertEquals(expected,result);

          expected = 1;
          result = recordInstance2.%1$s_method().%1$s().getIntVal();
          assertEquals(expected,result);

          var expectedStr = "%1$s_r[%1$s=RecordTest$%1$s";
          var resultStr = recordInstance1.toString().replaceAll("@[^@]*$","");
          assertEquals(expectedStr,resultStr);

          resultStr = recordInstance2.toString().replaceAll("@[^@]*$","") ;
          assertEquals(expectedStr,resultStr);

          assertFalse(recordInstance1.equals(recordInstance2));
       }
      }
      """.formatted(validString);

    System.out.print(recordTestSource);
  }
}

