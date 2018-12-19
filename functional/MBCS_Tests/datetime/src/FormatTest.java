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

public class FormatTest{

    @Test
    public void WeekOfDayTest(){
        LocalDate date = LocalDate.of(2013, 4, 30);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "E EE EEE EEEE EEEEE",
                            Locale.US));
        assertEquals("Tue Tue Tue Tuesday T", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "e ee eee eeee eeeee",
                            Locale.US));
        assertEquals("3 03 Tue Tuesday T", str);
        try{
            str = date.format(DateTimeFormatter.ofPattern(
                            "cc", Locale.US));
            fail("cc is not supported. It should be c, ccc, cccc, ccccc");
        }catch(IllegalArgumentException e){
        }

        str = date.format(DateTimeFormatter.ofPattern(
                            "c ccc cccc ccccc",
                            Locale.US));
        assertEquals("3 Tue Tuesday T", str);

/*
        date = LocalDate.of(2014, 3, 16);
        for (int i=0;i<7;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                //"ccccc",
                                "M/d (E) c ccc cccc ccccc",
                                Locale.US));
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
                            Locale.US));
            fail("F simbol should be single.");
        }catch(IllegalArgumentException e){
        }

/*
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "M/d (E) F",
                                Locale.US));
            System.out.println(str);
            date = date.plusDays(1);
        }
*/
        date = LocalDate.of(2014, 3, 1);
        for (int i=0;i<31;i++) {
            str = date.format(DateTimeFormatter.ofPattern(
                                "F",
                                Locale.US));
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
                            Locale.US));
        assertEquals("1 01 Q1 1st quarter 1", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "q qq qqq qqqq qqqqq",
                            Locale.US));
        assertEquals("1 01 Q1 1st quarter 1", str);
    }

    @Test
    public void MonthTest(){
        LocalDate date = LocalDate.of(2014, 3, 17);
        String str;
        str = date.format(DateTimeFormatter.ofPattern(
                            "M MM MMM MMMM MMMMM",
                            Locale.US));
        assertEquals("3 03 Mar March M", str);

        str = date.format(DateTimeFormatter.ofPattern(
                            "L LL LLL LLLL LLLLL",
                            Locale.US));
        assertEquals("3 03 Mar March M", str);
    }
}
