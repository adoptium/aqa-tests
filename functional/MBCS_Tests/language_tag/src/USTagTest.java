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

public class USTagTest{
    // NUMBER
    @Test
    public void numberTest(){
        String tag = "en-US-u-nu-latn";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, Western Digits)", l.getDisplayName(l));
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
        String tag = "en-US-u-cu-usd";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, Currency: US Dollar)", l.getDisplayName(l));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("cu-usd", l.getExtension('u'));
        assertEquals("usd", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("USD", currency.toString());
        assertEquals("$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(840, currency.getNumericCode());
    }

    @Test
    public void currencyWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"cu-usd");
        Locale l = lBuilder.build();
        assertEquals("cu-usd", l.getExtension('u'));
        assertEquals("usd", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("USD", currency.toString());
        assertEquals("$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(840, currency.getNumericCode());
    }

    // CURRENCY with REGION OVERRIDE
    @Test
    public void currencyWithRegionTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"rg-uszzzz");
        Locale l = lBuilder.build();
        assertEquals("rg-uszzzz", l.getExtension('u'));
        assertEquals("uszzzz", l.getUnicodeLocaleType("rg"));

        Currency currency = Currency.getInstance(l);
        assertEquals("USD", currency.toString());
        assertEquals("$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(840, currency.getNumericCode());
    }

    @Test
    public void currencyWithRegion2Test(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setRegion("us");
        Locale l = lBuilder.build();

        Currency currency = Currency.getInstance(l);
        assertEquals("USD", currency.toString());
        assertEquals("$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(840, currency.getNumericCode());
    }

    // TIMEZONE
    @Test
    public void timezoneTest(){
        String tag = "en-US-u-tz-usnyc"; //Defined in common/bcp47/teimezone.xml
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, Time Zone: Eastern Time)", 
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("tz-usnyc", l.getExtension('u'));
        assertEquals("usnyc", l.getUnicodeLocaleType("tz"));

        ZonedDateTime zdt = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("VV");
        formatter = formatter.localizedBy(l);
        assertEquals("America/New_York", formatter.getZone().toString());
        assertEquals("America/New_York", zdt.format(formatter));
    }

    @Test
    public void timezoneJPCalendarTest(){
        String tag = "en-US-u-ca-japanese-tz-usnyc";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, Japanese Calendar, Time Zone: Eastern Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-tz-usnyc", l.getExtension('u'));
        assertEquals("usnyc", l.getUnicodeLocaleType("tz"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy/M/d/ HH:mm:ss VV");
        formatter = formatter.localizedBy(l);
        assertEquals("America/New_York", formatter.getZone().toString());
        assertEquals("Reiwa1/6/1/ 12:00:00 America/New_York", zdt.format(formatter));
    }

    @Test
    public void timezoneJPCalendarNumberTest(){
        String tag = "en-US-u-ca-japanese-nu-latn-tz-usnyc";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, Japanese Calendar, Western Digits, Time Zone: Eastern Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-nu-latn-tz-usnyc", l.getExtension('u'));
        assertEquals("usnyc", l.getUnicodeLocaleType("tz"));
        assertEquals("latn", l.getUnicodeLocaleType("nu"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("America/New_York"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy/M/d HH:mm:ss VV");
        formatter = formatter.localizedBy(l);
        assertEquals("America/New_York", formatter.getZone().toString());
        assertEquals("Reiwa1/6/1 12:00:00 America/New_York", zdt.format(formatter));
    }

    // FIRST DAY of WEEK
    @Test
    public void firstDayOfWeekTest(){
        String tag = "en-US-u-fw-mon";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("English (United States, First Day of Week Is Monday)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("fw-mon", l.getExtension('u'));
        assertEquals("mon", l.getUnicodeLocaleType("fw"));

        Calendar ca = Calendar.getInstance(l);
        assertEquals(Calendar.MONDAY, ca.getFirstDayOfWeek());
    }
}
