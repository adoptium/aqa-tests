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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;

public class FormatterTest_ko_kr{

    @Test
    public void formatterTestLocal(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        assertEquals("2013-04-30", date.toString());

        // G
        str = date.format(DateTimeFormatter.ofPattern(
                          "Gy\ub144M\uc6d4d\uc77c"));
        assertEquals("\uc11c\uae302013\ub1444\uc6d430\uc77c", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGy\ub144M\uc6d4d\uc77c"));
        assertEquals("\uc11c\uae302013\ub1444\uc6d430\uc77c", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGy\ub144M\uc6d4d\uc77c"));
        assertEquals("\uc11c\uae302013\ub1444\uc6d430\uc77c", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGy\ub144M\uc6d4d\uc77c"));
        assertEquals("\uc11c\uae302013\ub1444\uc6d430\uc77c", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGy\ub144M\uc6d4d\uc77c"));
        assertEquals("AD2013\ub1444\uc6d430\uc77c", str);

        // y M d
        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyy\ub144MM\uc6d4dd\uc77c"));
        assertEquals("AD13\ub14404\uc6d430\uc77c", str);

        try{
            str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyy\ub144MMMddd\uc77c")); // 3d
            fail("d should be one or two letter");
        }catch(IllegalArgumentException e) {
        }

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyy\ub144MMMM"));
        assertEquals("AD2013\ub1444\uc6d4", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyy\ub144MMMM"));
        assertEquals("AD2013\ub1444\uc6d4", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyyy\ub144MMMMM"));
        assertEquals("AD02013\ub1444\uc6d4", str);

    }

    @Test
    public void localizedLocalTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        assertEquals("2013\ub144 4\uc6d4 30\uc77c \ud654\uc694\uc77c", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        assertEquals("2013\ub144 4\uc6d4 30\uc77c", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        assertEquals("2013. 4. 30.", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        assertEquals("13. 4. 30.", str);
    }

    @Test
    public void localizedDateTimeLocalTest(){
        LocalDateTime dt = LocalDateTime.of(2013, 4, 30, 12, 0);
        String str;

        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
            fail("Korean FULL time format needs timezone");
        }catch(DateTimeException e){
        }

        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
            fail("Korean LONG time format needs timezone");
        }catch(DateTimeException e){
        }

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013. 4. 30. \uc624\ud6c4 12:00:00", str);

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("13. 4. 30. \uc624\ud6c4 12:00", str);
    }

    @Test
    public void localizedDateTimeLocalTZTest(){
        ZonedDateTime zdt = ZonedDateTime.of(2013,4,30, 12,0,0,0, ZoneId.of("Asia/Seoul"));
        String str;

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
        assertEquals("2013\ub144 4\uc6d4 30\uc77c \ud654\uc694\uc77c \uc624\ud6c4 12\uc2dc 0\ubd84 0\ucd08 \ub300\ud55c\ubbfc\uad6d \ud45c\uc900\uc2dc",str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        assertEquals("2013\ub144 4\uc6d4 30\uc77c \uc624\ud6c4 12\uc2dc 0\ubd84 0\ucd08 KST", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013. 4. 30. \uc624\ud6c4 12:00:00", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("13. 4. 30. \uc624\ud6c4 12:00", str);
    }

    @Test
    public void localizedTimeLocalTest(){
        LocalTime time = LocalTime.of(12, 0);
        String str;

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        assertEquals("\uc624\ud6c4 12:00", str);

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
        assertEquals("\uc624\ud6c4 12:00:00", str);

        try{
            str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
            fail("Korean LONG time format needs timezone");
        }catch(DateTimeException e){
        }

        try{
            str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
            fail("Korean FULL time format needs timezone");
        }catch(DateTimeException e){
        }
    }

}
