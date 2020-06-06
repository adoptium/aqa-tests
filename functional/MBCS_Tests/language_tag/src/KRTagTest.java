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

import java.util.Currency;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Calendar;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class KRTagTest{
    // NUMBER
    @Test
    public void numberTest(){
        String tag = "ko-KR-u-nu-latn";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("한국어 (대한민국, 서양 숫자)", l.getDisplayName(l));
        assertEquals("Korean (South Korea, Western Digits)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("nu-latn", l.getExtension('u'));
        assertEquals("latn", l.getUnicodeLocaleType("nu"));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(l);
        char zero = symbols.getZeroDigit();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append((char)(zero+i));
        }
        assertEquals("0123456789",builder.toString());
    }

    @Test
    public void numberWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"nu-latn");
        Locale l = lBuilder.build();
        assertEquals("nu-latn", l.getExtension('u'));
        assertEquals("latn", l.getUnicodeLocaleType("nu"));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(l);
        char zero = symbols.getZeroDigit();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append((char)(zero+i));
        }
        assertEquals("0123456789",builder.toString());
    }

    // CURRENCY
    @Test
    public void currencyTest(){
        String tag = "ko-KR-u-cu-krw";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("한국어 (대한민국, 통화: 대한민국 원)", l.getDisplayName(l));
        assertEquals("Korean (South Korea, Currency: South Korean Won)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("cu-krw", l.getExtension('u'));
        assertEquals("krw", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("KRW", currency.toString());
        assertEquals("₩", currency.getSymbol(Locale.ENGLISH));
        assertEquals(410, currency.getNumericCode());
    }

    @Test
    public void currencyWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"cu-krw");
        Locale l = lBuilder.build();
        assertEquals("cu-krw", l.getExtension('u'));
        assertEquals("krw", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("KRW", currency.toString());
        assertEquals("₩", currency.getSymbol(Locale.ENGLISH));
        assertEquals(410, currency.getNumericCode());
    }

    // CURRENCY with REGION OVERRIDE
    @Test
    public void currencyWithRegionTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"rg-krzzzz");
        Locale l = lBuilder.build();
        assertEquals("rg-krzzzz", l.getExtension('u'));
        assertEquals("krzzzz", l.getUnicodeLocaleType("rg"));

        Currency currency = Currency.getInstance(l);
        assertEquals("KRW", currency.toString());
        assertEquals("₩", currency.getSymbol(Locale.ENGLISH));
        assertEquals(410, currency.getNumericCode());
    }

    @Test
    public void currencyWithRegion2Test(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setRegion("kr");
        Locale l = lBuilder.build();

        Currency currency = Currency.getInstance(l);
        assertEquals("KRW", currency.toString());
        assertEquals("₩", currency.getSymbol(Locale.ENGLISH));
        assertEquals(410, currency.getNumericCode());
    }

    // TIMEZONE
    @Test
    public void timezoneTest(){
        String tag = "ko-KR-u-tz-krsel"; //Defined in common/bcp47/teimezone.xml
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("Korean (South Korea, Time Zone: Korean Time)", 
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("tz-krsel", l.getExtension('u'));
        assertEquals("krsel", l.getUnicodeLocaleType("tz"));

        ZonedDateTime zdt = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("VV");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Seoul", formatter.getZone().toString());
        assertEquals("Asia/Seoul", zdt.format(formatter));
    }

    @Test
    public void timezoneJPCalendarTest(){
        String tag = "ko-KR-u-ca-japanese-tz-krsel";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("Korean (South Korea, Japanese Calendar, Time Zone: Korean Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-tz-krsel", l.getExtension('u'));
        assertEquals("krsel", l.getUnicodeLocaleType("tz"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy/M/d/ HH:mm:ss VV");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Seoul", formatter.getZone().toString());
        assertEquals("레이와1/6/1/ 12:00:00 Asia/Seoul", zdt.format(formatter));
    }

    @Test
    public void timezoneJPCalendarNumberTest(){
        String tag = "ko-KR-u-ca-japanese-nu-latn-tz-krsel";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("Korean (South Korea, Japanese Calendar, Western Digits, Time Zone: Korean Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-nu-latn-tz-krsel", l.getExtension('u'));
        assertEquals("krsel", l.getUnicodeLocaleType("tz"));
        assertEquals("latn", l.getUnicodeLocaleType("nu"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy/M/d HH:mm:ss VV");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Seoul", formatter.getZone().toString());
        assertEquals("레이와1/6/1 12:00:00 Asia/Seoul", zdt.format(formatter));
    }

    // FIRST DAY of WEEK
    @Test
    public void firstDayOfWeekTest(){
        String tag = "ko-KR-u-fw-mon";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("한국어 (대한민국, fw: mon)",
                     l.getDisplayName(l));
        assertEquals("Korean (South Korea, First Day of Week Is Monday)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("fw-mon", l.getExtension('u'));
        assertEquals("mon", l.getUnicodeLocaleType("fw"));

        Calendar ca = Calendar.getInstance(l);
        assertEquals(Calendar.MONDAY, ca.getFirstDayOfWeek());
    }
}
