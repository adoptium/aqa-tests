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

public class CLDR11 {
  public static void main(String[] args) {
    String f = "CLDR11-"+Locale.getDefault().toLanguageTag()+".properties";
    String prefix = "expected_";
    try (InputStream is = CLDR11.class.getResourceAsStream(f);) {
      Properties prop = new Properties();
      prop.load(is);
      String[] files = String.valueOf(prop.getProperty("files")).split(" ");
      for(String s : files) {
        String[] fn = s.split("-");
        int max = Integer.parseInt(String.valueOf(prop.getProperty(s+".0")));
        System.setOut(new java.io.PrintStream(prefix+fn[0]+"-"+Locale.getDefault().toLanguageTag()+"-"+fn[1]+".log"));
        for(int i=1; i<=max; i++) {
          System.out.println(prop.getProperty(s+"."+i)+"");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
