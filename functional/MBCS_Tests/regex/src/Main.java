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
      String propfile = "setup_" + args[0] + "_" + args[1] + ".properties";
      if ( args[2].equalsIgnoreCase("CN") || args[2].equalsIgnoreCase("TW") ){
         propfile = "setup_" + args[0] + "_" + args[1] + "-" + args[2] + ".properties";
      }
      
      try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
         Properties prop = new Properties();
         prop.load(is);
         
         System.out.println("------------ Pattern matching test ------------");
         String[] argsarr = {"", args[4], args[3]};
         int simplegrep_num = Integer.parseInt(prop.getProperty("simplegrep_num"));
         for (int i = 0; i < simplegrep_num; i++){
            argsarr[0] = prop.getProperty("simplegrep_arg" + String.valueOf(i));
            SimpleGrep.main(argsarr);
            System.out.println(prop.getProperty("simplegrep_arg" + String.valueOf(i) + "_1"));
            System.out.println(prop.getProperty("simplegrep_arg" + String.valueOf(i) + "_2"));
         }

         System.out.println();
         System.out.println("------------ Pattern replacement test ------------");
         String[] argsarr2 = {"", "", args[4], args[3], "-v"};
         int regexreplace_num = Integer.parseInt(prop.getProperty("regexreplace_num"));
         for (int i = 0; i < regexreplace_num; i++){
            argsarr2[0] = prop.getProperty("regexreplace_arg" + String.valueOf(i));
            argsarr2[1] = prop.getProperty("regexreplace_arg" + String.valueOf(i) + "_1");
            RegexReplaceTest.main(argsarr2);
            System.out.println(prop.getProperty("regexreplace_arg" + String.valueOf(i) + "_2"));
            System.out.println(prop.getProperty("regexreplace_arg" + String.valueOf(i) + "_3"));
         }

      } catch (Exception e) {
         e.printStackTrace();
      }
      System.out.close();
   }
}
