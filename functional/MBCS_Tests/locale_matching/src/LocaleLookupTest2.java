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

public class LocaleLookupTest2 {
    public static void main(String[] args) throws Exception {
        PrintStream sysOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);

        Locale loc = Locale.getDefault();
        TreeSet<String> tagCol = new TreeSet<>();
        for(Locale locale : Locale.getAvailableLocales()) tagCol.add(locale.toLanguageTag());
        Hashtable<String,List<String>> tags = new Hashtable<>();
        tags.put(Locale.JAPAN.toLanguageTag(),(List<String>)Arrays.asList("ja-Jpan-JP","ja-Jpan","ja-JP","ja"));
        tags.put(Locale.CHINA.toLanguageTag(),(List<String>)Arrays.asList("zh-Hans-CN","zh-Hans","zh-CN","zh"));
        tags.put(Locale.TAIWAN.toLanguageTag(),(List<String>)Arrays.asList("zh-Hant-TW","zh-Hant","zh-TW"));
        tags.put(Locale.KOREA.toLanguageTag(),(List<String>)Arrays.asList("ko-Hang-KR","ko-Hang","ko-KR","ko"));
        tags.put(Locale.US.toLanguageTag(),(List<String>)Arrays.asList("en-Latn-US","en-Latn","en-us","en"));
        for(List l : tags.values()) {
            for(Object s : l) {
                tagCol.add(Locale.forLanguageTag((String)s).toLanguageTag());
            }
        }

        //System.out.println(Arrays.toString(tagCol.toArray()));
        String ranges = tags.get(loc.toLanguageTag()).get(0)+",en-us;q=0.5";
        System.out.println("Ranges: "+ranges);
        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(ranges);
        while(true) {
            String tag = Locale.lookupTag(list, tagCol);
            if (null == tag) break;
            System.out.println("  LanguageTag="+tag);
            tagCol.remove(Locale.forLanguageTag(tag).toLanguageTag());
        }

        ps.close();
        baos.close();
        System.setOut(sysOut);
        String outString = baos.toString();
        if (Boolean.valueOf(System.getProperty("result","true"))) System.out.print(outString);
        Properties prop = new Properties();
        prop.load(LocaleLookupTest2.class.
            getResourceAsStream("LocaleLookupTest2.properties"));
        String expected = String.format(prop.getProperty(loc.toString()));
        if (Boolean.valueOf(System.getProperty("expected","false"))) System.out.printf("--- expected ---%n"+expected);
        System.out.println("Test: "+(expected.equals(outString) ? "Passed" : "Failed"));
    }
}
