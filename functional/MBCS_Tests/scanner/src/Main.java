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
      Locale.setDefault(new Locale(args[1],args[2]));
      System.setOut(new java.io.PrintStream(new File("output"),args[3]));
      String propfile = args[0]+"_"+args[1]+"_"+args[2]+"."+args[3]+".properties";
      String[] teststring = null;
      try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
         Properties prop = new Properties();
         prop.load(is);
         teststring = prop.getProperty(args[4]).split(" ");
      } catch (Exception e) {
         e.printStackTrace();
      }
      ScannerTest.main(teststring);
      System.out.close();
   }
}
