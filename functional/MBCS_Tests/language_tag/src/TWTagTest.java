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

public class TWTagTest{
    // NUMBER
    @Test
    public void numberTest(){
        //String tag = "zh-TW-u-nu-hanidec"; //not supported, since it's not sequential
        String tag = "zh-TW-u-nu-fullwide";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (台灣，全形數字)", l.getDisplayName(l));
        assertEquals("Chinese (Taiwan, Full-Width Digits)", l.getDisplayName(Locale.ENGLISH));
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
        String tag = "zh-TW-u-cu-hkd";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (台灣，貨幣：港幣)", l.getDisplayName(l));
        assertEquals("Chinese (Taiwan, Currency: Hong Kong Dollar)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("cu-hkd", l.getExtension('u'));
        assertEquals("hkd", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("HKD", currency.toString());
        assertEquals("HK$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(344, currency.getNumericCode());
    }

    @Test
    public void currencyWithBuilderTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"cu-hkd");
        Locale l = lBuilder.build();
        assertEquals("cu-hkd", l.getExtension('u'));
        assertEquals("hkd", l.getUnicodeLocaleType("cu"));

        Currency currency = Currency.getInstance(l);
        assertEquals("HKD", currency.toString());
        assertEquals("HK$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(344, currency.getNumericCode());
    }

    // CURRENCY with REGION OVERRIDE
    @Test
    public void currencyWithRegionTest(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setExtension('u',"rg-twzzzz");
        Locale l = lBuilder.build();
        assertEquals("rg-twzzzz", l.getExtension('u'));
        assertEquals("twzzzz", l.getUnicodeLocaleType("rg"));

        Currency currency = Currency.getInstance(l);
        assertEquals("TWD", currency.toString());
        assertEquals("NT$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(901, currency.getNumericCode());
    }

    @Test
    public void currencyWithRegion2Test(){
        Builder lBuilder = new Builder().setLocale(Locale.getDefault());
        lBuilder.setRegion("tw");
        Locale l = lBuilder.build();

        Currency currency = Currency.getInstance(l);
        assertEquals("TWD", currency.toString());
        assertEquals("NT$", currency.getSymbol(Locale.ENGLISH));
        assertEquals(901, currency.getNumericCode());
    }

    // TIMEZONE
    @Test
    public void timezoneTest(){
        String tag = "zh-TW-u-tz-twtpe"; //Defined in common/bcp47/teimezone.xml
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (台灣，時區：台北時間)", l.getDisplayName(l));
        assertEquals("Chinese (Taiwan, Time Zone: Taipei Time)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("tz-twtpe", l.getExtension('u'));
        assertEquals("twtpe", l.getUnicodeLocaleType("tz"));

        ZonedDateTime zdt = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("VV");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Taipei", formatter.getZone().toString());
        assertEquals("Asia/Taipei", zdt.format(formatter));
    }

    @Test
    public void timezoneTWCalendarTest(){
        String tag = "zh-TW-u-ca-roc-tz-twtpe";
        Locale l = Locale.forLanguageTag(tag);
        long ver = JavaVersion.getVersion();
        if (ver >= 16000000L) {
            assertEquals("中文 (台灣，國曆，時區：台北時間)", l.getDisplayName(l));
        } else {
            assertEquals("中文 (台灣，民國曆，時區：台北時間)", l.getDisplayName(l));
        }
        assertEquals("Chinese (Taiwan, Minguo Calendar, Time Zone: Taipei Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-roc-tz-twtpe", l.getExtension('u'));
        assertEquals("twtpe", l.getUnicodeLocaleType("tz"));
        assertEquals("roc", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Taipei"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 EEEE ah:mm:ss [zzzz]");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Taipei", formatter.getZone().toString());
        assertEquals("民國108年6月1日 星期六 下午12:00:00 台北標準時間", zdt.format(formatter));
    }

    @Test
    public void timezoneTWCalendarNumberTest(){
        String tag = "zh-TW-u-ca-roc-nu-fullwide-tz-twtpe";
        Locale l = Locale.forLanguageTag(tag);
        long ver = JavaVersion.getVersion();
        if (ver >= 16000000L) {
            assertEquals("中文 (台灣，國曆，全形數字，時區：台北時間)",
                         l.getDisplayName(l));
        } else {
            assertEquals("中文 (台灣，民國曆，全形數字，時區：台北時間)",
                         l.getDisplayName(l));
        }
        assertEquals("Chinese (Taiwan, Minguo Calendar, Full-Width Digits, Time Zone: Taipei Time)",
                     l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("ca-roc-nu-fullwide-tz-twtpe", l.getExtension('u'));
        assertEquals("twtpe", l.getUnicodeLocaleType("tz"));
        assertEquals("fullwide", l.getUnicodeLocaleType("nu"));
        assertEquals("roc", l.getUnicodeLocaleType("ca"));

        ZonedDateTime zdt = ZonedDateTime.of(2019,6,1,12,0,0,0,ZoneId.of("Asia/Taipei"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("GGGGy年M月d日 EEEE ah:mm:ss [zzzz]");
        formatter = formatter.localizedBy(l);
        assertEquals("Asia/Taipei", formatter.getZone().toString());
        assertEquals("民國１０８年６月１日 星期六 下午１２:００:００ 台北標準時間", zdt.format(formatter));
    }

    // FIRST DAY of WEEK
    @Test
    public void firstDayOfWeekTest(){
        String tag = "zh-TW-u-fw-mon";
        Locale l = Locale.forLanguageTag(tag);
        assertEquals("中文 (台灣，fw：mon)", l.getDisplayName(l));
        assertEquals("Chinese (Taiwan, First Day of Week Is Monday)", l.getDisplayName(Locale.ENGLISH));
        assertEquals(tag, l.toLanguageTag());
        assertEquals("fw-mon", l.getExtension('u'));
        assertEquals("mon", l.getUnicodeLocaleType("fw"));

        Calendar ca = Calendar.getInstance(l);
        assertEquals(Calendar.MONDAY, ca.getFirstDayOfWeek());
    }
}
