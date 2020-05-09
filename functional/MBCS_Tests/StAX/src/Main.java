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
      Locale.setDefault(new Locale(args[3],args[4]));
      String propfile = args[2]+"_"+args[3]+"_"+args[4]+"."+args[5]+".properties";
      if(args[4].equalsIgnoreCase("tw")){
         propfile = args[2]+"_"+args[3]+"_"+args[4]+"."+args[5]+"_2.properties";
      }
      String[] arr1 = { args[0] };
      String[] arr2 = { args[1] };
      String[] teststring = null;
      try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
         Properties prop = new Properties();
         prop.load(is);
         teststring = prop.getProperty(args[6]).split(" ");
      } catch (Exception e) {
         e.printStackTrace();
      }
      
      System.setOut(new java.io.PrintStream(new File("read_cursor.html"),args[5]));
      StAXReadCursor.main(arr1);
      
      System.setOut(new java.io.PrintStream(new File("read_event.html"),args[5]));
      StAXReadEveIter.main(arr2);
      
      PrintStream ps = new PrintStream(new File("log"),args[5]);
      System.setOut(ps);
      System.setErr(ps);
      StAXWriteCursor.main(teststring);
      StAXWriteEveIter.main(teststring);
     
      System.out.close();
   }
}

