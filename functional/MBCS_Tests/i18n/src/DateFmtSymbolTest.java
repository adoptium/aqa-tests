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
 * DateFmtSymbolTest.java
 */
import java.text.*;
import java.util.*;
public class DateFmtSymbolTest {
    
    public DateFmtSymbolTest() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        SimpleDateFormat sdf = (SimpleDateFormat)df;
        System.out.println(sdf.format(new Date()));
        System.out.println(sdf.toPattern());
        DateFormatSymbols dfs = sdf.getDateFormatSymbols();
        //DateFormatSymbols dfs = new DateFormatSymbols();
        System.out.println(dfs.getLocalPatternChars());
    }
    
    public static void main(String[] args) {
        new DateFmtSymbolTest();
    }
}
