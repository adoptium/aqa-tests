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

public class CheckZHTW {
  private final static boolean result;
  static {
    result = Locale.TAIWAN.equals(Locale.getDefault())
      && "HAST".equals(TimeZone.getTimeZone("US/Aleutian").getDisplayName(false, TimeZone.SHORT));
  }
  public static void main(String[] args) {
    System.exit(result ? 1 : 0);
  }
  private static void setIgnoreData(Set<String> tset) {
   if ("true".equalsIgnoreCase(System.getenv("USE_ZHTW_WORKAROUND"))) {
      tset.add("America/Adak");
      tset.add("America/Atka");
      tset.add("Pacific/Honolulu");
      tset.add("Pacific/Johnston");
      tset.add("SystemV/HST10");
      tset.add("US/Aleutian");
      tset.add("US/Hawaii");
      tset.add("Antarctica/Troll");
      tset.add("Asia/Taipei");
    }
  }
  public static Set<String> getDataInstance() {
    TreeSet<String> tset = new TreeSet<String>();
    if (result) setIgnoreData(tset);
    return tset;
  }
}
