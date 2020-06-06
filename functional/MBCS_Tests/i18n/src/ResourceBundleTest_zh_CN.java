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
import java.util.ListResourceBundle;

public class ResourceBundleTest_zh_CN extends ListResourceBundle {
   public Object[][] getContents(){
      return contents;
   }

   static final Object[][] contents = {
      {"locale","Locale"},
      {"timezone","Time Zone"},
      {"date","Date"},
      {"number","Number"},
      {"currency","Currency"},
      {"percent","Percent"},
      {"FullFormat","y\u5e74M\u6708d\u65e5EEEE zzzz ah:mm:ss"},
      {"LongFormat","y\u5e74M\u6708d\u65e5 z ah:mm:ss"},
      {"MediumFormat","y\u5e74M\u6708d\u65e5 ah:mm:ss"},
      {"ShortFormat","y/M/d ah:mm"},
      /* for E,EE,EEE */
      {"Day1","\u65e5"},
      {"Day2","\u4e00"},
      {"Day3","\u4e8c"},
      {"Day4","\u4e09"},
      {"Day5","\u56db"},
      {"Day6","\u4e94"},
      {"Day7","\u516d"},
      /* for EEEE */
      {"Day1F","\u661f\u671f\u65e5"},
      {"Day2F","\u661f\u671f\u4e00"},
      {"Day3F","\u661f\u671f\u4e8c"},
      {"Day4F","\u661f\u671f\u4e09"},
      {"Day5F","\u661f\u671f\u56db"},
      {"Day6F","\u661f\u671f\u4e94"},
      {"Day7F","\u661f\u671f\u516d"},
      /* for MMM */
      {"Month1","1"},
      {"Month2","2"},
      {"Month3","3"},
      {"Month4","4"},
      {"Month5","5"},
      {"Month6","6"},
      {"Month7","7"},
      {"Month8","8"},
      {"Month9","9"},
      {"Month10","10"},
      {"Month11","11"},
      {"Month12","12"},
      /* for MMMM */
      {"Month1F","1\u6708"},
      {"Month2F","2\u6708"},
      {"Month3F","3\u6708"},
      {"Month4F","4\u6708"},
      {"Month5F","5\u6708"},
      {"Month6F","6\u6708"},
      {"Month7F","7\u6708"},
      {"Month8F","8\u6708"},
      {"Month9F","9\u6708"},
      {"Month10F","10\u6708"},
      {"Month11F","11\u6708"},
      {"Month12F","12\u6708"},
      /* for a */
      {"AM","\u4e0a\u5348"},
      {"PM","\u4e0b\u5348"},
   };
}
