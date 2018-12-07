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

import java.time.LocalTime;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

public class LocalTimeTest{
    @Test
    public void ofTest() {
        assertEquals(LocalTime.of(12,0),
                     LocalTime.of(12,0,0));
        assertEquals(LocalTime.of(12,0),
                     LocalTime.NOON);
        assertEquals(LocalTime.of(12,59),
                     LocalTime.of(12,59,0,0));
        assertEquals(LocalTime.of(12,59),
                     LocalTime.ofSecondOfDay((12*60+59)*60));
        assertEquals(LocalTime.of(12,59,59,123456789),
                     LocalTime.ofNanoOfDay(((12L*60L+59L)*60L+59L)*1_000_000_000L+123456789L));
    }
    @Test
    public void parseTest() {
        assertEquals(LocalTime.of(12,59,59),
                     LocalTime.parse("12:59:59"));
        assertEquals(LocalTime.of(12,59,59),
                     LocalTime.parse("12:59:59", DateTimeFormatter.ISO_TIME));
    }

    @Test
    public void getMethodsTest() {
        LocalTime time = LocalTime.of(23,59,59);
        assertEquals(23, time.getHour());
        assertEquals(59, time.getMinute());
        assertEquals(59, time.getSecond());
        assertEquals(59+60*(59+60*(23)),
                     time.get(ChronoField.SECOND_OF_DAY));
        assertEquals(time, LocalTime.ofSecondOfDay(59+60*(59+60*(23))));
        assertEquals(59+60*(59+60*(23)), time.toSecondOfDay());
        assertEquals(time, LocalTime.parse("23:59:59"));

        time = LocalTime.of(23,59,59,123456789);
        assertEquals(23, time.getHour());
        assertEquals(59, time.getMinute());
        assertEquals(59, time.getSecond());
        assertEquals(123456789, time.getNano());
        assertEquals(time, LocalTime.parse("23:59:59.123456789"));

        assertEquals(23, time.get(ChronoField.HOUR_OF_DAY));
        assertEquals(11, time.get(ChronoField.HOUR_OF_AMPM));
        assertEquals(1, time.get(ChronoField.AMPM_OF_DAY));
        assertEquals(59, time.get(ChronoField.MINUTE_OF_HOUR));
        assertEquals(59, time.get(ChronoField.SECOND_OF_MINUTE));
        assertEquals(123456789, time.get(ChronoField.NANO_OF_SECOND));
        assertEquals(123456, time.get(ChronoField.MICRO_OF_SECOND));
        assertEquals(123, time.get(ChronoField.MILLI_OF_SECOND));
        assertEquals(123456789L+1_000_000_000L*(59L+60L*(59L+60L*(23L))),
                     time.getLong(ChronoField.NANO_OF_DAY));
        assertEquals(time, LocalTime.ofNanoOfDay(123456789L+
                           1_000_000_000L*(59L+60L*(59L+60L*(23L)))));
        assertEquals(123456789L+1_000_000_000L*(59L+60L*(59L+60L*(23L))),
                     time.toNanoOfDay());
    }

    @Test
    public void invalidTimeTest() {
        try{
            LocalTime time = LocalTime.of(24,0,0);
            fail("LocalTime.of(24,0,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalTime time = LocalTime.of(23,60,0);
            fail("LocalTime.of(23,60,0)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalTime time = LocalTime.of(0,0,60);
            fail("LocalTime.of(0,0,60)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalTime time = LocalTime.of(0,0,0,1_000_000_000);
            fail("LocalTime.of(0,0,0,1,000,000,000)");
        }catch(DateTimeException e){
            //ok
        }
        try{
            LocalTime time = LocalTime.of(0,0,0,-1);
            fail("LocalTime.of(0,0,0,-1)");
        }catch(DateTimeException e){
            //ok
        }
    }

    @Test
    public void isSupportedTest() {
        LocalTime time = LocalTime.of(23,59,59);
        assertTrue(time.isSupported(ChronoField.NANO_OF_SECOND));
        assertFalse(time.isSupported(ChronoField.ERA));
        assertFalse(time.isSupported(ChronoField.EPOCH_DAY));
    }

    @Test
    public void rangeTest() {
        LocalTime time = LocalTime.of(23,59,59);
        ValueRange range = time.range(ChronoField.HOUR_OF_DAY);
        assertEquals(23, range.getMaximum());
        assertEquals(0, range.getMinimum());
        assertEquals(23, range.getSmallestMaximum());
        assertEquals(0, range.getLargestMinimum());
    }

    @Test
    public void withTest() {
        LocalTime time = LocalTime.of(23,59,59);
        assertEquals(LocalTime.of(11,59,59),
                     time.with(ChronoField.AMPM_OF_DAY, 0));
        assertEquals(LocalTime.of(1,59,59),
                     time.with(ChronoField.HOUR_OF_DAY, 1));
        assertEquals(LocalTime.of(23,0,59),
                     time.with(ChronoField.MINUTE_OF_HOUR, 0));
        assertEquals(LocalTime.of(23,59,0),
                     time.with(ChronoField.SECOND_OF_MINUTE, 0));

        assertEquals(LocalTime.of(1,59,59),
                     time.withHour(1));
        assertEquals(LocalTime.of(23,0,59),
                     time.withMinute(0));
        assertEquals(LocalTime.of(23,59,0),
                     time.withSecond(0));

        time = LocalTime.of(23,59,59,123456789);
        assertEquals(LocalTime.of(23,59,59, 987654321),
                     time.withNano(987654321));
    }

    @Test
    public void trancatedToTest() {
        LocalTime time = LocalTime.of(23,59,59,123456789);
        assertEquals(LocalTime.of(23,59,59,123456789),
                     time.truncatedTo(ChronoUnit.NANOS));
        assertEquals(LocalTime.of(23,59,59,123456_000),
                     time.truncatedTo(ChronoUnit.MICROS));
        assertEquals(LocalTime.of(23,59,59,123_000_000),
                     time.truncatedTo(ChronoUnit.MILLIS));
        assertEquals(LocalTime.of(23,59,59),
                     time.truncatedTo(ChronoUnit.SECONDS));
        assertEquals(LocalTime.of(23,59,0),
                     time.truncatedTo(ChronoUnit.MINUTES));
        assertEquals(LocalTime.of(23,0,0),
                     time.truncatedTo(ChronoUnit.HOURS));
        assertEquals(LocalTime.of(12,0,0),
                     time.truncatedTo(ChronoUnit.HALF_DAYS));
        assertEquals(LocalTime.of(0,0,0),
                     time.truncatedTo(ChronoUnit.DAYS));
    }

    @Test
    public void plusTest() {
        LocalTime time = LocalTime.of(23,59,59,123456789);
        assertEquals(LocalTime.of(0,59,59,123456789),
                     time.plus(Duration.ofHours(1)));
        assertEquals(LocalTime.of(0,0,59,123456789),
                     time.plus(Duration.ofMinutes(1)));
        assertEquals(LocalTime.of(0,0,0,123456789),
                     time.plus(Duration.ofSeconds(1)));
        assertEquals(LocalTime.of(0,0,0,   456789),
                     time.plus(Duration.ofMillis(877)));
        assertEquals(LocalTime.of(0,0,0,        0),
                     time.plus(Duration.ofNanos(876543211)));

        assertEquals(LocalTime.of(11,59,59,123456789),
                     time.plus(1, ChronoUnit.HALF_DAYS));
        assertEquals(LocalTime.of(0,59,59,123456789),
                     time.plus(1, ChronoUnit.HOURS));
        assertEquals(LocalTime.of(0,0,59,123456789),
                     time.plus(1, ChronoUnit.MINUTES));
        assertEquals(LocalTime.of(0,0,0,123456789),
                     time.plus(1, ChronoUnit.SECONDS));
        assertEquals(LocalTime.of(0,0,0,   456789),
                     time.plus(877, ChronoUnit.MILLIS));
        assertEquals(LocalTime.of(0,0,0,      789),
                     time.plus(876544, ChronoUnit.MICROS));
        assertEquals(LocalTime.of(0,0,0,        0),
                     time.plus(876543211, ChronoUnit.NANOS));

        assertEquals(LocalTime.of(0,59,59,123456789),
                     time.plusHours(1));
        assertEquals(LocalTime.of(0,0,59,123456789),
                     time.plusMinutes(1));
        assertEquals(LocalTime.of(0,0,0,123456789),
                     time.plusSeconds(1));
        assertEquals(LocalTime.of(0,0,0,        0),
                     time.plusNanos(876543211));
    }

    @Test
    public void minusTest() {
        LocalTime time = LocalTime.of(0,0,0,123456789);
        assertEquals(LocalTime.of(23,0,0,123456789),
                     time.minus(Duration.ofHours(1)));
        assertEquals(LocalTime.of(23,59,0,123456789),
                     time.minus(Duration.ofMinutes(1)));
        assertEquals(LocalTime.of(23,59,59,123456789),
                     time.minus(Duration.ofSeconds(1)));
        assertEquals(LocalTime.of(0,0,0,   456789),
                     time.minus(Duration.ofMillis(123)));
        assertEquals(LocalTime.of(23,59,59, 999_999_999),
                     time.minus(Duration.ofNanos(123456790)));
        assertEquals(LocalTime.of(12,0,0,123456789),
                     time.minus(1, ChronoUnit.HALF_DAYS));
        assertEquals(LocalTime.of(23,0,0,123456789),
                     time.minus(1, ChronoUnit.HOURS));
        assertEquals(LocalTime.of(23,59,0,123456789),
                     time.minus(1, ChronoUnit.MINUTES));
        assertEquals(LocalTime.of(23,59,59,123456789),
                     time.minus(1, ChronoUnit.SECONDS));
        assertEquals(LocalTime.of(0,0,0,   456789),
                     time.minus(123, ChronoUnit.MILLIS));
        assertEquals(LocalTime.of(0,0,0,      789),
                     time.minus(123456, ChronoUnit.MICROS));
        assertEquals(LocalTime.of(0,0,0,        0),
                     time.minus(123456789, ChronoUnit.NANOS));

        assertEquals(LocalTime.of(23,0,0,123456789),
                     time.minusHours(1));
        assertEquals(LocalTime.of(23,59,0,123456789),
                     time.minusMinutes(1));
        assertEquals(LocalTime.of(23,59,59,123456789),
                     time.minusSeconds(1));
        assertEquals(LocalTime.of(0,0,0,        0),
                     time.minusNanos(123456789));
    }

    @Test
    public void untilTest() {
        LocalTime time = LocalTime.of(1,2,3,123456789);
        assertEquals(0L, // +0:57:57
                     time.until(LocalTime.of(2,0,0,123456789), ChronoUnit.HOURS));
        assertEquals(57L, // +0:57:57
                     time.until(LocalTime.of(2,0,0,123456789), ChronoUnit.MINUTES));
        assertEquals(57L*60L+57L,
                     time.until(LocalTime.of(2,0,0,123456789), ChronoUnit.SECONDS));

        time = LocalTime.of(23,30,30,123456789);
        assertEquals(-23L, 
                     time.until(LocalTime.of(0,0,0,123456789), ChronoUnit.HOURS));
        assertEquals(-23L*60L-30L, 
                     time.until(LocalTime.of(0,0,0,123456789), ChronoUnit.MINUTES));
        assertEquals((-23L*60L-30L)*60L-30L,
                     time.until(LocalTime.of(0,0,0,123456789), ChronoUnit.SECONDS));
    }

    @Test
    public void isAfterTest() {
        LocalTime time = LocalTime.of(1,2,3,123456789);
        assertTrue(time.isAfter(LocalTime.of(0,2,3)));
        assertTrue(time.isAfter(LocalTime.of(1,1,3)));
        assertTrue(time.isAfter(LocalTime.of(1,2,2)));
        assertTrue(time.isAfter(LocalTime.of(1,2,3,123456788)));

        assertTrue(time.isBefore(LocalTime.of(2,2,3)));
        assertTrue(time.isBefore(LocalTime.of(1,3,3)));
        assertTrue(time.isBefore(LocalTime.of(1,2,4)));
        assertTrue(time.isBefore(LocalTime.of(1,2,3,123456790)));
    }
}
