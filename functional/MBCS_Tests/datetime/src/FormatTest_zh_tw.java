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

import java.time.*;
import java.time.format.*;
import java.util.*;

public class FormatTest_zh_tw{
    private long version = 0L;

    @Before
    public void init() {
        version = JavaVersion.getVersion();
    }

    @Test
    public void WeekOfDayTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "E EE EEE EEEE EEEEE",
                            Locale.TRADITIONAL_CHINESE));
        assertEquals("\u9031\u4e8c \u9031\u4e8c \u9031\u4e8c \u661f\u671f\u4e8c \u4e8c", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "e ee eee eeee eeeee",
                            Locale.TRADITIONAL_CHINESE));
        assertEquals("3 03 \u9031\u4e8c \u661f\u671f\u4e8c \u4e8c", str);
        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "cc", Locale.TRADITIONAL_CHINESE));
            fail("cc is not supported. It should be c, ccc, cccc, ccccc");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                            "c ccc cccc ccccc",
                            Locale.TRADITIONAL_CHINESE));
        assertEquals("3 \u9031\u4e8c \u661f\u671f\u4e8c \u4e8c", str);

/*
        date = LocalDate.of(2014, 3, 16);
        for (int i=0;i<7;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                //"ccccc",
                                "M/d (E) c ccc cccc ccccc",
                                Locale.TRADITIONAL_CHINESE));
            System.out.println(str);
            String expected = new Integer(i+1).toString();
            //assertEquals(expected, str);
            date = date.plusDays(1);
        }
*/
    }

    @Test
    public void AlignedWeekOfDayTest(){
        LocalDate date = LocalDate.of(2014, 3, 16);
        String str;
        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "FF",
                            Locale.TRADITIONAL_CHINESE));
            fail("F simbol should be single.");
        }catch(IllegalArgumentException e){
        }

/*
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "M/d (E) F",
                                Locale.TRADITIONAL_CHINESE));
            System.out.println(str);
            date = date.plusDays(1);
        }
*/
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "F",
                                Locale.TRADITIONAL_CHINESE));
            String expected = Integer.valueOf(i%7+1).toString();
            assertEquals(expected, str); // 3/1=1, 3/2=2
            date = date.plusDays(1);
        }
    }

    @Test
    public void QuarterTest(){
        LocalDate date = LocalDate.of(2014, 3, 17);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "Q QQ QQQ QQQQ QQQQQ",
                            Locale.TRADITIONAL_CHINESE));
        // Not sure about no QuarterAbbreviations case.
        // assertEquals("1 01 1 \u7b2c1\u5b63 1", str);
        //  or
        // assertEquals("1 01 \u7b2c1\u5b63 \u7b2c1\u5b63 1", str);
        // ?

        str = date.format(DateTimeFormatter.ofPattern(
                            "q qq qqq qqqq qqqqq",
                            Locale.TRADITIONAL_CHINESE));
        if (11_000_000L <= version && version < 13_000_000L) {
            assertEquals("1 01 1 \u7b2c1\u5b63 1", str);
        } else if (version >= 13_000_000L) {
            //Updated by JDK-8221432: Upgrade CLDR to Version 35.1
            assertEquals("1 01 \u7b2c1\u5b63 \u7b2c1\u5b63 1", str);
        }
    }

    @Test
    public void MonthTest(){
        LocalDate date = LocalDate.of(2014, 3, 17);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "M MM MMM MMMM MMMMM",
                            Locale.TRADITIONAL_CHINESE));
        assertEquals("3 03 3\u6708 3\u6708 3", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "L LL LLL LLLL LLLLL",
                            Locale.TRADITIONAL_CHINESE));
        assertEquals("3 03 3\u6708 3\u6708 3", str);
    }
}
