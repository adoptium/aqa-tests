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

public class TimeZoneTestA {
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

  static String printable(String s) {
    StringBuilder sb = new StringBuilder();
    for(char c : s.toCharArray()) {
      if (ce0.canEncode(c) && ce1.canEncode(c) && ce2.canEncode(c)) {
        sb.append(c);
      } else {
        sb.append(String.format("\\u%04X",(int)c));
      }
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    TreeMap<Integer,ArrayList<TimeZone>> map = new TreeMap<Integer,ArrayList<TimeZone>>(new Comparator<Integer>(){
      public int compare(Integer i1, Integer i2) {
        return i2.compareTo(i1);
      }
    });
    for(String id : TimeZone.getAvailableIDs()) {
      TimeZone tz = TimeZone.getTimeZone(id);
      ArrayList<TimeZone> list = null;
      Integer offset = Integer.valueOf(tz.getRawOffset());
      if (map.containsKey(offset)) {
        list = map.get(offset);
      } else {
        list = new ArrayList<TimeZone>();
        map.put(offset, list);
      }
      list.add(tz);
    }
    for(ArrayList<TimeZone> list : map.values()) {
      list.sort((tz1, tz2)->{
          String s1 = tz1.getDisplayName(false, TimeZone.SHORT)+"\t"+tz1.getID();
          String s2 = tz2.getDisplayName(false, TimeZone.SHORT)+"\t"+tz2.getID();
          return s1.compareTo(s2);
      });
      for(TimeZone tz: list) {
        if (tz.useDaylightTime()) {
          if (tz.getDisplayName(false, TimeZone.SHORT).startsWith("GMT") &&
              tz.getDisplayName(true, TimeZone.SHORT).startsWith("GMT")) {
            continue;
          }
          System.out.println(tz.getRawOffset()+"\t"+tz.getID()
            +"\t"+tz.getDisplayName(false, TimeZone.SHORT)
            +"\t"+tz.getDisplayName(true, TimeZone.SHORT));
        } else {
          if (tz.getDisplayName(false, TimeZone.SHORT).startsWith("GMT")) continue;
          if (tz.getDisplayName(false, TimeZone.SHORT).startsWith("UTC")) continue;
          System.out.println(tz.getRawOffset()+"\t"+tz.getID()
            +"\t"+tz.getDisplayName(false, TimeZone.SHORT));
        }
      }
    }
  }
}
