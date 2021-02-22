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

public class LocaleFilterTest3 {
    public static void main(String[] args) throws Exception {
        PrintStream sysOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        Locale loc = Locale.getDefault();
        String langtag = loc.getLanguage()+"-"+loc.getCountry().toLowerCase(Locale.ENGLISH);
        String ranges = langtag+",en-us;q=0.4";
        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(ranges);
        Vector<String> collection = new Vector<>();
        for(Locale locale : Locale.getAvailableLocales()) collection.add(locale.toLanguageTag());
        collection.add(Locale.forLanguageTag("ja-Jpan-JP").toLanguageTag());
        collection.add(Locale.forLanguageTag("ko-Hang-KR").toLanguageTag());
        collection.add(Locale.forLanguageTag("zh-Hans-CN").toLanguageTag());
        collection.add(Locale.forLanguageTag("zh-Hant-TW").toLanguageTag());
        Collections.sort(collection);

        System.out.println("Ranges: "+ranges);
        for(Locale.LanguageRange lr : list) System.out.println(">>"+lr.getRange()+";q="+lr.getWeight());
        List<String> filter = null;
        for(Locale.FilteringMode mode : Locale.FilteringMode.values()) {
            System.out.println("  "+mode.toString());
            filter = Locale.filterTags(list, collection, mode);
            for(Object obj : filter) System.out.println("    "+obj.toString());
        }

        ranges = langtag+";q=0.1,en-us;q=0.4";
        list = Locale.LanguageRange.parse(ranges);
        System.out.println("Ranges: "+ranges);
        for(Locale.LanguageRange lr : list) System.out.println(">>"+lr.getRange()+";q="+lr.getWeight());
        for(Locale.FilteringMode mode : Locale.FilteringMode.values()) {
            System.out.println("  "+mode.toString());
            try {
                filter = Locale.filterTags(list, collection, mode);
                for(Object obj : filter) System.out.println("    "+obj.toString());
            } catch (IllegalArgumentException iae) {
                //iae.printStackTrace(System.out);
                System.out.println("    "+iae.getClass().getName()+": "+iae.getMessage());
            }
        }

        ps.close();
        baos.close();
        System.setOut(sysOut);
        String outString = baos.toString();
        if (Boolean.valueOf(System.getProperty("result","true"))) System.out.print(outString);
        Properties prop = new Properties();
        String suffix = "";
        long ver = JavaVersion.getVersion();
        if (ver >= 16000000L) suffix = "_16";
        prop.load(LocaleFilterTest3.class.
            getResourceAsStream("LocaleFilterTest3"+suffix+".properties"));
        String expected = String.format(prop.getProperty(loc.toString()));
        if (Boolean.valueOf(System.getProperty("expected","false"))) System.out.printf("--- expected ---%n"+expected);
        System.out.println("Test: "+(expected.equals(outString) ? "Passed" : "Failed"));
    }
}
