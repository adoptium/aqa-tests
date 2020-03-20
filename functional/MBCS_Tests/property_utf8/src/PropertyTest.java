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

import java.io.*;
import java.util.Properties;
import java.util.Locale;
import java.util.ResourceBundle;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class PropertyTest {
    @Test
    public void jpUTF8() throws Exception {
        utf8Resource("ja-JP");
    }
    @Test
    public void jpInvalid() throws Exception {
        invalidProperty("ja-JP");
    }
    @Test
    public void koUTF8() throws Exception {
        utf8Resource("ko-KR");
    }
    @Test
    public void koInvalid() throws Exception {
        invalidProperty("ko-KR");
    }
    @Test
    public void cnUTF8() throws Exception {
        utf8Resource("zh-CN");
    }
    @Test
    public void cnInvalid() throws Exception {
        invalidProperty("zh-CN");
    }
    @Test
    public void twUTF8() throws Exception {
        utf8Resource("zh-TW");
    }
    @Test
    public void twInvalid() throws Exception {
        invalidProperty("zh-TW");
    }

    private void utf8Resource(String localeString) throws Exception {
        Locale l = Locale.forLanguageTag(localeString);
        String f = "CLDR11_"+l.getLanguage()+"_"+l.getCountry()+".properties";
        String BASE = System.getenv("BASE");
        if (BASE == null) BASE = "";

        ResourceBundle bundle = ResourceBundle.getBundle("CLDR11", l);

        Files.lines(Paths.get(BASE + f), StandardCharsets.UTF_8)
             .filter(s -> s.contains("="))
             .forEach(s -> {
                 String[] v = s.split("=", 2);
                 String direct = v[1].replace("\\\\","\\");
                 String b = bundle.getString(v[0]);
                 assertEquals(b, direct);
             });
    }

    private void invalidProperty(String localeString) throws Exception {
        Locale l = Locale.forLanguageTag(localeString);
        String f = "invalid.properties";
        String BASE = System.getenv("BASE");
        if (BASE == null) BASE = "";
        ResourceBundle bundle = ResourceBundle.getBundle("invalid", l);

        // Invalid property file should be read as ISO-8859-1
        Files.lines(Paths.get(BASE + f), StandardCharsets.ISO_8859_1)
             .filter(s -> s.contains("="))
             .forEach(s -> {
                 String[] v = s.split("=", 2);
                 String direct = v[1].replace("\\\\","\\");
                 String bundleString = bundle.getString(v[0]);
                 for (byte abyte : bundleString.getBytes(Charset.forName("ISO-8859-1"))) {
                     System.out.printf("%2x", abyte);
                 }
                 assertEquals(bundleString, direct);
             });
    }
}
