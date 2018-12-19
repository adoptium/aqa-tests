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
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

public class LocalDateTest{

    @Test
    public void startOfDayTest() {
        LocalDate baseDate = java.time.LocalDate.of(2013, 2, 1);
        LocalDateTime time = baseDate.atStartOfDay();
        assertEquals(LocalDateTime.of(2013,2,1,0,0), time);
    }

    @Test
    public void atTimeTest(){
        LocalDate baseDate = java.time.LocalDate.of(2013, 2, 1);
        LocalDateTime time = baseDate.atTime(23,59);
        assertEquals(LocalDateTime.of(2013,2,1,23,59), time);

        time = baseDate.atTime(23,59,59);
        assertEquals(LocalDateTime.of(2013,2,1,23,59,59), time);

        time = baseDate.atTime(23,59,59, 999_999_999);
        assertEquals(LocalDateTime.of(2013,2,1,23,59,59, 999_999_999), time);

        try{
            time = baseDate.atTime(24,0);
            fail("atTime(24,0)");
        }catch(java.time.DateTimeException e){
            // ok
        }
        try{
            time = baseDate.atTime(23,60);
            fail("atTime(23,60)");
        }catch(java.time.DateTimeException e){
            // ok
        }
        try{
            time = baseDate.atTime(23,59,60);
            fail("atTime(23,59,60)");
        }catch(java.time.DateTimeException e){
            // ok
        }
        try{
            time = baseDate.atTime(23,59,59, 1_000_000_000);
            fail("atTime(23,59,59,1_000_000000)");
        }catch(java.time.DateTimeException e){
            // ok
        }

    }

    @Test
    public void CompareToTest(){
        LocalDate base,test;
        int result;

        base = java.time.LocalDate.of(2012, 2, 29);
        test = java.time.LocalDate.of(2012, 3, 1);
        result = base.compareTo(test);
        assertTrue("Compare result="+result, result < 0);

        test = java.time.LocalDate.of(2012, 2, 28);
        result = base.compareTo(test);
        assertTrue("Compare result="+result, result > 0);

        test = java.time.LocalDate.of(2012, 2, 29);
        result = base.compareTo(test);
        assertTrue("Compare result="+result, result == 0);
    }

    @Test
    public void EqualsTest(){
        LocalDate base,test;

        base = java.time.LocalDate.of(2012, 2, 29);
        test = java.time.LocalDate.of(2012, 3, 1);
        assertFalse(base.equals(test));

        base = java.time.LocalDate.of(2012, 2, 29);
        test = java.time.LocalDate.of(2012, 2, 29);
        assertTrue(base.equals(test));
    }

    @Test
    public void getTest(){
        LocalDate base;
        base = LocalDate.of(2012, 2, 29);

        assertEquals(29, base.getDayOfMonth());
        assertEquals(DayOfWeek.WEDNESDAY, base.getDayOfWeek());
        assertEquals(31+29, base.getDayOfYear());
        assertEquals(1, base.getEra().getValue());
        assertEquals(Month.FEBRUARY, base.getMonth());
        assertEquals(2, base.getMonthValue());
        assertEquals(2012, base.getYear());
        assertEquals(29, base.lengthOfMonth());
        assertEquals(366, base.lengthOfYear());
        assertTrue(base.isLeapYear());
        assertEquals(LocalDate.of(2012,2,28),
                     base.minusDays(1));
        assertEquals(1, base.minusMonths(1).getMonthValue());
        assertEquals(22, base.minusWeeks(1).getDayOfMonth());
        assertEquals(LocalDate.of(2011,2,28),
                     base.minusYears(1));
        assertEquals(LocalDate.of(2012,3,1),
                     base.plusDays(1));
        assertEquals(3, base.plusMonths(1).getMonthValue());
        assertEquals(7, base.plusWeeks(1).getDayOfMonth());
        assertEquals(LocalDate.of(2013,2,28),
                     base.plusYears(1));
        assertEquals("2012-02-29", base.toString());

        assertEquals(base,
                     LocalDate.parse("2012-02-29"));
    }

    @Test
    public void isAfterTest(){
        LocalDate base,test;
        base = java.time.LocalDate.of(2013, 2, 28);
        test = java.time.LocalDate.of(2013, 3, 1);
        assertTrue(test.isAfter(base));
        assertTrue(base.isBefore(test));
    }

    @Test
    public void adjustTest() {
        LocalDate base,test;

        base = java.time.LocalDate.of(2013, 2, 1);
        test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, LocalDate.of(2013,2,28));

        base = java.time.LocalDate.of(2012, 2, 1);
        test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, LocalDate.of(2012,2,29));
    }

}
