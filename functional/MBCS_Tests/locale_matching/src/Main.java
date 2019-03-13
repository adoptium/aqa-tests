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

import java.io.*;
import java.util.*;

public class Main {

   public static void main(String[] args) throws Exception {
      Locale.setDefault(new Locale(args[0],args[1]));
      String[] dummyarr = new String[1];
      System.setOut(new java.io.PrintStream(new File("LocaleFilterTest1.out"),args[2]));
      LocaleFilterTest1.main(dummyarr); 
      System.setOut(new java.io.PrintStream(new File("LocaleFilterTest2.out"),args[2]));
      LocaleFilterTest2.main(dummyarr); 
      System.setOut(new java.io.PrintStream(new File("LocaleFilterTest3.out"),args[2]));
      LocaleFilterTest3.main(dummyarr); 
      System.setOut(new java.io.PrintStream(new File("LocaleLookupTest1.out"),args[2]));
      LocaleLookupTest1.main(dummyarr); 
      System.setOut(new java.io.PrintStream(new File("LocaleLookupTest2.out"),args[2]));
      LocaleLookupTest2.main(dummyarr); 
      System.out.close();
   }
}
