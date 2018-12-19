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

public class LocaleTest {
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
    System.out.println("Default=["+Locale.getDefault().toString()+", "+Locale.getDefault().toLanguageTag()+"]");
    List<Locale> locList = Arrays.asList(Locale.getAvailableLocales());
    Collections.sort(locList, new Comparator<Locale>(){
      public int compare(Locale l1, Locale l2) {
        return l1.toString().compareTo(l2.toString());
      }
    });
    for(Locale loc : locList) {
      StringBuffer out = new StringBuffer();
      out.append("Name=["+loc.toString()+", "+loc.toLanguageTag()+", "+loc.getDisplayName());
      out.append("], Language=["+loc.getLanguage()+", ");
      try {
        out.append(loc.getISO3Language());
      } catch (MissingResourceException mre) {}
      out.append(", "+loc.getDisplayLanguage());
      out.append("], Country=["+loc.getCountry()+", ");
      try {
        out.append(loc.getISO3Country());
      } catch (MissingResourceException mre) {}
      out.append(", "+loc.getDisplayCountry());
      out.append("], Variant=["+loc.getVariant()+", "+loc.getDisplayVariant());
      out.append("], Script=["+loc.getScript()+", "+loc.getDisplayScript());
      {
        StringBuffer sb = new StringBuffer();
        for(String s : loc.getUnicodeLocaleKeys()) {
          sb.append(", "+s+"="+loc.getUnicodeLocaleType(s));
        }
        if (sb.length() > 2) out.append("], UnicodeLocaleType=["+sb.substring(2).toString());
      }
      {
        StringBuffer sb = new StringBuffer();
        for(String s : loc.getUnicodeLocaleAttributes()) {
          sb.append(", "+s);
        }
        if (sb.length() > 2) out.append("], UnicodeLocaleAttribute=["+sb.substring(2).toString());
      }
      {
        StringBuffer sb = new StringBuffer();
        for(Character c : loc.getExtensionKeys()) {
          sb.append(", "+c+"="+loc.getExtension(c));
        }
        if (sb.length() > 2) out.append("], Extension=["+sb.substring(2).toString());
      }
      out.append("]");
      System.out.println(printable(out.toString()));
    }
    System.out.println("ISOCountries="+printable(Arrays.toString(Locale.getISOCountries())));
    System.out.println("ISOLanguages="+printable(Arrays.toString(Locale.getISOLanguages())));
  }
}
