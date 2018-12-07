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

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.time.Period;
import java.time.Month;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;
/*
import java.time.LocalTime;
import java.time.temporal.Adjusters; 
*/

public class LocalDateTimeTest{
    @Test
    public void ofTest() {
        LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 12, 0, 0);
        assertEquals(LocalDateTime.of(2013, Month.FEBRUARY, 28, 12, 0), dt);
        assertEquals(LocalDateTime.of(2013, Month.FEBRUARY, 28, 12, 0, 0), dt);
        assertEquals(LocalDateTime.of(2013, Month.FEBRUARY, 28, 12, 0, 0, 0), dt);
        assertEquals(LocalDateTime.of(2013, 2, 28, 12, 0), dt);
        assertEquals(LocalDateTime.of(2013, 2, 28, 12, 0, 0, 0), dt);
        assertEquals(LocalDateTime.of(LocalDate.of(2013, 2, 28), LocalTime.of(12, 0)), dt);
        assertEquals(LocalDateTime.of(LocalDate.of(2013, 2, 28), LocalTime.NOON), dt);
    }

    @Test
    public void parseTest() {
        LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 12, 0, 0);
        assertEquals(dt,
                     LocalDateTime.parse("2013-02-28T12:00:00"));
        assertEquals(dt,
                     LocalDateTime.parse("2013-02-28T12:00:00",
                                         DateTimeFormatter.ISO_DATE_TIME));
    }

    @Test
    public void invalidDateTimeTest() {
        try{
            LocalDateTime dt = LocalDateTime.of(2013, 2, 29, 12, 0, 0);
            fail("LocalDateTime.of(2013,2,29,12,0,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalDateTime dt = LocalDateTime.of(2013, 13, 29, 12, 0, 0);
            fail("LocalDateTime.of(2013,13,29,12,0,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 24, 0, 0);
            fail("LocalDateTime.of(2013,2,28,24,0,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 23, 60, 0);
            fail("LocalDateTime.of(2013,2,28,23,60,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 23, 0, 60);
            fail("LocalDateTime.of(2013,2,28,23,0,60)");
        }catch(DateTimeException e){
            //ok
        }
    }

    @Test
    public void getMethodsTest() {
        LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 23, 59, 59, 123456789);

        assertEquals(2013, dt.get(ChronoField.YEAR));
        assertEquals(2, dt.get(ChronoField.MONTH_OF_YEAR));
        assertEquals(28, dt.get(ChronoField.DAY_OF_MONTH));
        assertEquals(23, dt.get(ChronoField.HOUR_OF_DAY));
        assertEquals(11, dt.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, dt.get(ChronoField.AMPM_OF_DAY));
        assertEquals(59, dt.get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(59, dt.get(ChronoField.SECOND_OF_MINUTE));
        assertEquals(123456789, dt.get(ChronoField.NANO_OF_SECOND));
        assertEquals(123456, dt.get(ChronoField.MICRO_OF_SECOND));
        assertEquals(123, dt.get(ChronoField.MILLI_OF_SECOND));

        assertEquals(2013, dt.getYear());
        assertEquals(Month.FEBRUARY, dt.getMonth());
        assertEquals(2, dt.getMonthValue());
        assertEquals(28, dt.getDayOfMonth());
        assertEquals(31+28, dt.getDayOfYear());
        assertEquals(DayOfWeek.THURSDAY, dt.getDayOfWeek());
        assertEquals(23, dt.getHour());
        assertEquals(59, dt.getMinute());
        assertEquals(59, dt.getSecond());
        assertEquals(123456789, dt.getNano());
        assertEquals(59+60*(59+60*(23)),
                     dt.get(ChronoField.SECOND_OF_DAY));
    }

    @Test
    public void isSupportedTest() {
        LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 23, 59, 59, 123456789);
        assertTrue(dt.isSupported(ChronoField.NANO_OF_SECOND));
        assertTrue(dt.isSupported(ChronoField.ERA));
        assertTrue(dt.isSupported(ChronoField.EPOCH_DAY));
        assertFalse(dt.isSupported(ChronoField.INSTANT_SECONDS));
    }

    @Test
    public void rangeTest() {
        LocalDateTime dt = LocalDateTime.of(2013, 2, 28, 23, 59, 59, 123456789);

        ValueRange range = dt.range(ChronoField.HOUR_OF_DAY);
        assertEquals(23, range.getMaximum());
        assertEquals(0, range.getMinimum());
        assertEquals(23, range.getSmallestMaximum());
        assertEquals(0, range.getLargestMinimum());

        range = dt.range(ChronoField.YEAR);
        assertEquals(999999999, range.getMaximum());
        assertEquals(-999999999, range.getMinimum());
        assertEquals(999999999, range.getSmallestMaximum());
        assertEquals(-999999999, range.getLargestMinimum());
    }

    @Test
    public void withTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 23, 59, 59, 123456789);
        assertEquals(LocalDateTime.of(2013,2,28,23,59,59, 123456789),
                     dt.with(ChronoField.YEAR, 2013));
        assertEquals(LocalDateTime.of(2012,1,29,23,59,59, 123456789),
                     dt.with(ChronoField.MONTH_OF_YEAR, 1));
        assertEquals(LocalDateTime.of(2012,2,1,23,59,59, 123456789),
                     dt.with(ChronoField.DAY_OF_MONTH, 1));
        assertEquals(LocalDateTime.of(2012,2,29,11,59,59, 123456789),
                     dt.with(ChronoField.AMPM_OF_DAY, 0));
        assertEquals(LocalDateTime.of(2012,2,29,1,59,59, 123456789),
                     dt.with(ChronoField.HOUR_OF_DAY, 1));
        assertEquals(LocalDateTime.of(2012,2,29,23,0,59, 123456789),
                     dt.with(ChronoField.MINUTE_OF_HOUR, 0));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,0, 123456789),
                     dt.with(ChronoField.SECOND_OF_MINUTE, 0));

        assertEquals(LocalDateTime.of(2013,2,28,23,59,59, 123456789),
                     dt.withYear(2013));
        assertEquals(LocalDateTime.of(2012,1,29,23,59,59, 123456789),
                     dt.withMonth(1));
        assertEquals(LocalDateTime.of(2012,2,1,23,59,59, 123456789),
                     dt.withDayOfMonth(1));
        assertEquals(LocalDateTime.of(2012,1,1,23,59,59, 123456789),
                     dt.withDayOfYear(1));
        assertEquals(LocalDateTime.of(2012,2,29,1,59,59, 123456789),
                     dt.withHour(1));
        assertEquals(LocalDateTime.of(2012,2,29,23,0,59, 123456789),
                     dt.withMinute(0));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,0, 123456789),
                     dt.withSecond(0));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,59, 987654321),
                     dt.withNano(987654321));

    }

    @Test
    public void trancatedToTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 23, 59, 59, 123456789);

        assertEquals(LocalDateTime.of(2012,2,29,23,59,59, 123456789),
                     dt.truncatedTo(ChronoUnit.NANOS));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,59, 123456_000),
                     dt.truncatedTo(ChronoUnit.MICROS));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,59, 123_000_000),
                     dt.truncatedTo(ChronoUnit.MILLIS));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,59, 0),
                     dt.truncatedTo(ChronoUnit.SECONDS));
        assertEquals(LocalDateTime.of(2012,2,29,23,59,0, 0),
                     dt.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(LocalDateTime.of(2012,2,29,23,0,0, 0),
                     dt.truncatedTo(ChronoUnit.HOURS));
        assertEquals(LocalDateTime.of(2012,2,29,12,0,0, 0),
                     dt.truncatedTo(ChronoUnit.HALF_DAYS));
        assertEquals(LocalDateTime.of(2012,2,29,0,0,0, 0),
                     dt.truncatedTo(ChronoUnit.DAYS));
        try{
            assertEquals(LocalDateTime.of(2012,2,1,0,0,0, 0),
                         dt.truncatedTo(ChronoUnit.MONTHS));
            fail("truncatedTo(MONTHS)");
        }catch(java.time.DateTimeException e){
            //ok
        }
    }

    @Test
    public void plusTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 23, 59, 59, 123456789);

        assertEquals(LocalDateTime.of(2013, 2, 28, 23,59,59,123456789),
                     dt.plus(Period.ofYears(1)));
        assertEquals(LocalDateTime.of(2012, 3, 29, 23,59,59,123456789),
                     dt.plus(Period.ofMonths(1)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 23,59,59,123456789),
                     dt.plus(Period.ofDays(1)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,59,59,123456789),
                     dt.plus(Duration.ofHours(1)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,59,123456789),
                     dt.plus(Duration.ofMinutes(1)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,123456789),
                     dt.plus(Duration.ofSeconds(1)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,   456789),
                     dt.plus(Duration.ofMillis(877)));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,        0),
                     dt.plus(Duration.ofNanos(876543211)));

        assertEquals(LocalDateTime.of(2013, 2, 28, 23,59,59,123456789),
                     dt.plus(1, ChronoUnit.YEARS));
        assertEquals(LocalDateTime.of(2012, 3, 29, 23,59,59,123456789),
                     dt.plus(1, ChronoUnit.MONTHS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 23,59,59,123456789),
                     dt.plus(1, ChronoUnit.DAYS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 11,59,59,123456789),
                     dt.plus(1, ChronoUnit.HALF_DAYS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,59,59,123456789),
                     dt.plus(1, ChronoUnit.HOURS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,59,123456789),
                     dt.plus(1, ChronoUnit.MINUTES));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,123456789),
                     dt.plus(1, ChronoUnit.SECONDS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,   456789),
                     dt.plus(877, ChronoUnit.MILLIS));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,        0),
                     dt.plus(876543211, ChronoUnit.NANOS));

        assertEquals(LocalDateTime.of(2013, 2, 28, 23,59,59,123456789),
                     dt.plusYears(1));
        assertEquals(LocalDateTime.of(2012, 3, 29, 23,59,59,123456789),
                     dt.plusMonths(1));
        assertEquals(LocalDateTime.of(2012, 3, 7, 23,59,59,123456789),
                     dt.plusWeeks(1));
        assertEquals(LocalDateTime.of(2012, 3, 1, 23,59,59,123456789),
                     dt.plusDays(1));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,59,59,123456789),
                     dt.plusHours(1));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,59,123456789),
                     dt.plusMinutes(1));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,123456789),
                     dt.plusSeconds(1));
        assertEquals(LocalDateTime.of(2012, 3, 1, 0,0,0,        0),
                     dt.plusNanos(876543211));
    }
    @Test
    public void minusTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 0, 0, 0, 123456789);

        assertEquals(LocalDateTime.of(2011, 2, 28, 0,0,0,123456789),
                     dt.minus(Period.ofYears(1)));
        assertEquals(LocalDateTime.of(2012, 1, 29, 0,0,0,123456789),
                     dt.minus(Period.ofMonths(1)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 0,0,0,123456789),
                     dt.minus(Period.ofDays(1)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,0,0,123456789),
                     dt.minus(Duration.ofHours(1)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,0,123456789),
                     dt.minus(Duration.ofMinutes(1)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,123456789),
                     dt.minus(Duration.ofSeconds(1)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59, 999_456789),
                     dt.minus(Duration.ofMillis(124)));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,999999999),
                     dt.minus(Duration.ofNanos(123456790)));

        assertEquals(LocalDateTime.of(2011, 2, 28, 0,0,0,123456789),
                     dt.minus(1, ChronoUnit.YEARS));
        assertEquals(LocalDateTime.of(2012, 1, 29, 0,0,0,123456789),
                     dt.minus(1, ChronoUnit.MONTHS));
        assertEquals(LocalDateTime.of(2012, 2, 22, 0,0,0,123456789),
                     dt.minus(1, ChronoUnit.WEEKS));
        assertEquals(LocalDateTime.of(2012, 2, 28, 0,0,0,123456789),
                     dt.minus(1, ChronoUnit.DAYS));
        assertEquals(LocalDateTime.of(2012, 2, 28, 12,0,0,123456789),
                     dt.minus(1, ChronoUnit.HALF_DAYS));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,0,0,123456789),
                     dt.minus(1, ChronoUnit.HOURS));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,0,123456789),
                     dt.minus(1, ChronoUnit.MINUTES));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,123456789),
                     dt.minus(1, ChronoUnit.SECONDS));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,999999999),
                     dt.minus(123456790, ChronoUnit.NANOS));

        assertEquals(LocalDateTime.of(2011, 2, 28, 0,0,0,123456789),
                     dt.minusYears(1));
        assertEquals(LocalDateTime.of(2012, 1, 29, 0,0,0,123456789),
                     dt.minusMonths(1));
        assertEquals(LocalDateTime.of(2012, 2, 22, 0,0,0,123456789),
                     dt.minusWeeks(1));
        assertEquals(LocalDateTime.of(2012, 2, 28, 0,0,0,123456789),
                     dt.minusDays(1));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,0,0,123456789),
                     dt.minusHours(1));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,0,123456789),
                     dt.minusMinutes(1));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,123456789),
                     dt.minusSeconds(1));
        assertEquals(LocalDateTime.of(2012, 2, 28, 23,59,59,999999999),
                     dt.minusNanos(123456790));
    }

    @Test
    public void untilTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 1, 2, 3, 123456789);

        assertEquals(0, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.YEARS));
        assertEquals(11, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.MONTHS));
        assertEquals(365-28, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.DAYS));
        assertEquals((365-28)/7, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.WEEKS));
        assertEquals((365-28)*24+0, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.HOURS));
        assertEquals(((365-28)*24+0)*60+57, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.MINUTES));
        assertEquals((((365-28)*24+0)*60+57)*60+57, // + 0/11/31 0:57:57
                     dt.until(LocalDateTime.of(2013, 1, 31, 2,0,0,123456789), ChronoUnit.SECONDS));

        dt = LocalDateTime.of(2012, 2, 29, 23, 30, 30, 123456789);
        LocalDateTime dt2 = LocalDateTime.of(2012, 2, 29, 0,0,0,123456789);
        assertEquals(0, dt.until(dt2, ChronoUnit.DAYS));
        // failed?? -1??
        assertEquals(0, dt2.until(dt, ChronoUnit.DAYS));
        assertEquals(-23, dt.until(dt2, ChronoUnit.HOURS));
        assertEquals(-23*60-30, dt.until(dt2, ChronoUnit.MINUTES));
        assertEquals((-23*60-30)*60-30, dt.until(dt2, ChronoUnit.SECONDS));

        dt = LocalDateTime.of(2013, 4, 4, 12, 0, 0);
        dt2 = LocalDateTime.of(2013, 4, 4, 0, 0, 0);
        assertEquals(0, dt.until(dt2, ChronoUnit.DAYS));
        // failed?? -1??
        assertEquals(0, dt2.until(dt, ChronoUnit.DAYS));
        assertEquals(-12, dt.until(dt2, ChronoUnit.HOURS));
        assertEquals(12, dt2.until(dt, ChronoUnit.HOURS));
    }

    @Test
    public void isAfterTest() {
        LocalDateTime dt = LocalDateTime.of(2012, 2, 29, 1, 2, 3, 123456789);

        assertTrue(dt.isAfter(LocalDateTime.of(2011, 2, 28, 1, 2, 3, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 1, 29, 1, 2, 3, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 2, 28, 1, 2, 3, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 2, 29, 0, 2, 3, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 2, 29, 1, 1, 3, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 2, 29, 1, 2, 2, 123456789)));
        assertTrue(dt.isAfter(LocalDateTime.of(2012, 2, 29, 1, 2, 3, 123456788)));

        assertTrue(dt.isBefore(LocalDateTime.of(2013, 2, 28, 1, 2, 3, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 3, 29, 1, 2, 3, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 3,  1, 1, 2, 3, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 2, 29, 2, 2, 3, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 2, 29, 1, 3, 3, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 2, 29, 1, 2, 4, 123456789)));
        assertTrue(dt.isBefore(LocalDateTime.of(2012, 2, 29, 1, 2, 3, 123456790)));
    }
}
