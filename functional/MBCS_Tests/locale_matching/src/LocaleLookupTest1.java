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

import java.util.*;
import java.io.*;

public class LocaleLookupTest1 {
    public static void main(String[] args) throws Exception {
        PrintStream sysOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        Locale loc = Locale.getDefault();
        String langtag = loc.getLanguage()+"-"+loc.getCountry().toLowerCase(Locale.ENGLISH);
        String ranges = langtag+",en-us;q=0.4";
        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(ranges);
        Vector<Locale> localeCol = new Vector<>();
        Vector<String> tagCol = new Vector<>();
        for(Locale locale : Locale.getAvailableLocales()) localeCol.add(locale);
        localeCol.add(Locale.forLanguageTag("ja-Jpan-JP"));
        localeCol.add(Locale.forLanguageTag("ko-Hang-KR"));
        localeCol.add(Locale.forLanguageTag("zh-Hans-CN"));
        localeCol.add(Locale.forLanguageTag("zh-Hant-TW"));
        for(Locale locale : Locale.getAvailableLocales()) tagCol.add(locale.toLanguageTag());
        tagCol.add(Locale.forLanguageTag("ja-Jpan-JP").toLanguageTag());
        tagCol.add(Locale.forLanguageTag("ko-Hang-KR").toLanguageTag());
        tagCol.add(Locale.forLanguageTag("zh-Hans-CN").toLanguageTag());
        tagCol.add(Locale.forLanguageTag("zh-Hant-TW").toLanguageTag());
        System.out.println("Ranges: "+ranges);
        for(Locale.LanguageRange lr : list) System.out.println(">>"+lr.getRange()+";q="+lr.getWeight());
        System.out.println("  Locale="+Locale.lookup(list, localeCol));
        System.out.println("  LanguageTag="+Locale.lookupTag(list, tagCol));

        ranges = langtag+";q=0.1,en-us;q=0.4";
        list = Locale.LanguageRange.parse(ranges);
        System.out.println("Ranges: "+ranges);
        for(Locale.LanguageRange lr : list) System.out.println(">>"+lr.getRange()+";q="+lr.getWeight());
        System.out.println("  Locale="+Locale.lookup(list, localeCol));
        System.out.println("  LanguageTag="+Locale.lookupTag(list, tagCol));

        ps.close();
        baos.close();
        System.setOut(sysOut);
        String outString = baos.toString();
        if (Boolean.valueOf(System.getProperty("result","true"))) System.out.print(outString);
        Properties prop = new Properties();
        prop.load(LocaleLookupTest1.class.
            getResourceAsStream("LocaleLookupTest1.properties"));
        String expected = String.format(prop.getProperty(loc.toString()));
        if (Boolean.valueOf(System.getProperty("expected","false"))) System.out.printf("--- expected ---%n"+expected);
        System.out.println("Test: "+(expected.equals(outString) ? "Passed" : "Failed"));
    }
}
