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

public class CNTagTest{

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
        //String tag = "zh-CN-u-nu-hanidec"; //not supported, since it's not sequential
        String tag = "zh-CN-u-nu-fullwide";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，全角数字)", l.getDisplayName(l));
        assertEquals("Chinese (China, Full-Width Digits)", l.getDisplayName(Locale.ENGLISH));
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
        String tag = "zh-CN-u-cu-cny";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，货币：人民币)", l.getDisplayName(l));
        assertEquals("Chinese (China, Currency: Chinese Yuan)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("cu-cny", l.getExtension('u'));
        assertEquals("cny", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("CNY", currency.toString());
        assertEquals("CN¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(156, currency.getNumericCode());
    }

    @Test
    public void currencyWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"cu-cny");
        Locale l = lBuilder.build();
        assertEquals("cu-cny", l.getExtension('u'));
        assertEquals("cny", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("CNY", currency.toString());
        assertEquals("CN¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(156, currency.getNumericCode());
    }

    // CURRENCY with REGION OVERRIDE
    @Test
    public void currencyWithRegionTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"rg-cnzzzz");
        Locale l = lBuilder.build();
        assertEquals("rg-cnzzzz", l.getExtension('u'));
        assertEquals("cnzzzz", l.getUnicodeLocaleType("rg"));

        Currency currency = Currency.getInstance(l);
        assertEquals("CNY", currency.toString());
        assertEquals("CN¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(156, currency.getNumericCode());
    }

    @Test
    public void currencyWithRegion2Test(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setRegion("cn");
        Locale l = lBuilder.build();

        Currency currency = Currency.getInstance(l);
        assertEquals("CNY", currency.toString());
        assertEquals("CN¥", currency.getSymbol(Locale.ENGLISH));
        assertEquals(156, currency.getNumericCode());
    }

    // TIMEZONE
    @Test
    public void timezoneTest(){
        String tag = "zh-CN-u-tz-cnsha"; //Defined in common/bcp47/teimezone.xml
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，时区：中国时间)", l.getDisplayName(l));
        assertEquals("Chinese (China, Time Zone: China Time)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("tz-cnsha", l.getExtension('u'));
        assertEquals("cnsha", l.getUnicodeLocaleType("tz"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Shanghai", formatter.getZone().toString());
                assertEquals("Asia/Shanghai", zdt.format(formatter));
            } else {
                fail("Cannot invoke DateTimeFormatter.localizedBy(Locale)");
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void timezoneCNCalendarTest(){
        String tag = "zh-CN-u-ca-roc-tz-cnsha";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，民国纪年，时区：中国时间)", l.getDisplayName(l));
        assertEquals("Chinese (China, Minguo Calendar, Time Zone: China Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-roc-tz-cnsha", l.getExtension('u'));
        assertEquals("cnsha", l.getUnicodeLocaleType("tz"));
        assertEquals("roc", l.getUnicodeLocaleType("ca"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Shanghai"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 ah:mm:ss VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Shanghai", formatter.getZone().toString());
                assertEquals("民国108年6月1日 下午12:00:00 Asia/Shanghai", zdt.format(formatter));
            } else {
                fail("Cannot invoke DateTimeFormatter.localizedBy(Locale)");
            }
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    @Test
    public void timezoneCNCalendarNumberTest(){
        String tag = "zh-CN-u-ca-roc-nu-fullwide-tz-cnsha";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，民国纪年，全角数字，时区：中国时间)",
                     l.getDisplayName(l));
        assertEquals("Chinese (China, Minguo Calendar, Full-Width Digits, Time Zone: China Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-roc-nu-fullwide-tz-cnsha", l.getExtension('u'));
        assertEquals("cnsha", l.getUnicodeLocaleType("tz"));
        assertEquals("fullwide", l.getUnicodeLocaleType("nu"));
        assertEquals("roc", l.getUnicodeLocaleType("ca"));

        try {
            if (localizedBy != null) {
                ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Shanghai"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 ah:mm:ss VV");
                formatter = (DateTimeFormatter) localizedBy.invoke(formatter, l);
                assertEquals("Asia/Shanghai", formatter.getZone().toString());
                assertEquals("民国１０８年６月１日 下午１２:００:００ Asia/Shanghai", zdt.format(formatter));
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
        String tag = "zh-CN-u-fw-mon";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (中国，fw：mon)", l.getDisplayName(l));
        assertEquals("Chinese (China, First Day of Week Is Monday)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("fw-mon", l.getExtension('u'));
        assertEquals("mon", l.getUnicodeLocaleType("fw"));

        Calendar ca = Calendar.getInstance(l);
        assertEquals(Calendar.MONDAY, ca.getFirstDayOfWeek());
    }
}
