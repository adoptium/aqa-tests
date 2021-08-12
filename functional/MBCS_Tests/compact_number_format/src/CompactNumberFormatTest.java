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

public class CompactNumberFormatTest {
  private static final long[] LONG_ARRAY = {10000, 100000000, 1000000000000L, 
                                            10000000000000000L, 19400, 19500, 
                                            19600, 19940, 19950, 19960, 19994, 
                                            19995, 19996};
  private static final int MAX_FRACTION_DIGITS = 4;
  
  private NumberFormat compactNumberFormat;
  

  public CompactNumberFormatTest() { 
    compactNumberFormat = NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.LONG);
  }

  private Number cnfParse(String cnf_string) {
    try {
      return compactNumberFormat.parse(cnf_string);
    } catch (ParseException pe) {
      System.out.println(pe.getMessage());
      return null;
    }
  }
  
  private void testMaximumFractionDigits(long value, int fraction_digits) {
    compactNumberFormat.setMaximumFractionDigits(fraction_digits);
    String formatted = compactNumberFormat.format(value);
    Number parsed = cnfParse(formatted);
    System.out.println("setMaximumFractionDigits : " + fraction_digits 
                     + " input : " + value + " ===> FORMAT : [" 
                     + formatted + "] ===> PARSE : [" + parsed +"]");
  }

  private void testMinimumFractionDigits(long value, int fraction_digits) {
    compactNumberFormat.setMinimumFractionDigits(fraction_digits);
    String formatted = compactNumberFormat.format(value);
    Number parsed = cnfParse(formatted);
    System.out.println("setMinimumFractionDigits : " + fraction_digits 
                     + " input : " + value + " ===> FORMAT : ["
                     + formatted + "] ===> PARSE : [" + parsed +"]");
  }


  public static void main(String[] args) {
    CompactNumberFormatTest cnft = new CompactNumberFormatTest();
    String sep = String.join("", Collections.nCopies(5, "+")); 
    
    for(int i = 0; i < LONG_ARRAY.length; i++) {
      for(int j = 1; j <= MAX_FRACTION_DIGITS; j++) {
        cnft.testMaximumFractionDigits(LONG_ARRAY[i],j);
      }
      System.out.println(sep);
    }

    for(int i = 0; i < LONG_ARRAY.length; i++) {
      for(int j = 1; j <= MAX_FRACTION_DIGITS; j++) {
        cnft.testMinimumFractionDigits(LONG_ARRAY[i],j);
      }
      System.out.println(sep);
    }
  }
}
