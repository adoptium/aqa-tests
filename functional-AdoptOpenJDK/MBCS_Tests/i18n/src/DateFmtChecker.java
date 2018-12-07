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
/*
 * DateFmtChecker.java
 */
import java.util.*;
import java.text.*;
public class DateFmtChecker {
    public static void main(String[] args) {
        DateFormatTest datefmt;
        Locale[] locales = Locale.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            System.out.print(locales[i] + ":   ");
            datefmt = new DateFormatTest(locales[i]);
            datefmt.printDate(DateFormat.FULL);
        }
    }
}

