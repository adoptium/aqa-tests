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

import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.util.Set;
import java.util.Locale;

public class ChronoTest{
    @Test
    public void getAvailableChronologiesTest(){
        Set<Chronology> set = Chronology.getAvailableChronologies();
        assertTrue(set.contains(JapaneseChronology.INSTANCE));
        assertTrue(set.contains(IsoChronology.INSTANCE));

        assertEquals(IsoChronology.INSTANCE, Chronology.of("ISO"));
        assertEquals(JapaneseChronology.INSTANCE, Chronology.of("Japanese"));
        assertEquals(MinguoChronology.INSTANCE, Chronology.of("Minguo"));
    }
    @Test
    public void ofLocaleTest(){
        assertEquals(JapaneseChronology.INSTANCE, Chronology.ofLocale(new Locale("ja","JP","JP")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("ja","JP")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("ja")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("ko","KR")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("ko")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("zh","CN")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("zh")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("zh","TW")));
        assertEquals(IsoChronology.INSTANCE, Chronology.ofLocale(new Locale("zh","TW","TW")));

        assertEquals(JapaneseChronology.INSTANCE, Chronology.ofLocale(Locale.forLanguageTag("ja-JP-u-ca-japanese")));
        assertEquals(JapaneseChronology.INSTANCE, Chronology.ofLocale(Locale.forLanguageTag("en-US-u-ca-japanese")));
        assertEquals(MinguoChronology.INSTANCE, Chronology.ofLocale(Locale.forLanguageTag("zh-TW-u-ca-roc")));
        assertEquals(MinguoChronology.INSTANCE, Chronology.ofLocale(Locale.forLanguageTag("en-US-u-ca-roc")));
    }
}
