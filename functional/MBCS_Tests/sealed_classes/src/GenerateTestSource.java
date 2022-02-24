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

  public static String[] generateArrayString(String[] args, String prefix, int size) {
    String[] arrayString = new String[size];
    String line;
    char ch;
    for (int j = 0; j < args.length ; j++){
      line = args[j];
      StringBuffer valid = new StringBuffer();
      for (int i = 0; i < line.length(); i++) {
        ch = line.charAt(i);
        if (valid.length() == 0) {
          if (Character.isJavaIdentifierStart(ch)) valid.append(ch);
        } else {
          if (Character.isJavaIdentifierPart(ch)) valid.append(ch);
        }
      }
      if (valid.length() == 0) {
        arrayString[j] = prefix + j;
      } else {
        arrayString[j] = valid.toString();
      }
    }
    for (int i = size - 1; i > args.length - 1; i--) {
      arrayString[i] = prefix + i;
    }
    return arrayString;
  }

  public static void main (String[] args) throws Exception {
    if (args.length < 1) System.exit(1);
    String[] arrayString = generateArrayString(args, "ChildClass", 5);

    var sealedClassTestSource =
      """
      import java.util.*;
      import junit.framework.*;

      public class SealedClassTest extends TestCase {
        public void testMethod() {
          %1$s p;
          var expected = 0;

          expected = 1;
          p = new %2$s();
          assertEquals(expected,p.getIntValue());

          expected = 2;
          p = new %3$s();
          assertEquals(expected,p.getIntValue());

          expected = 3;
          p = new %4$s();
          assertEquals(expected,p.getIntValue());

          expected = 4;
          p = new %5$s();
          assertEquals(expected,p.getIntValue());
        }
      }

      sealed abstract class %1$s permits %2$s, %3$s, %4$s, %5$s {
         public abstract int %1$s_method();

         public int getIntValue() {
           return %1$s_method();
         }
      }

      final class %2$s extends %1$s {
        public int %1$s_method() {
          return 1;
        }
      }

      final class %3$s extends %1$s {
        public int %1$s_method() {
          return 2;
        }
      }

      final class %4$s extends %1$s {
        public int %1$s_method() {
          return 3;
        }
      }

      final class %5$s extends %1$s {
        public int %1$s_method() {
          return 4;
        }
      }
      """;

    System.setOut(new PrintStream(new File("SealedClassTest.java")));
    System.out.print(String.format(sealedClassTestSource, (Object[])arrayString));

    var sealedClassCETestSource = sealedClassTestSource
        .replaceAll("SealedClassTest", "SealedClassCETest")
        .replaceAll("permits %2\\$s,", "permits");
    System.setOut(new PrintStream(new File("SealedClassCETest.java")));
    System.out.print(String.format(sealedClassCETestSource, (Object[])arrayString));

    var sealedClassSbcsTestSource = sealedClassCETestSource
        .replaceAll("SealedClassCETest", "SealedClassSbcsTest");
    System.setOut(new PrintStream(new File("SealedClassSbcsTest.java")));
    System.out.print(String.format(sealedClassSbcsTestSource, "STRING0", "STRING1", "STRING2", "STRING3", "STRING4"));
  }
}
