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
import java.time.chrono.Era;
import java.time.chrono.JapaneseEra;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Calendar;

public class JapaneseChronoTest{
    JapaneseDateData jpdata = new JapaneseDateData();

    @Test
    public void japaneseEraTest(){
        assertEquals(JapaneseChronology.INSTANCE.getId(), "Japanese");
        assertEquals(JapaneseChronology.INSTANCE.getCalendarType(), "japanese");
    }

    @Test
    public void dateTest1(){
        for(PairOfDate data: jpdata.dateList){
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear, 
                                     data.month, data.day);
            JapaneseDate dateS = JapaneseChronology.INSTANCE.date(
                                     data.year, data.month, data.day);
            assertEquals(dateH, dateS);
            assertTrue(dateH.equals(dateS));
        }
    }

    @Test
    public void dateInvalidTest(){
        boolean result=true;
        StringBuilder failedData = new StringBuilder();
        for(PairOfDate data: jpdata.invalidList){ // Invalid List
            try{
                JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                         data.era, data.eraYear, 
                                         data.month, data.day);
                //System.out.println("Failed: "+data);
                failedData.append(" ").append(data).append(",");
                result = false;
            }catch(DateTimeException de) {
            }catch(ClassCastException ce) {
            }
        }
        if (!result) {
            fail(failedData.toString());
        }
    }

    @Test
    public void dateYearDayTest1(){
        Calendar ca = Calendar.getInstance();
        for(PairOfDate data: jpdata.dateList){
            ca.set(data.year, data.month-1, data.day);
            int day_of_year = ca.get(Calendar.DAY_OF_YEAR);
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear, 
                                     data.month, data.day);
            JapaneseDate dateS = JapaneseChronology.INSTANCE.dateYearDay(
                                     data.year, day_of_year);
            assertEquals(dateH, dateS);
            assertTrue(dateH.equals(dateS));
        }
    }

    @Test
    public void dateYearDayTest2(){
        Calendar ca = Calendar.getInstance();
        for(PairOfDate data: jpdata.dateList){
            ca.set(data.year, data.month-1, data.day);
            int day_of_year = ca.get(Calendar.DAY_OF_YEAR);
            JapaneseDate dateH = JapaneseChronology.INSTANCE.date(
                                     data.era, data.eraYear, 
                                     data.month, data.day);
            JapaneseDate dateS = JapaneseChronology.INSTANCE.dateYearDay(
                                     data.year, day_of_year);
            assertEquals(dateH, dateS);
            assertTrue(dateH.equals(dateS));

            // dateyearDay(Era, year, day_of_year) returns the day from the era if 1st year.
            if (data.eraYear == 1) {
                if (data.era == JapaneseEra.HEISEI) {
                    ca.set(1989, 1-1, 8-1); // Heisei started 1989/1/8
                    int start_day = ca.get(Calendar.DAY_OF_YEAR);
                    day_of_year -= start_day;
                } else if (data.era == JapaneseEra.SHOWA) {
                    ca.set(1926, 12-1, 25-1); // Showa started 1926/12/25
                    int start_day = ca.get(Calendar.DAY_OF_YEAR);
                    day_of_year -= start_day;
                } else if (data.era == JapaneseEra.TAISHO) {
                    ca.set(1912, 7-1, 30-1); // Taisho started on 1912/7/30
                    int start_day = ca.get(Calendar.DAY_OF_YEAR);
                    day_of_year -= start_day;
                } // No MEIJI
            }
            JapaneseDate dateS2 = JapaneseChronology.INSTANCE.dateYearDay(
                                     data.era, data.eraYear, day_of_year);
            assertEquals(dateH, dateS2);
            assertTrue(dateH.equals(dateS2));
        }
    }

    @Test
    public void eraOfTest(){
        assertEquals(JapaneseEra.HEISEI, 
                     JapaneseChronology.INSTANCE.eraOf(2));
        assertEquals(JapaneseEra.SHOWA,
                     JapaneseChronology.INSTANCE.eraOf(1));
        assertEquals(JapaneseEra.TAISHO,
                     JapaneseChronology.INSTANCE.eraOf(0));
        assertEquals(JapaneseEra.MEIJI,
                     JapaneseChronology.INSTANCE.eraOf(-1));
    }

    @Test
    public void erasTest(){
        Era eraObj;
        eraObj = JapaneseChronology.INSTANCE.eraOf(2) ;
        assertTrue(JapaneseChronology.INSTANCE.eras().contains(eraObj));
        eraObj = JapaneseChronology.INSTANCE.eraOf(1) ;
        assertTrue(JapaneseChronology.INSTANCE.eras().contains(eraObj));
        eraObj = JapaneseChronology.INSTANCE.eraOf(0) ;
        assertTrue(JapaneseChronology.INSTANCE.eras().contains(eraObj));
        eraObj = JapaneseChronology.INSTANCE.eraOf(-1) ;
        assertTrue(JapaneseChronology.INSTANCE.eras().contains(eraObj));
    }

    @Test
    public void isLeapYearTest(){
        for(PairOfDate data: jpdata.dateList){
            assertEquals(JapaneseChronology.INSTANCE.isLeapYear(data.year),
                         data.leap);
        }
    }

    @Test(expected=java.time.DateTimeException.class)
    public void leapTest(){
        JapaneseDate dateS = JapaneseChronology.INSTANCE.date(
                                JapaneseEra.HEISEI,25,
                                2,29); //invalid
    }

    @Test
    public void prolepticYearTest(){
        for(PairOfDate data: jpdata.dateList){
            assertEquals(data.year,
                         JapaneseChronology.INSTANCE.
                             prolepticYear(data.era, data.eraYear));
        }
    }

    @Test
    public void rangeTest(){
        ValueRange range;
        range = JapaneseChronology.INSTANCE.range(ChronoField.ERA);
        //System.out.println("range="+range);
        assertEquals(3, range.getMaximum());
        assertEquals(-1, range.getMinimum());
    }

}
