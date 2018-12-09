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

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.time.chrono.*;

public class FormatterBuilderTest{
    @Test
    public void yearTest(){
        LocalDate date = LocalDate.of( 1, 3, 18);
        String str;
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        Map<Long, String> yearMap = new HashMap<>();
        yearMap.put(1L, "1st");

        Map<Long, String> monthMap = new HashMap<>();
        monthMap.put(1L, "JNY");
        monthMap.put(2L, "FBY");
        monthMap.put(3L, "MCH");

        builder.appendText(ChronoField.ERA, TextStyle.FULL);
        builder.appendText(ChronoField.YEAR_OF_ERA, yearMap);
        builder.appendLiteral('/');
        builder.appendText(ChronoField.MONTH_OF_YEAR, monthMap);
        builder.appendLiteral('/');
        builder.appendText(ChronoField.DAY_OF_MONTH, TextStyle.FULL);

        DateTimeFormatter formatter = builder.toFormatter(Locale.US);
      
        assertEquals("0001-03-18", date.toString());

        str = date.format(formatter);
        assertEquals("Anno Domini1st/MCH/18", str);

        date = date.plusMonths(1);
        str = date.format(formatter);
        assertEquals("Anno Domini1st/4/18", str); // 4 is not defined in monthMap

        date = date.plusYears(2000);
        str = date.format(formatter);
        assertEquals("Anno Domini2001/4/18", str);
    }
    @Test
    public void DecimalStyleTest(){
        ZonedDateTime date = ZonedDateTime.of( -1, 4, 17, 12, 30, 15, 123,
                                 ZoneId.of("JST", ZoneId.SHORT_IDS));
        DateTimeFormatter baseFormatter = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 2, 4,
                 SignStyle.EXCEEDS_PAD)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR)
            .appendFraction(ChronoField.SECOND_OF_MINUTE, 2, 3, true)
            .appendLiteral(' ')
            .appendOffset("+HH:mm", "no offset")
            .toFormatter();

        String str = date.format(baseFormatter);
        // 15 second = 1/4 = .25
        assertEquals("-01/4/17 12:30.25 +09:18", str);

        DecimalStyle style = DecimalStyle.STANDARD;
        assertEquals("DecimalStyle[0+-.]", style.toString());

        // Replaced Japanese Full-Width characters
        DecimalStyle modify = style.withDecimalSeparator('\uff0e')
                                   .withNegativeSign('\uff0d')
                                   .withPositiveSign('\uff0b')
                                   .withZeroDigit('\uff10');
        assertEquals("DecimalStyle[\uff10\uff0b\uff0d\uff0e]",
                     modify.toString());

        DateTimeFormatter modFormatter = baseFormatter.withDecimalStyle(modify);
        str = date.format(modFormatter);
        assertEquals("\uff0d\uff10\uff11/\uff14/\uff11\uff17 \uff11\uff12:\uff13\uff10\uff0e\uff12\uff15 +09:18", str);
        // TimeZone is not effected?

        date = date.plusYears(2015);
        str = date.format(baseFormatter);
        assertEquals("+2014/4/17 12:30.25 +09", str);

        str = date.format(modFormatter);
        assertEquals("\uff0b\uff12\uff10\uff11\uff14/\uff14/\uff11\uff17 \uff11\uff12:\uff13\uff10\uff0e\uff12\uff15 +09", str);
    }
}
