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

import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.Locale;

public class JapaneseDateTest{

    JapaneseDateData jpdata = new JapaneseDateData();

    @Test
    public void ofTest() {
        for(PairOfDate data: jpdata.dateList){
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
            JapaneseDate dateO1 = JapaneseDate.of((JapaneseEra)data.era, data.eraYear,
                                     data.month, data.day);
            JapaneseDate dateO2 = JapaneseDate.of(data.year,
                                     data.month, data.day);
            assertEquals(dateH, dateO1);
            assertEquals(dateH, dateO2);
            assertTrue(dateH.equals(dateO1));
            assertTrue(dateH.equals(dateO2));
        }
    }

    @Test
    public void ofInvalidTest(){
        boolean result=true;
        StringBuilder failedData = new StringBuilder();
        for(PairOfDate data: jpdata.invalidList){ // Invalid List
            try{
                JapaneseDate dateJ = JapaneseDate.of((JapaneseEra)data.era, data.eraYear,
                                         data.month, data.day);
                failedData.append(" ").append(data).append(",");
                //System.out.println("ofInvalidTest Failed: "+data);
                result = false;
            }catch(DateTimeException e){
            }catch(ClassCastException e){
                //System.out.println("OK Invalid "+data);
                //ok
            }
        }
        if (!result) {
            fail(failedData.toString());
        }
    }

    @Test
    public void getTest() {
        for(PairOfDate data: jpdata.dateList){
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
            assertEquals(data.era.getValue(), dateH.get(ChronoField.ERA));
            assertEquals(data.eraYear, dateH.get(ChronoField.YEAR_OF_ERA));
            assertEquals(data.year, dateH.get(ChronoField.YEAR));
            assertEquals(data.month, dateH.get(ChronoField.MONTH_OF_YEAR));
            assertEquals(data.day, dateH.get(ChronoField.DAY_OF_MONTH));
        }
    }

    @Test
    public void getMethodsTest() {
        for(PairOfDate data: jpdata.dateList){
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
            assertEquals(data.era, dateH.getEra());
            assertEquals(data.era.getValue(), dateH.getLong(ChronoField.ERA));
            assertEquals(data.eraYear, dateH.getLong(ChronoField.YEAR_OF_ERA));
            assertEquals(data.year, dateH.getLong(ChronoField.YEAR));
            assertEquals(data.month, dateH.getLong(ChronoField.MONTH_OF_YEAR));
            assertEquals(data.day, dateH.getLong(ChronoField.DAY_OF_MONTH));
        }
    }

    @Test
    public void lengthOfMonthTest() {
        JapaneseDate date;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 1);
        assertEquals(28, date.lengthOfMonth());

        date = JapaneseDate.of(JapaneseEra.HEISEI, 24, 2, 1);
        assertEquals(29, date.lengthOfMonth());

        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 1, 1);
        assertEquals(31, date.lengthOfMonth());

        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 4, 1);
        assertEquals(30, date.lengthOfMonth());
    }

    @Test
    public void adjustTest() {
        JapaneseDate base, test;
        base = JapaneseChronology.INSTANCE.date(
                                JapaneseEra.HEISEI, 25, 2, 1);
        test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(2013, 2, 28));

        base = JapaneseChronology.INSTANCE.date(
                                JapaneseEra.HEISEI, 24, 2, 1);
        test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(2012, 2, 29));
    }
    
    @Test
    public void untilTest() {
        JapaneseDate date, target;
        date   = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 1);
        target = JapaneseDate.of(JapaneseEra.HEISEI, 25, 3, 1);
        long diff = date.until(target, ChronoUnit.MONTHS);
        assertEquals(diff, 1);

        date   = JapaneseDate.of(JapaneseEra.SHOWA, 64, 1, 1); // = 1989
        target = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10); // = 1989
        diff = date.until(target, ChronoUnit.DAYS);
        assertEquals(diff, 9);

    }

    @Test
    public void periodTest() {
        JapaneseDate date, target;
        date   = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 1);
        target = JapaneseDate.of(JapaneseEra.HEISEI, 25, 3, 1);
        ChronoPeriod period = date.until(target);
        assertEquals(JapaneseChronology.INSTANCE.period(0,1,0), period);

        LocalDate local = LocalDate.of(2013,3,1);
        period = date.until(local);
        assertEquals(JapaneseChronology.INSTANCE.period(0,1,0), period);

        date   = JapaneseDate.of(JapaneseEra.SHOWA, 64, 1, 1); // = 1989
        target = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10); // = 1989

        period = date.until(target);
        assertEquals(JapaneseChronology.INSTANCE.period(0,0,9), period);

        local = LocalDate.of(1989,1,10);
        period = date.until(local);
        assertEquals(JapaneseChronology.INSTANCE.period(0,0,9), period);
    }

    @Test
    public void minusTest() {
        JapaneseDate date, result;
        JapaneseChronology jpChrono = JapaneseChronology.INSTANCE;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        result = date.minus(jpChrono.period(0,1,0));
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 25, 1, 28),
                     result);

        date = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10);
        result = date.minus(jpChrono.period(0,1,0));
        assertEquals(JapaneseDate.of(JapaneseEra.SHOWA, 63, 12, 10),
                     result);

        date = JapaneseDate.of(JapaneseEra.SHOWA, 1, 12, 25);
        result = date.minus(jpChrono.period(0,0,1));
        assertEquals(JapaneseDate.of(JapaneseEra.TAISHO, 15, 12, 24),
                     result);

        date = JapaneseDate.of(JapaneseEra.TAISHO, 1, 7, 30);
        result = date.minus(jpChrono.period(0,0,1));
        assertEquals(JapaneseDate.of(JapaneseEra.MEIJI, 45, 7, 29),
                     result);

        date = JapaneseDate.of(JapaneseEra.MEIJI, 6, 1, 1);
        try{
            result = date.minus(jpChrono.period(0,0,1));
            fail("Meiji 5 is invalid. Meiji supports from 6");
        }catch(DateTimeException e){
        }
    }

    @Test
    public void minusTest2() {
        JapaneseDate date, result;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        result = date.minus(1, ChronoUnit.MONTHS);
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 25, 1, 28),
                     result);

        date = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10);
        result = date.minus(1, ChronoUnit.MONTHS);
        assertEquals(JapaneseDate.of(JapaneseEra.SHOWA, 63, 12, 10),
                     result);

        date = JapaneseDate.of(JapaneseEra.SHOWA, 1, 12, 25);
        result = date.minus(1, ChronoUnit.DAYS);
        assertEquals(JapaneseDate.of(JapaneseEra.TAISHO, 15, 12, 24),
                     result);

        date = JapaneseDate.of(JapaneseEra.TAISHO, 1, 7, 30);
        result = date.minus(1, ChronoUnit.DAYS);
        assertEquals(JapaneseDate.of(JapaneseEra.MEIJI, 45, 7, 29),
                     result);

        date = JapaneseDate.of(JapaneseEra.MEIJI, 6, 1, 1);
        try{
            result = date.minus(1, ChronoUnit.DAYS);
            fail("Meiji 5 is invalid. Meiji supports from 6");
        }catch(DateTimeException e){
        }
    }

    @Test
    public void plusTest() {
        JapaneseDate date, result;
        JapaneseChronology jpChrono = JapaneseChronology.INSTANCE;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        result = date.plus(jpChrono.period(0,1,0));
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 25, 3, 28),
                     result);

        date = JapaneseDate.of(JapaneseEra.SHOWA, 63, 12, 10);
        result = date.plus(jpChrono.period(0,1,0));
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10),
                     result);

        date = JapaneseDate.of(JapaneseEra.TAISHO, 15, 12, 24);
        result = date.plus(jpChrono.period(0,0,1));
        assertEquals(JapaneseDate.of(JapaneseEra.SHOWA, 1, 12, 25),
                     result);

        date = JapaneseDate.of(JapaneseEra.MEIJI, 45, 7, 29);
        result = date.plus(jpChrono.period(0,0,1));
        assertEquals(JapaneseDate.of(JapaneseEra.TAISHO, 1, 7, 30),
                     result);

        try{
            date = JapaneseDate.of(JapaneseEra.MEIJI, 5, 12, 31); // not supported
            fail("Meiji 5 is invalid. Meiji supports from 6");
        }catch(DateTimeException e){
        }
    }
 
    @Test
    public void plusTest2() {
        JapaneseDate date, result;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        result = date.plus(1, ChronoUnit.MONTHS);
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 25, 3, 28),
                     result);

        date = JapaneseDate.of(JapaneseEra.SHOWA, 63, 12, 10);
        result = date.plus(1, ChronoUnit.MONTHS);
        assertEquals(JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10),
                     result);

        date = JapaneseDate.of(JapaneseEra.TAISHO, 15, 12, 24);
        result = date.plus(1, ChronoUnit.DAYS);
        assertEquals(JapaneseDate.of(JapaneseEra.SHOWA, 1, 12, 25),
                     result);

        date = JapaneseDate.of(JapaneseEra.MEIJI, 45, 7, 29);
        result = date.plus(1, ChronoUnit.DAYS);
        assertEquals(JapaneseDate.of(JapaneseEra.TAISHO, 1, 7, 30),
                     result);

        try{
            date = JapaneseDate.of(JapaneseEra.MEIJI, 5, 12, 31);
            fail("Meiji 5 is invalid. Meiji supports from 6");
            //result = date.plus(1, ChronoUnit.DAYS);
            //assertEquals(JapaneseDate.of(JapaneseEra.MEIJI, 6, 1, 1),
            //         result);
        }catch(DateTimeException e){
        }
    }
 
    @Test
    public void formatterTest() {
        JapaneseDate date;
        date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        String str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         new Locale("ja", "JP", "JP")));
        assertEquals("\u5e73\u621025\u5e742\u670828\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.JAPAN ));
        assertEquals("\u5e73\u621025\u5e742\u670828\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.JAPANESE ));
        assertEquals("\u5e73\u621025\u5e742\u670828\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.US ));
        assertEquals("Heisei25\u5e742\u670828\u65e5", str);

        /* move to FormatterTest_ja test
        date = JapaneseDate.of(JapaneseChronology.ERA_HEISEI, 1, 1, 10);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         new Locale("ja", "JP", "JP")));
        //assertEquals("\u5e73\u62101\u5e741\u670810\u65e5", str);
        assertEquals("\u5e73\u6210\u5143\u5e741\u670810\u65e5", str); // Gan-nen not supported?
        */
    }

    @Test
    public void formatterTest2() {
        JapaneseDate date = JapaneseDate.of(JapaneseEra.HEISEI, 1, 1, 10);
        final String Heisei_1_1_10 = new String("\u5e73\u62101\u5e741\u670810\u65e5");

        assertEquals("Japanese Heisei 1-01-10", date.toString()); // default

        String str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.JAPAN));
        assertEquals("\u5e73\u62101\u5e741\u670810\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.JAPANESE));
        assertEquals(Heisei_1_1_10, str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74M\u6708d\u65e5",
                         Locale.US));
        assertEquals("Heisei1\u5e741\u670810\u65e5", str);

        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGyyyy\u5e74MMMd\u65e5",
                         new Locale("ja", "JP", "JP")));
        assertEquals("\u5e73\u62100001\u5e741\u670810\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGyyyy\u5e74MMMd\u65e5",
                         Locale.JAPAN));
        assertEquals("\u5e73\u62100001\u5e741\u670810\u65e5", str);
        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGyyyy\u5e74MMM\u6708d\u65e5",
                         Locale.US));
        assertEquals("Heisei0001\u5e74Jan\u670810\u65e5", str); //Jan

        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGGy\u5e74MMMd\u65e5",
                         new Locale("ja", "JP", "JP")));
        assertEquals("H1\u5e741\u670810\u65e5", str); // H1*1*10*
        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGGy\u5e74MMMd\u65e5",
                         Locale.JAPAN));
        assertEquals("H1\u5e741\u670810\u65e5", str); // H1*1*10*
        str = date.format(DateTimeFormatter.ofPattern(
                         "GGGGGy\u5e74MMM\u6708d\u65e5",
                         Locale.US));
        assertEquals("H1\u5e74Jan\u670810\u65e5", str); //H1*Jan*10*

        LocalDate local = LocalDate.from(date);
        str = local.format(DateTimeFormatter.ofPattern(
                         "Gy\u5e74MMMd\u65e5",
                         new Locale("ja", "JP", "JP")));
        assertEquals("\u897f\u66a61989\u5e741\u670810\u65e5", str);

        date = JapaneseDate.from(local);
        assertEquals("Japanese Heisei 1-01-10", date.toString());
    }

    @Test
    public void atTimeTest() {
        JapaneseDate date = JapaneseDate.of(JapaneseEra.HEISEI, 25, 2, 28);
        ChronoLocalDateTime datetime = date.atTime(LocalTime.of(0,0,0));
        assertEquals("Japanese Heisei 25-02-28T00:00", datetime.toString());
    }

    @Test
    public void isAfterTest() {
        JapaneseDate preDate = null;
        for(PairOfDate data: jpdata.dateList){
            if (preDate == null) {
                preDate = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
                continue;
            }
            JapaneseDate target = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
            assertTrue(preDate+"-isAfter-"+target, preDate.isAfter(target));
            assertFalse(preDate+"-isBefore-"+target, preDate.isBefore(target));
            preDate = target;
        }
    }

    @Test
    public void isLeapYearTest() {
        for(PairOfDate data: jpdata.dateList){
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear,
                                     data.month, data.day);
            assertEquals(dateH.isLeapYear(), data.leap);
        }
    }
}
