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

import java.lang.reflect.Method;

public class JavaVersion {
  final static long version;
  final static int feature;
  final static int interim;
  final static int update;

  static {
    int tempFeature = 0;
    int tempInterim = 0;
    int tempUpdate  = 0;
    try {
      Class<?> runtimeClass = Class.forName("java.lang.Runtime");
      Method versionMid = runtimeClass.getDeclaredMethod("version", (Class<?>[])null);
      Object ver = versionMid.invoke(null, (Object[])null);
      Method featureMid = ver.getClass().getDeclaredMethod("feature", (Class<?>[])null);
      Method interimMid = ver.getClass().getDeclaredMethod("interim", (Class<?>[])null);
      Method updateMid  = ver.getClass().getDeclaredMethod("update",  (Class<?>[])null);
      tempFeature = (int)featureMid.invoke(ver, (Object[])null);
      tempInterim = (int)interimMid.invoke(ver, (Object[])null);
      tempUpdate  = (int)updateMid.invoke(ver, (Object[])null);
    } catch (Exception e) {
    }
    feature = tempFeature;
    interim = tempInterim;
    update  = tempUpdate;
    version = feature * 1000000L +
              interim * 1000L +
              update;
  }

  public static long getVersion() {
    return version;
  }

  public static long getFeature() {
    return feature;
  }

  public static long getInterim() {
    return interim;
  }

  public static long getUpdate() {
    return update;
  }
}
