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

public class LocaleFilterTest2 {
    public static void main(String[] args) throws Exception {
        PrintStream sysOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        ArrayList<Locale.LanguageRange> list = new ArrayList<>();
        Vector<String> collection = new Vector<>();
        Locale loc = Locale.getDefault();
        for(Locale locale : Locale.getAvailableLocales()) collection.add(locale.toLanguageTag());
        collection.add(Locale.forLanguageTag("ja-Jpan-JP").toLanguageTag());
        collection.add(Locale.forLanguageTag("ko-Hang-KR").toLanguageTag());
        collection.add(Locale.forLanguageTag("zh-Hans-CN").toLanguageTag());
        collection.add(Locale.forLanguageTag("zh-Hant-TW").toLanguageTag());
        Collections.sort(collection);

        List<String> filter = null;
        String range = loc.getLanguage()+"-"+loc.getCountry();
        System.out.println("Language Priority List: "+range);
        list.add(new Locale.LanguageRange(range));
        for(Locale.FilteringMode mode : Locale.FilteringMode.values()) {
            System.out.println("  "+mode.toString());
            filter = Locale.filterTags(list, collection, mode);
            for(Object obj : filter) System.out.println("    "+obj.toString());
        }
        list.clear();

        range = loc.getLanguage()+"-*-"+loc.getCountry();
        System.out.println("Language Priority List: "+range);
        list.add(new Locale.LanguageRange(range));
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
        prop.load(LocaleFilterTest2.class.
            getResourceAsStream("LocaleFilterTest2.properties"));
        String expected = String.format(prop.getProperty(loc.toString()));
        if (Boolean.valueOf(System.getProperty("expected","false"))) System.out.printf("--- expected ---%n"+expected);
        System.out.println("Test: "+(expected.equals(outString) ? "Passed" : "Failed"));
    }
}
