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

// This testcase works on Java11 and later.

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.time.chrono.*;

public class FormatterBuilderTest_ja_jp{
    @Test
    public void yearTest(){
        JapaneseDate date = JapaneseDate.of(JapaneseEra.HEISEI, 1, 3, 18);
        String str;
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        Map<Long, String> yearMap = new HashMap<>();
        yearMap.put(1L, "\u5143");

        Map<Long, String> monthMap = new HashMap<>();
        monthMap.put(1L, "\u7766\u6708");
        monthMap.put(2L, "\u5982\u6708");
        monthMap.put(3L, "\u5f25\u751f");
        monthMap.put(4L, "\u536f\u6708");
        monthMap.put(5L, "\u7690\u6708");
        monthMap.put(6L, "\u6c34\u7121\u6708");
        monthMap.put(7L, "\u6587\u6708");
        monthMap.put(8L, "\u8449\u6708");
        monthMap.put(9L, "\u9577\u6708");
        monthMap.put(10L, "\u795e\u7121\u6708");
        monthMap.put(11L, "\u971c\u6708");
        monthMap.put(12L, "\u5e2b\u8d70");

        builder.appendText(ChronoField.ERA, TextStyle.FULL);
        builder.appendText(ChronoField.YEAR_OF_ERA, yearMap);
        builder.appendLiteral('\u5e74');
        builder.appendText(ChronoField.MONTH_OF_YEAR, monthMap);
        builder.appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL);
        builder.appendLiteral('\u65e5');

        DateTimeFormatter formatter = builder.toFormatter(Locale.JAPANESE);
      
        assertEquals("Japanese Heisei 1-03-18", date.toString());

        str = date.format(formatter);
        assertEquals("\u5e73\u6210\u5143\u5e74\u5f25\u751f18\u65e5", str);

        date = date.plus(1, ChronoUnit.MONTHS);
        str = date.format(formatter);
        assertEquals("\u5e73\u6210\u5143\u5e74\u536f\u670818\u65e5", str);

        date = date.plus(1, ChronoUnit.YEARS);
        str = date.format(formatter);
        assertEquals("\u5e73\u62102\u5e74\u536f\u670818\u65e5", str);
    }
}
