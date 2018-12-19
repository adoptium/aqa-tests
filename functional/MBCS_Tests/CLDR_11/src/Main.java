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

import java.util.Locale;

public class Main {
  public static void main(String[] args) throws Exception {
    String provider = System.getProperty("java.locale.providers","DEFAULT");
    String fmt = "%s-"+Locale.getDefault().toLanguageTag()+"-"+provider+".log";
    System.setOut(new java.io.PrintStream(String.format(fmt,"DecimalFormatSymbolsTest")));
    System.out.println("<<< DecimalFormatSymbolsTest >>>");
    DecimalFormatSymbolsTest.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"DateFormatSymbolsTest")));
    System.out.println("<<< DateFormatSymbolsTest >>>");
    DateFormatSymbolsTest.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"DecimalStyleTest")));
    System.out.println("<<< DecimalStyleTest >>>");
    DecimalStyleTest.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"CurrencyTest")));
    System.out.println("<<< CurrencyTest >>>");
    CurrencyTest.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"LocaleTest")));
    System.out.println("<<< LocaleTest >>>");
    LocaleTest.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"TimeZoneTestA")));
    System.out.println("<<< TimeZoneTestA >>>");
    TimeZoneTestA.main(args);
    System.setOut(new java.io.PrintStream(String.format(fmt,"TimeZoneTestB")));
    System.out.println("<<< TimeZoneTestB >>>");
    TimeZoneTestB.main(args);
    System.out.close();
  }
}
