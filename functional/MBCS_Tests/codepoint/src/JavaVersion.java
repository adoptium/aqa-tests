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
import java.util.regex.*;

public class JavaVersion {
  public static long getVersion() {
    String version_s = System.getProperty("java.version");
    long version = 0;
    Pattern pat = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*");
    Matcher mat = pat.matcher(version_s);
    if (mat.matches()) {
        try {
          version = Long.parseLong(mat.group(1)) * 1000000L +
                    Long.parseLong(mat.group(2)) * 1000L +
                    Long.parseLong(mat.group(3));
        } catch (NumberFormatException nfe) {
        }
    }
    if (0 == version) {
      pat = Pattern.compile("^(\\d+)[^0-9]*.*");
      mat = pat.matcher(version_s);
      if (mat.matches()) {
        try {
          version = Long.parseLong(mat.group(1)) * 1000000L;
        } catch (NumberFormatException nfe) {
        }
      }
    }
    return version;
  }
}
