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
import java.lang.reflect.*;

public class DecimalFormatSymbolsTest {
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

  static Class<?> NumberFormatICUClass = null;
  static Method unwrap = null;
  static Class<?> ICU_DecimalFormatClass = null;
  static Method toLocalizedPattern = null;
  static Method toPattern = null;

  static {
    try {
      NumberFormatICUClass = Class.forName("com.ibm.icu.impl.jdkadapter.NumberFormatICU");
      unwrap = NumberFormatICUClass.getDeclaredMethod("unwrap", (Class<?>[]) null);
      ICU_DecimalFormatClass = Class.forName("com.ibm.icu.text.DecimalFormat");
      toLocalizedPattern = ICU_DecimalFormatClass.getDeclaredMethod("toLocalizedPattern", (Class<?>[]) null);
      toPattern = ICU_DecimalFormatClass.getDeclaredMethod("toPattern", (Class<?>[]) null);
    } catch (Exception e) { }
  }

  static void printNumberFormatPattern(String type, NumberFormat nf) {
     if (nf instanceof DecimalFormat) {
       DecimalFormat df = (DecimalFormat) nf;
       System.out.println(printable(type+": toLocalizedPattern="+df.toLocalizedPattern()+", toPattern="+df.toPattern()));
     } else if (null != NumberFormatICUClass && null != ICU_DecimalFormatClass && NumberFormatICUClass.isInstance(nf)) {
       try {
         Object obj0 = unwrap.invoke(NumberFormatICUClass.cast(nf), (Object[])null);
         Object obj1 = toLocalizedPattern.invoke(ICU_DecimalFormatClass.cast(obj0), (Object[])null);
         Object obj2 = toPattern.invoke(ICU_DecimalFormatClass.cast(obj0), (Object[])null);
         System.out.println(printable(type+": toLocalizedPattern="+obj1+", toPattern="+obj2));
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
  }

  public static void main(String[] args) {
    DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
    System.out.println(printable("ZeroDigit="+dfs.getZeroDigit()));
    System.out.println(printable("GroupingSeparator="+dfs.getGroupingSeparator()));
    System.out.println(printable("DecimalSeparator="+dfs.getDecimalSeparator()));
    System.out.println(printable("PerMill="+dfs.getPerMill()));
    System.out.println(printable("Percent="+dfs.getPercent()));
    System.out.println(printable("Digit="+dfs.getDigit()));
    System.out.println(printable("PatternSeparator="+dfs.getPatternSeparator()));
    System.out.println(printable("MinusSign="+dfs.getMinusSign()));
    System.out.println(printable("MonetaryDecimalSeparator="+dfs.getMonetaryDecimalSeparator()));
    System.out.println(printable("Infinity="+dfs.getInfinity()));
    System.out.println(printable("NaN="+dfs.getNaN()));
    System.out.println(printable("CurrencySymbol="+dfs.getCurrencySymbol()));
    System.out.println(printable("InternationalCurrencySymbol="+dfs.getInternationalCurrencySymbol()));
    System.out.println(printable("ExponentSeparator="+dfs.getExponentSeparator()));
    printNumberFormatPattern("General", NumberFormat.getInstance());
    printNumberFormatPattern("Number", NumberFormat.getNumberInstance());
    printNumberFormatPattern("Integer", NumberFormat.getIntegerInstance());
    printNumberFormatPattern("Currency", NumberFormat.getCurrencyInstance());
    printNumberFormatPattern("Percent", NumberFormat.getPercentInstance());
  }
}
