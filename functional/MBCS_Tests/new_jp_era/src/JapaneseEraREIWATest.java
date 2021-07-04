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

import java.time.chrono.*;
import java.time.format.*;
import java.util.*;
import junit.framework.*;

public class JapaneseEraREIWATest extends TestCase {
  
  private String encode(String s) {
    StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
          if ('\\' == ch) {
            sb.append("\\");
          } else if (ch >= ' ' && ch <= '~') {
            sb.append(ch);
          } else {
            sb.append(String.format("\\u%04x", (int)ch));
          }
        }
     return sb.toString();
  }

  public void testGetValue() {
    int expected = 3;
    int result = JapaneseEra.REIWA.getValue();
    assertEquals(expected,result);
  }

  public void testGetDisplayName() {
    Locale[] locales = new Locale[] {
      Locale.CHINA, Locale.JAPAN, Locale.KOREA, Locale.TAIWAN, Locale.US
    };
    String[] reiwaStrs = new String[] {
     "\u4ee4\u548c", "\u4ee4\u548c", "\ub808\uc774\uc640", "\u4ee4\u548c", "Reiwa"
    };
    for(int i = 0; i < locales.length; ++i) {
      String result = JapaneseEra.REIWA.getDisplayName(TextStyle.FULL, locales[i]);
      if(!reiwaStrs[i].equals(result)) {
        fail(encode(String.join(" != ", reiwaStrs[i], result)));
      }
    }
  }
}
