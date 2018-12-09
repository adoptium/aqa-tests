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

import java.lang.*;
import java.util.*;

public class EnvTest {

   public static void main(String[] args){
      System.out.printf("TEST_STRINGS(old method): %s%n", System.getenv("TEST_STRINGS"));
      Map<String,String> envmap = System.getenv();
      System.out.printf("TEST_STRINGS(new method): %s%n", envmap.get("TEST_STRINGS"));
/*
      System.out.println("All Environment Variables:");
      Set<String> keys = envmap.keySet();
      for(Iterator<String> ite = keys.iterator(); ite.hasNext(); ){
         String key = ite.next();
         String val = envmap.get(key);
         System.out.printf("%s:%s%n", key, val);
      }
*/
   }
}
