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

public class FormatterTest_zh_tw{

    // TODO
    // Please add Minguo Calendar testcases

    @Test
    public void formatterTestLocal(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        assertEquals("2013-04-30", date.toString());

        // G
        str = date.format(DateTimeFormatter.ofPattern(
                          "Gy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u51432013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u51432013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u51432013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u51432013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("A2013\u5e744\u670830\u65e5", str);

        // y M d
        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyy\u5e74MM\u6708dd\u65e5"));
        assertEquals("A13\u5e7404\u670830\u65e5", str);

        try{
            str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyy\u5e74MMMddd\u65e5"));
            fail("d should be one or two letter");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyy\u5e74MMM"));
        assertEquals("A2013\u5e74\u56db\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyy\u5e74MMMM"));
        assertEquals("A2013\u5e74\u56db\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyyy\u5e74MMMMM\u6708"));
        assertEquals("A02013\u5e744\u6708", str);

    }

    @Test
    public void localizedLocalTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        assertEquals("2013\u5e744\u670830\u65e5 \u661f\u671f\u4e8c", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        assertEquals("2013/4/30", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        assertEquals("2013/4/30", str);
    }

    @Test
    public void localizedDateTimeLocalTest(){
        LocalDateTime dt = LocalDateTime.of(2013, 4, 30, 12, 0);
        String str;

        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
            fail("FULL needs timezone");
        }catch(DateTimeException e){
        }

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5 \u4e0b\u534812\u664200\u520600\u79d2", str);

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/4/30 \u4e0b\u5348 12:00:00", str);

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/4/30 \u4e0b\u5348 12:00", str);
    }

    @Test
    public void localizedDateTimeLocalTZTest(){
        ZonedDateTime zdt = ZonedDateTime.of(2013,4,30, 12,0,0,0, ZoneId.of("Asia/Taipei"));
        String str;

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
        assertEquals("2013\u5e744\u670830\u65e5 \u661f\u671f\u4e8c \u4e0b\u534812\u664200\u520600\u79d2 TST", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5 \u4e0b\u534812\u664200\u520600\u79d2", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/4/30 \u4e0b\u5348 12:00:00", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/4/30 \u4e0b\u5348 12:00", str);
    }

    @Test
    public void localizedTimeLocalTest(){
        LocalTime time = LocalTime.of(12, 0);
        String str;

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        assertEquals("\u4e0b\u5348 12:00", str);

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
        assertEquals("\u4e0b\u5348 12:00:00", str);

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
        assertEquals("\u4e0b\u534812\u664200\u520600\u79d2", str);

        try{
            str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
            fail("FULL needs tiemzone");
        }catch(DateTimeException e){
        }
    }

}
