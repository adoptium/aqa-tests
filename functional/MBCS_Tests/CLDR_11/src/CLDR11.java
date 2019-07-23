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
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

public class CLDR11 {
  static CharsetEncoder ce1 = null;
  static CharsetEncoder ce2 = null;
  static CharsetEncoder ce0 = Charset.defaultCharset().newEncoder();

  static {
    Locale loc = Locale.getDefault();
    {
      if (Locale.CHINA.equals(loc)) {
        ce1 = Charset.forName("GB2312").newEncoder();
        ce2 = Charset.forName("MS936").newEncoder();
      } else if (Locale.JAPAN.equals(loc)) {
        ce1 = Charset.forName("Shift_JIS").newEncoder();
        ce2 = Charset.forName("MS932").newEncoder();
      } else if (Locale.KOREA.equals(loc)) {
        ce1 = Charset.forName("EUC-KR").newEncoder();
        ce2 = Charset.forName("MS949").newEncoder();
      } else if (Locale.TAIWAN.equals(loc)) {
        ce1 = Charset.forName("BIG5").newEncoder();
        ce2 = Charset.forName("MS950").newEncoder();
      } else {
        ce1 = Charset.forName("ASCII").newEncoder();
        ce2 = Charset.forName("ASCII").newEncoder();
      }
    }
  }

  static void printtable(String s) {
    StringBuilder sb = new StringBuilder();
    for(char c : s.toCharArray()) {
      if (ce0.canEncode(c) && ce1.canEncode(c) && ce2.canEncode(c)) {
        sb.append(c);
      } else {
        sb.append(String.format("\\u%04X",(int)c));
      }
    }
    System.out.println(sb.toString());
  }

  public static void main(String[] args) {
    String f = "CLDR11-"+Locale.getDefault().toLanguageTag()+".properties";
    String prefix = "expected_";
    String version = String.format("%03d%03d%03d", JavaVersion.getFeature(),
                                                   JavaVersion.getInterim(),
                                                   JavaVersion.getUpdate());
    try (InputStream is = CLDR11.class.getResourceAsStream(f);) {
      Properties prop = new Properties();
      prop.load(is);
      String[] files = String.valueOf(prop.getProperty("files")).split(" ");
      String[] versions = String.valueOf(prop.getProperty("versions","")).split(",");
      for(String s : files) {
        String[] fn = s.split("-");
        int pos;
        String key = s;
        for(pos = versions.length - 1; pos > -1; pos--) {
          if (versions[pos].compareTo(version) <= 0) {
            key = s+"."+versions[pos];
            if (!"unknown".equals(prop.getProperty(key+".patch", "unknown"))) {
              break;
            }
          }
        }
        System.setOut(new java.io.PrintStream(prefix+fn[0]+"-"+Locale.getDefault().toLanguageTag()+"-"+fn[1]+".log"));
        if (-1 == pos) {
          int max = Integer.parseInt(String.valueOf(prop.getProperty(s+".0","0")));
          for(int i=1; i<=max; i++) {
            printtable(prop.getProperty(s+"."+i)+"");
          }
        } else {
          Pattern pat = Pattern.compile("(.*):(\\d+)\\.\\.(\\d+)");
          for(String patch : prop.getProperty(key+".patch").split(",")) {
            Matcher mat = pat.matcher(patch);
            if (mat.matches()) {
              String k = mat.group(1);
              int min = Integer.parseInt(mat.group(2));
              int max = Integer.parseInt(mat.group(3));
              k =  0 == k.length() ? s : s+"."+k;
              for(int i=min; i<=max; i++) {
                printtable(prop.getProperty(k+"."+i)+"");
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
