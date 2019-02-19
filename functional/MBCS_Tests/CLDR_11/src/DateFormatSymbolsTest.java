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

import java.text.*;
import java.util.*;
import java.nio.charset.*;

public class DateFormatSymbolsTest {
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
    DateFormatSymbols dfs = new DateFormatSymbols();
    System.out.println(printable("AmPmStrings: "+Arrays.toString(dfs.getAmPmStrings())));
    System.out.println(printable("Eras: "+Arrays.toString(dfs.getEras())));
    System.out.println(printable("Months: "+Arrays.toString(dfs.getMonths())));
    System.out.println(printable("ShortMonths: "+Arrays.toString(dfs.getShortMonths())));
    System.out.println(printable("ShortWeekdays: "+Arrays.toString(dfs.getShortWeekdays())));
    System.out.println(printable("Weekdays: "+Arrays.toString(dfs.getWeekdays())));
    System.out.println(printable("LocalPatternChars: "+dfs.getLocalPatternChars()));
    String[] styleNames = new String[]{"DEFAULT", "FULL", "LONG", "MEDIUM", "SHORT"};
    int[] styles = new int[]{DateFormat.DEFAULT, DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};
    for(int i=0; i<styles.length; i++) {
      SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateTimeInstance(styles[i],styles[i]);
      System.out.print("DateTime:");
      System.out.print(styleNames[i]+": toLocalizedPattern="+sdf.toLocalizedPattern());
      System.out.println(printable(", toPattern="+sdf.toPattern()));
      sdf = (SimpleDateFormat)DateFormat.getDateInstance(styles[i]);
      System.out.print("Date:");
      System.out.print(styleNames[i]+": toLocalizedPattern="+sdf.toLocalizedPattern());
      System.out.println(printable(", toPattern="+sdf.toPattern()));
      sdf = (SimpleDateFormat)DateFormat.getTimeInstance(styles[i]);
      System.out.print("Time:");
      System.out.print(styleNames[i]+": toLocalizedPattern="+sdf.toLocalizedPattern());
      System.out.println(printable(", toPattern="+sdf.toPattern()));
    }
  }
}
