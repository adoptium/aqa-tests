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

public class FormatTest_ko_kr{

    @Test
    public void WeekOfDayTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "E EE EEE EEEE EEEEE",
                            Locale.KOREAN));
        assertEquals("\ud654 \ud654 \ud654 \ud654\uc694\uc77c \ud654", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "e ee eee eeee eeeee",
                            Locale.KOREAN));
        assertEquals("3 03 \ud654 \ud654\uc694\uc77c \ud654", str);
        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "cc", Locale.KOREAN));
            fail("cc is not supported. It should be c, ccc, cccc, ccccc");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                            "c ccc cccc ccccc",
                            Locale.KOREAN));
        assertEquals("3 \ud654 \ud654\uc694\uc77c \ud654", str);

/*
        date = LocalDate.of(2014, 3, 16);
        for (int i=0;i<7;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                //"ccccc",
                                "M/d (E) c ccc cccc ccccc",
                                Locale.KOREAN));
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
                            Locale.KOREAN));
            fail("F simbol should be single.");
        }catch(IllegalArgumentException e){
        }

/*
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "M/d (E) F",
                                Locale.KOREAN));
            System.out.println(str);
            date = date.plusDays(1);
        }
*/
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "F",
                                Locale.KOREAN));
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
                            Locale.KOREAN));
        assertEquals("1 01 1\ubd84\uae30 \uc81c 1/4\ubd84\uae30 1", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "q qq qqq qqqq qqqqq",
                            Locale.KOREAN));
        assertEquals("1 01 1\ubd84\uae30 \uc81c 1/4\ubd84\uae30 1", str);
    }

    @Test
    public void MonthTest(){
        LocalDate date = LocalDate.of(2014, 3, 17);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "M MM MMM MMMM MMMMM",
                            Locale.KOREAN));
        assertEquals("3 03 3\uc6d4 3\uc6d4 3\uc6d4", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "L LL LLL LLLL LLLLL",
                            Locale.KOREAN));
        assertEquals("3 03 3\uc6d4 3\uc6d4 3\uc6d4", str);
    }
}
