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

public class ResourceBundleTest_en extends ListResourceBundle {
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
      {"FullFormat","EEEE, MMMM d, y 'at' h:mm:ss a zzzz"},
      {"LongFormat","MMMM d, y 'at' h:mm:ss a z"},
      {"MediumFormat","MMM d, y, h:mm:ss a"},
      {"ShortFormat","M/d/yy, h:mm a"},
      /* for E,EE,EEE */
      {"Day1","Sun"},
      {"Day2","Mon"},
      {"Day3","Tue"},
      {"Day4","Wed"},
      {"Day5","Thu"},
      {"Day6","Fri"},
      {"Day7","Sat"},
      /* for EEEE */
      {"Day1F","Sunday"},
      {"Day2F","Monday"},
      {"Day3F","Tuesday"},
      {"Day4F","Wednesday"},
      {"Day5F","Thursday"},
      {"Day6F","Friday"},
      {"Day7F","Saturday"},
      /* for MMM */
      {"Month1","Jan"},
      {"Month2","Feb"},
      {"Month3","Mar"},
      {"Month4","Apr"},
      {"Month5","May"},
      {"Month6","Jun"},
      {"Month7","Jul"},
      {"Month8","Aug"},
      {"Month9","Sep"},
      {"Month10","Oct"},
      {"Month11","Nov"},
      {"Month12","Dec"},
      /* for MMMM */
      {"Month1F","January"},
      {"Month2F","February"},
      {"Month3F","March"},
      {"Month4F","April"},
      {"Month5F","May"},
      {"Month6F","June"},
      {"Month7F","July"},
      {"Month8F","August"},
      {"Month9F","September"},
      {"Month10F","October"},
      {"Month11F","November"},
      {"Month12F","December"},
      /* for a */
      {"AM","AM"},
      {"PM","PM"},
   };
}
