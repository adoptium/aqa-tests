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

public class ResourceBundleTest_ko extends ListResourceBundle {
   public Object[][] getContents(){
      return contents;
   }

   /* *_4 is for Java1.4.x */
   static final Object[][] contents = {
      {"locale","\uc5b8\uc5b4"},
      {"timezone","\uc2dc\uac04"},
      {"date","\ub0a0\uc9dc"},
      {"number","\uc22b\uc790"},
      {"currency","\ud1b5\ud654"},
      {"percent","\ud37c\uc13c\ud2b8"},
      {"FullFormat","y\ub144 M\uc6d4 d\uc77c EEEE a h\uc2dc m\ubd84 s\ucd08 zzzz"},
      {"LongFormat","y\ub144 M\uc6d4 d\uc77c a h\uc2dc m\ubd84 s\ucd08 z"},
      {"MediumFormat","y. M. d. a h:mm:ss"},
      {"ShortFormat","yy. M. d. a h:mm"},
      /* for E,EE,EEE */
      {"Day1","\uc77c"},
      {"Day2","\uc6d4"},
      {"Day3","\ud654"},
      {"Day4","\uc218"},
      {"Day5","\ubaa9"},
      {"Day6","\uae08"},
      {"Day7","\ud1a0"},
      /* for EEEE */
      {"Day1F","\uc77c\uc694\uc77c"},
      {"Day2F","\uc6d4\uc694\uc77c"},
      {"Day3F","\ud654\uc694\uc77c"},
      {"Day4F","\uc218\uc694\uc77c"},
      {"Day5F","\ubaa9\uc694\uc77c"},
      {"Day6F","\uae08\uc694\uc77c"},
      {"Day7F","\ud1a0\uc694\uc77c"},
      /* for MMM */
      {"Month1","1\uc6d4"},
      {"Month2","2\uc6d4"},
      {"Month3","3\uc6d4"},
      {"Month4","4\uc6d4"},
      {"Month5","5\uc6d4"},
      {"Month6","6\uc6d4"},
      {"Month7","7\uc6d4"},
      {"Month8","8\uc6d4"},
      {"Month9","9\uc6d4"},
      {"Month10","10\uc6d4"},
      {"Month11","11\uc6d4"},
      {"Month12","12\uc6d4"},
      /* for MMMM */
      {"Month1F","1\uc6d4"},
      {"Month2F","2\uc6d4"},
      {"Month3F","3\uc6d4"},
      {"Month4F","4\uc6d4"},
      {"Month5F","5\uc6d4"},
      {"Month6F","6\uc6d4"},
      {"Month7F","7\uc6d4"},
      {"Month8F","8\uc6d4"},
      {"Month9F","9\uc6d4"},
      {"Month10F","10\uc6d4"},
      {"Month11F","11\uc6d4"},
      {"Month12F","12\uc6d4"},
      /* for a */
      {"AM","\uc624\uc804"},
      {"PM","\uc624\ud6c4"},
   };
}
