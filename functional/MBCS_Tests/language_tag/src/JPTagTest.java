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
import java.lang.reflect.Method;

public class JPTagTest{

    private static Method localizedBy = null;
    static {
        try {
            localizedBy = DateTimeFormatter.class
                .getDeclaredMethod("localizedBy", Locale.class);
        } catch (NoSuchMethodException nsme) {
        }
    }

    // NUMBER
    @Test
    public void numberTest(){
        //String tag = "ja-JP-u-nu-hanidec"; //not supported, since it's not sequential
        String tag = "ja-JP-u-nu-fullwide";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、全角数字)", l.getDisplayName(l));
        assertEquals("Japanese (Japan, Full-Width Digits)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("nu-fullwide", l.getExtension('u'));
        assertEquals("fullwide", l.getUnicodeLocaleType("nu"));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(l);
        char zero = symbols.getZeroDigit();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append((char)(zero+i));
        }
        assertEquals("０１２３４５６７８９",builder.toString());
    }

    @Test
    public void numberWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"nu-fullwide");
        Locale l = lBuilder.build();
        assertEquals("nu-fullwide", l.getExtension('u'));
        assertEquals("fullwide", l.getUnicodeLocaleType("nu"));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(l);
        char zero = symbols.getZeroDigit();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append((char)(zero+i));
        }
        assertEquals("０１２３４５６７８９",builder.toString());
    }

    // CURRENCY
    @Test
    public void currencyTest(){
        String tag = "ja-JP-u-cu-jpy";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、通貨: 日本円)", l.getDisplayName(l));
        assertEquals("Japanese (Japan, Currency: Japanese Yen)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("cu-jpy", l.getExtension('u'));
        assertEquals("jpy", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("JPY", currency.toString());
        assertEquals("¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(392, currency.getNumericCode());
    }

    @Test
    public void currencyWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"cu-jpy");
        Locale l = lBuilder.build();
        assertEquals("cu-jpy", l.getExtension('u'));
        assertEquals("jpy", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("JPY", currency.toString());
        assertEquals("¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(392, currency.getNumericCode());
    }

    // CURRENCY with REGION OVERRIDE
    @Test
    public void currencyWithRegionTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"rg-jpzzzz");
        Locale l = lBuilder.build();
        assertEquals("rg-jpzzzz", l.getExtension('u'));
        assertEquals("jpzzzz", l.getUnicodeLocaleType("rg"));

        Currency currency = Currency.getInstance(l);
        assertEquals("JPY", currency.toString());
        assertEquals("¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(392, currency.getNumericCode());
    }

    @Test
    public void currencyWithRegion2Test(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setRegion("jp");
        Locale l = lBuilder.build();

        Currency currency = Currency.getInstance(l);
        assertEquals("JPY", currency.toString());
        assertEquals("¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(392, currency.getNumericCode());
    }

    // TIMEZONE
    @Test
    public void timezoneTest(){
        String tag = "ja-JP-u-tz-jptyo"; //Defined in common/bcp47/teimezone.xml
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、タイムゾーン: 日本時間)", l.getDisplayName(l));
        assertEquals("Japanese (Japan, Time Zone: Japan Time)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("tz-jptyo", l.getExtension('u'));
        assertEquals("jptyo", l.getUnicodeLocaleType("tz"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Tokyo", formatter.getZone().toString());
                assertEquals("Asia/Tokyo", zdt.format(formatter));
            } else {
                fail("Cannot invoke DateTimeFormatter.localizedBy(Locale)");
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void timezoneJPCalendarTest(){
        String tag = "ja-JP-u-ca-japanese-tz-jptyo";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、和暦、タイムゾーン: 日本時間)", l.getDisplayName(l));
        assertEquals("Japanese (Japan, Japanese Calendar, Time Zone: Japan Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-tz-jptyo", l.getExtension('u'));
        assertEquals("jptyo", l.getUnicodeLocaleType("tz"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Tokyo"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 HH時mm分ss秒 VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Tokyo", formatter.getZone().toString());
                assertEquals("令和1年6月1日 12時00分00秒 Asia/Tokyo", zdt.format(formatter));
            } else {
                fail("Cannot invoke DateTimeFormatter.localizedBy(Locale)");
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void timezoneJPCalendarNumberTest(){
        String tag = "ja-JP-u-ca-japanese-nu-fullwide-tz-jptyo";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、和暦、全角数字、タイムゾーン: 日本時間)",
                     l.getDisplayName(l));
        assertEquals("Japanese (Japan, Japanese Calendar, Full-Width Digits, Time Zone: Japan Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-japanese-nu-fullwide-tz-jptyo", l.getExtension('u'));
        assertEquals("jptyo", l.getUnicodeLocaleType("tz"));
        assertEquals("fullwide", l.getUnicodeLocaleType("nu"));
        assertEquals("japanese", l.getUnicodeLocaleType("ca"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Tokyo"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 HH時mm分ss秒 VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Tokyo", formatter.getZone().toString());
                assertEquals("令和１年６月１日 １２時００分００秒 Asia/Tokyo", zdt.format(formatter));
            } else {
                fail("Cannot invoke DateTimeFormatter.localizedBy(Locale)");
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    // FIRST DAY of WEEK
    @Test
    public void firstDayOfWeekTest(){
        String tag = "ja-JP-u-fw-mon";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("日本語 (日本、fw: mon)", l.getDisplayName(l));
        assertEquals("Japanese (Japan, First Day of Week Is Monday)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("fw-mon", l.getExtension('u'));
        assertEquals("mon", l.getUnicodeLocaleType("fw"));

        Calendar ca = Calendar.getInstance(l);
        assertEquals(Calendar.MONDAY, ca.getFirstDayOfWeek());
    }
}
