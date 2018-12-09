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

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.DateTimeException;

public class ZonedDateTimeTest{
    @Test
    public void ofTest() {
        LocalDateTime local = LocalDateTime.of(2013,2,28,12,30,30,0);

        ZonedDateTime zdt1 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime zdt2 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("Japan"));
        //assertNotEquals(zdt1, zdt2);
        assertFalse(zdt1.equals(zdt2));
        ZonedDateTime zdt3 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("JST", ZoneId.SHORT_IDS));
        assertEquals(zdt1, zdt3);
        ZonedDateTime zdt4 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("UTC+09:00"));
        //assertNotEquals(zdt1, zdt4);
        assertFalse(zdt1.equals(zdt4));

        assertEquals(zdt1.toOffsetDateTime(), zdt2.toOffsetDateTime());
        assertEquals(zdt1.toOffsetDateTime(), zdt3.toOffsetDateTime());
        assertEquals(zdt1.toOffsetDateTime(), zdt4.toOffsetDateTime());

        assertEquals(ZoneOffset.ofHours(9), zdt1.getOffset());
        assertEquals(ZoneOffset.ofHours(9), zdt2.getOffset());
        assertEquals(ZoneOffset.ofHours(9), zdt3.getOffset());
        assertEquals(ZoneOffset.ofHours(9), zdt4.getOffset());
    }

    @Test
    public void withZoneSameInstantTest() {
        ZonedDateTime zdt1 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-02-27T21:30:30-06:00[America/Chicago]", zdt2.toString());
        assertEquals(ZoneOffset.ofHours(-6), zdt2.getOffset());

        zdt1 = ZonedDateTime.of(2013,8,28,12,30,30,0, ZoneId.of("Asia/Tokyo"));
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-08-27T22:30:30-05:00[America/Chicago]", zdt2.toString());
        assertEquals(ZoneOffset.ofHours(-5), zdt2.getOffset());

        // post 2007 March/2nd Sun - Nov/1st Sun
        zdt1 = ZonedDateTime.of(2013,3,10,16,59,59,0, ZoneId.of("Asia/Tokyo")); //ST end
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-03-10T01:59:59-06:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2013,3,10,17,0,0,0, ZoneId.of("Asia/Tokyo")); //DST start
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-03-10T03:00-05:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2013,11,3, 15,59,59,0, ZoneId.of("Asia/Tokyo")); //DST end
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-11-03T01:59:59-05:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2013,11,3, 16,0,0,0, ZoneId.of("Asia/Tokyo")); //ST start
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2013-11-03T01:00-06:00[America/Chicago]", zdt2.toString());

        // pre 2007 April/1st Sun - Oct/last Sun
        zdt1 = ZonedDateTime.of(2006,4,2,16,59,59,0, ZoneId.of("Asia/Tokyo")); //ST end
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2006-04-02T01:59:59-06:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2006,4,2,17,0,0,0, ZoneId.of("Asia/Tokyo")); //DST start
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2006-04-02T03:00-05:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2006,10,29, 15,59,59,0, ZoneId.of("Asia/Tokyo")); //DST end
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2006-10-29T01:59:59-05:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2006,10,29, 16,0,0,0, ZoneId.of("Asia/Tokyo")); //ST start
        zdt2 = zdt1.withZoneSameInstant(ZoneId.of("America/Chicago"));
        assertEquals("2006-10-29T01:00-06:00[America/Chicago]", zdt2.toString());

    }

    @Test
    public void invalidTest(){
        ZonedDateTime zdt1 = ZonedDateTime.of(2013,3,10,2,59,59,0, ZoneId.of("America/Chicago"));
        // for "of", gap time moves to 1 hour more.
        assertEquals("2013-03-10T03:59:59-05:00[America/Chicago]", zdt1.toString());

        try{
            ZonedDateTime zdt2 = ZonedDateTime.ofStrict(LocalDateTime.of(2013,3,10,2,59,59,0),
                                                        ZoneOffset.ofHours(-5),
                                                        ZoneId.of("America/Chicago"));
            fail("In DST gap, 2013,3,10,2,59,59,0 -05:00");
        }catch(DateTimeException e){
            // ok
        }
        try{
            ZonedDateTime zdt2 = ZonedDateTime.ofStrict(LocalDateTime.of(2013,3,10,2,59,59,0),
                                                        ZoneOffset.ofHours(-6),
                                                        ZoneId.of("America/Chicago"));
            fail("In DST gap, 2013,3,10,2,59,59,0 -06:00");
        }catch(DateTimeException e){
            // ok
        }
    }

    @Test
    public void withZoneSameLocalTest() {
        ZonedDateTime zdt1 = ZonedDateTime.of(2013,2,28,12,30,30,0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime zdt2 = zdt1.withZoneSameLocal(ZoneId.of("America/Chicago"));
        assertEquals("2013-02-28T12:30:30-06:00[America/Chicago]", zdt2.toString());

        zdt1 = ZonedDateTime.of(2013,8,28,12,30,30,0, ZoneId.of("Asia/Tokyo"));
        zdt2 = zdt1.withZoneSameLocal(ZoneId.of("America/Chicago"));
        assertEquals("2013-08-28T12:30:30-05:00[America/Chicago]", zdt2.toString());
    }

    @Test
    public void withLaterOffsetAtOverlapTest() {
        ZonedDateTime z1 = ZonedDateTime.of(2013,11,3, 15,59,59,0, ZoneId.of("Asia/Tokyo")); //No effect
        ZonedDateTime z2 = z1.withEarlierOffsetAtOverlap();
        assertEquals(z1, z2);
        ZonedDateTime z3 = z1.withLaterOffsetAtOverlap();
        assertEquals(z1, z3);

        ZonedDateTime us = ZonedDateTime.of(2013,11,3, 01,59,59,0, ZoneId.of("America/Chicago")); //DST end
        assertEquals("2013-11-03T01:59:59-05:00[America/Chicago]", us.toString());
        // In DST last day, 01:00:00-01:59:59 comes twice
        ZonedDateTime usEarlier = us.withEarlierOffsetAtOverlap();
        assertEquals("2013-11-03T01:59:59-05:00[America/Chicago]", usEarlier.toString());
        ZonedDateTime usLater = us.withLaterOffsetAtOverlap();
        assertEquals("2013-11-03T01:59:59-06:00[America/Chicago]", usLater.toString());

        assertEquals(z1,
                     usEarlier.withZoneSameInstant(ZoneId.of("Asia/Tokyo")));

        assertEquals(ZonedDateTime.of(2013,11,3, 16,59,59,0, ZoneId.of("Asia/Tokyo")),
                     usLater.withZoneSameInstant(ZoneId.of("Asia/Tokyo")));
    }

    @Test
    public void untilTest() {
        ZonedDateTime z1 = ZonedDateTime.of(2013,11,3, 15,59,59,0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime us = ZonedDateTime.of(2013,11,3, 01,59,59,0, ZoneId.of("America/Chicago")); //DST end
        assertEquals(0, z1.until(us, ChronoUnit.DAYS));
        assertEquals(0, z1.until(us, ChronoUnit.HOURS));

        ZonedDateTime usLater = us.withLaterOffsetAtOverlap();
        assertEquals(0, z1.until(usLater, ChronoUnit.DAYS));
        assertEquals(1, z1.until(usLater, ChronoUnit.HOURS));

        ZonedDateTime us1 = ZonedDateTime.of(2013,11,3, 0,0,0,0, ZoneId.of("America/Chicago"));
        ZonedDateTime us2 = ZonedDateTime.of(2013,11,4, 0,0,0,0, ZoneId.of("America/Chicago"));
        assertEquals(1, us1.until(us2, ChronoUnit.DAYS));
        assertEquals(25, us1.until(us2, ChronoUnit.HOURS));
        assertEquals(-1, us2.until(us1, ChronoUnit.DAYS));
        assertEquals(-25, us2.until(us1, ChronoUnit.HOURS));

        us1 = ZonedDateTime.of(2013,3,10, 0,0,0,0, ZoneId.of("America/Chicago"));
        us2 = ZonedDateTime.of(2013,3,11, 0,0,0,0, ZoneId.of("America/Chicago"));
        assertEquals(1, us1.until(us2, ChronoUnit.DAYS));
        assertEquals(23, us1.until(us2, ChronoUnit.HOURS));
        assertEquals(-1, us2.until(us1, ChronoUnit.DAYS));
        assertEquals(-23, us2.until(us1, ChronoUnit.HOURS));
    }

    @Test
    public void parseTest() {
        ZonedDateTime jp = ZonedDateTime.of(2013,11,3, 15,59,59,0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime jpp = ZonedDateTime.parse("2013-11-03T15:59:59+09:00[Asia/Tokyo]");
        assertEquals(jp, jpp);

        ZonedDateTime us = ZonedDateTime.of(2013,11,3, 01,59,59,0, ZoneId.of("America/Chicago"));
        ZonedDateTime usp = ZonedDateTime.parse("2013-11-03T01:59:59-05:00[America/Chicago]");
        assertEquals(us, usp);
    }
}
