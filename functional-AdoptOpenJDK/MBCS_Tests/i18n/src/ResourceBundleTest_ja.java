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

public class ResourceBundleTest_ja extends ListResourceBundle {
   public Object[][] getContents(){
      return contents;
   }

   static final Object[][] contents = {
      {"locale","\u5730\u57df"},
      {"timezone","\u6642\u9593\u5e2f"},
      {"date","\u65e5\u4ed8"},
      {"number","\u6570\u5024"},
      {"currency","\u901a\u8ca8"},
      {"percent","\u5272\u5408(\u30d1\u30fc\u30bb\u30f3\u30c8)"},
      {"FullFormat","y\u5e74M\u6708d\u65e5EEEE H\u6642mm\u5206ss\u79d2 zzzz"},
      {"LongFormat","y\u5e74M\u6708d\u65e5 H:mm:ss z"},
      {"MediumFormat","y/MM/dd H:mm:ss"},
      {"ShortFormat","y/MM/dd H:mm"},
      /* for E,EE,EEE */
      {"Day1","\u65e5"},
      {"Day2","\u6708"},
      {"Day3","\u706b"},
      {"Day4","\u6c34"},
      {"Day5","\u6728"},
      {"Day6","\u91d1"},
      {"Day7","\u571f"},
      /* for EEEE */
      {"Day1F","\u65e5\u66dc\u65e5"},
      {"Day2F","\u6708\u66dc\u65e5"},
      {"Day3F","\u706b\u66dc\u65e5"},
      {"Day4F","\u6c34\u66dc\u65e5"},
      {"Day5F","\u6728\u66dc\u65e5"},
      {"Day6F","\u91d1\u66dc\u65e5"},
      {"Day7F","\u571f\u66dc\u65e5"},
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
      {"AM","\u5348\u524d"},
      {"PM","\u5348\u5f8c"},
   };
}
