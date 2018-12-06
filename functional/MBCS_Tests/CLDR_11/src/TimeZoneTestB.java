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

import java.util.*;
import java.nio.charset.*;

public class TimeZoneTestB {
  static CharsetEncoder ce1 = null;
  static CharsetEncoder ce2 = null;

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

  static String printable(String s) {
    StringBuilder sb = new StringBuilder();
    for(char c : s.toCharArray()) {
      if (ce1.canEncode(c) && ce2.canEncode(c)) {
        sb.append(c);
      } else {
        sb.append(String.format("\\u%04X",(int)c));
      }
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    TreeMap<String, String> map = new TreeMap<String, String>();
    for(String id : TimeZone.getAvailableIDs()) {
      TimeZone tz = TimeZone.getTimeZone(id);
      boolean useDST = tz.useDaylightTime();
      StringBuilder sb = new StringBuilder();
      if (useDST) {
        sb.append(tz.getDisplayName(false, TimeZone.LONG, Locale.ENGLISH)).append("\t")
          .append(tz.getDisplayName(true,  TimeZone.LONG, Locale.ENGLISH)).append("\t")
          .append(tz.getDisplayName(false, TimeZone.LONG)).append("\t")
          .append(tz.getDisplayName(true,  TimeZone.LONG));
      } else {
        sb.append(tz.getDisplayName(false, TimeZone.LONG,Locale.ENGLISH)).append("\t")
          .append(tz.getDisplayName(false, TimeZone.LONG));
      }
      map.put(id, sb.toString());
    }
    for(String id : map.keySet()) {
      System.out.println(id+"\t"+map.get(id));
    }
  }
}
