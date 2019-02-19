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

public class CurrencyTest {
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
    Hashtable<Locale,Currency> map = new Hashtable<Locale,Currency>();
    for(Locale l : Locale.getAvailableLocales()) {
      try {
        Currency currency = Currency.getInstance(l);
        map.put(l, currency);
      } catch (IllegalArgumentException iae) {
      }
    }
    List<Locale> list = Arrays.asList(map.keySet().toArray(new Locale[0]));
    Collections.sort(list, new Comparator<Locale>(){
      public int compare(Locale l1, Locale l2) {
        return l1.getCountry().compareTo(l2.getCountry());
      }
    });
    String country = "";
    String code = "";
    String symbol = "";
    for(Locale l : list) {
        if (country != l.getDisplayCountry()){
            System.out.println(printable(l.getDisplayCountry()+"\t"+
                map.get(l).getCurrencyCode()+"\t"+printable(map.get(l).getSymbol())));
            country = l.getDisplayCountry();
            code = map.get(l).getCurrencyCode();
            symbol = map.get(l).getSymbol();
        }else{
            if ( code != map.get(l).getCurrencyCode() ||
                 symbol != map.get(l).getSymbol()){
                System.out.println("Error different code:"+ l + "\t" +
                    map.get(l).getCurrencyCode() + "\t" +  
                    map.get(l).getSymbol());
            }
        }
    }
  }
}
