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
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.lang.management.*;

public class FormatterTest_ja_jp{
/*
    private static boolean ibmVM = false;

    @BeforeClass
    public static void checkVM() {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        String vendor = mxbean.getVmVendor();
        if (vendor.contains("IBM")) {
            ibmVM = true;
            System.out.println("using IBM VM...");
        }
    }
*/

    @Test
    public void formatterTestJP(){
        JapaneseDate date = JapaneseDate.of(2013, 4, 30);
        String str;

        assertEquals("Japanese Heisei 25-04-30", date.toString());

        // G
        str = date.format(DateTimeFormatter.ofPattern(
                            "Gy\u5e74M\u6708d\u65e5"));
        assertEquals("\u5e73\u621025\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "GGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u5e73\u621025\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "GGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u5e73\u621025\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "GGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u5e73\u621025\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "GGGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("H25\u5e744\u670830\u65e5", str);

        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "GGGGGGy\u5e74M\u6708d\u65e5")); // 6 Gs
            fail("GGGGG should be invalid");
        }catch(IllegalArgumentException e){
        }

        // y M d
        date = JapaneseDate.of(2013, 4, 1);
        str = date.format(DateTimeFormatter.ofPattern( // one char
                            "GGGGGyy\u5e74M\u6708d\u65e5"));
        assertEquals("H25\u5e744\u67081\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "GGGGGyy\u5e74MM\u6708dd\u65e5"));
        assertEquals("H25\u5e7404\u670801\u65e5", str);

        try{
            str = date.format(DateTimeFormatter.ofPattern(
                                "GGGGGyyy\u5e74MMM\u6708ddd\u65e5"));
            fail("d should be one or two letter");
        }catch(IllegalArgumentException e){
        }

        // M month; number/text
        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGyyy\u5e74MMM"));
        assertEquals("H025\u5e744\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern( // four letters should be TEXT full
                            "GGGGGyyyy\u5e74MMMM")); // no TSUKI here
        assertEquals("H0025\u5e744\u6708", str);

/*
        str = date.format(DateTimeFormatter.ofPattern( // five letters should be TEXT nallow
                            "GGGGGyyyyy\u5e74MMMMM\u6708"));
        assertEquals("H00025\u5e744\u6708", str);
*/

        // L month; standalone style in CLDR
        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGy\u5e74L\u6708"));
        assertEquals("H25\u5e744\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGyy\u5e74LL\u6708"));
        assertEquals("H25\u5e7404\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGyyy\u5e74LLL"));
        assertEquals("H025\u5e744\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGyyyy\u5e74LLLL"));
        assertEquals("H0025\u5e744\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern( // three letters should be TEXT short
                            "GGGGGyyyyy\u5e74LLLLL\u6708"));
        assertEquals("H00025\u5e744\u6708", str);

        // L month standalone
        str = date.format(DateTimeFormatter.ofPattern(
                            "L LL LLL LLLL LLLLL"));
        assertEquals("4 04 4\u6708 4\u6708 4", str);

        // M month
        str = date.format(DateTimeFormatter.ofPattern(
                            "M MM MMM MMMM MMMMM"));
        assertEquals("4 04 4\u6708 4\u6708 4", str);

// moved from above by known error
        str = date.format(DateTimeFormatter.ofPattern( // five letters should be TEXT nallow
                            "GGGGGyyyyy\u5e74MMMMM\u6708"));
        assertEquals("H00025\u5e744\u6708", str);
    }

    @Test
    public void formatterTestWeek(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;
        // E; text
        str = date.format(DateTimeFormatter.ofPattern(
                            "E EE EEE EEEE EEEEE"));
        assertEquals("\u706b \u706b \u706b \u706b\u66dc\u65e5 \u706b", str);

        // e; e and ee are numeric
        str = date.format(DateTimeFormatter.ofPattern(
                            "e ee eee eeee eeeee"));
        assertEquals("3 03 \u706b \u706b\u66dc\u65e5 \u706b", str);

        // c; c is numeric. ccc,cccc and ccccc are standalone text 
        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "cc"));
            fail("cc is not supported. It should be c, ccc, cccc, ccccc");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                            "c ccc cccc ccccc"));
        assertEquals("3 \u706b \u706b\u66dc\u65e5 \u706b", str);
    }

    @Test
    public void formatterTestLocal(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        assertEquals("2013-04-30", date.toString());

        // G
        str = date.format(DateTimeFormatter.ofPattern(
                          "Gy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u66a62013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u66a62013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u66a62013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("\u897f\u66a62013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGy\u5e74M\u6708d\u65e5"));
        assertEquals("AD2013\u5e744\u670830\u65e5", str);

        // y M d
        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyy\u5e74MM\u6708dd\u65e5"));
        assertEquals("AD13\u5e7404\u670830\u65e5", str);

        try{
            str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyy\u5e74MMM\u6708ddd\u65e5"));
            fail("d should be one or two letter");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyy\u5e74MMMM"));
        assertEquals("AD2013\u5e744\u6708", str);

        str = date.format(DateTimeFormatter.ofPattern(
                          "GGGGGyyyyy\u5e74MMMMM\u6708"));
        assertEquals("AD02013\u5e744\u6708", str);
    }

    @Test
    public void localizedJpTest(){
        JapaneseDate date = JapaneseDate.of(2013, 4, 30);
        String str;

        // ofLocalizedDate is using ISO style only
        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        assertEquals("2013\u5e744\u670830\u65e5\u706b\u66dc\u65e5", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        assertEquals("2013/04/30", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        assertEquals("2013/04/30", str);
    }

    @Test
    public void localizedLocalTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        assertEquals("2013\u5e744\u670830\u65e5\u706b\u66dc\u65e5", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        assertEquals("2013/04/30", str);

        str = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        assertEquals("2013/04/30", str);
    }

    @Test
    public void localizedDateTimeJpTest(){
        JapaneseDate date = JapaneseDate.of(2013, 4, 30);
        ChronoLocalDateTime dt = date.atTime(LocalTime.of(12,0));
        String str;

        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
            fail("FULL needs timezone on JP");
        }catch(DateTimeException e){
        }

        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
            fail("LONG needs timezone on JP");
        }catch(DateTimeException e){
        }

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/04/30 12:00:00", str);

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/04/30 12:00", str);
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
        try{
            str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
            fail("LONG needs timezone");
        }catch(DateTimeException e){
        }

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/04/30 12:00:00", str);

        str = dt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/04/30 12:00", str);
    }

    @Test
    public void localizedDateTimeLocalTZTest(){
        ZonedDateTime zdt = ZonedDateTime.of(2013,4,30, 12,0,0,0, ZoneId.of("Asia/Tokyo"));
        String str;

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
	/*
        if (ibmVM) {
            assertEquals("2013\u5e744\u670830\u65e5 (\u706b\u66dc\u65e5) 12\u664200\u520600\u79d2 \u65e5\u672c\u6a19\u6e96\u6642", str);
        } else {
	*/
            assertEquals("2013\u5e744\u670830\u65e5\u706b\u66dc\u65e5 12\u664200\u520600\u79d2 \u65e5\u672c\u6a19\u6e96\u6642", str);
	/*
        }
	*/

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5 12:00:00 JST", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/04/30 12:00:00", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/04/30 12:00", str);
    }

    @Test
    public void localizedZonedDateTimeJpTest(){
        JapaneseDate date = JapaneseDate.of(2013, 4, 30);
        ChronoLocalDateTime dt = date.atTime(LocalTime.of(12,0));
        ChronoZonedDateTime zdt = dt.atZone(ZoneId.of("Asia/Tokyo"));
        String str;

        // ofLocalizedDateTime is using ISO style only
        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
        assertEquals("2013\u5e744\u670830\u65e5\u706b\u66dc\u65e5 12\u664200\u520600\u79d2 \u65e5\u672c\u6a19\u6e96\u6642", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        assertEquals("2013\u5e744\u670830\u65e5 12:00:00 JST", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        assertEquals("2013/04/30 12:00:00", str);

        str = zdt.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        assertEquals("2013/04/30 12:00", str);
    }

    @Test
    public void localizedTimeLocalTest(){
        LocalTime time = LocalTime.of(12, 0);
        String str;

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        assertEquals("12:00", str);

        str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
        assertEquals("12:00:00", str);

        try{
            str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
            fail("LONG needs tiemzone on JP");
        }catch(DateTimeException e){
        }

        try{
            str = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
            fail("FULL needs tiemzone on JP");
        }catch(DateTimeException e){
        }
    }

    @Test
    public void firstYearTest(){
        JapaneseDate jp = JapaneseDate.of(1989, 1, 9);
        assertEquals("Japanese Heisei 1-01-09", jp.toString());

        String str = jp.format(DateTimeFormatter.ofPattern(
                                 "Gyyyy\u5e74M\u6708d\u65e5"));
        assertEquals("\u5e73\u62100001\u5e741\u67089\u65e5", str);
    }
}
