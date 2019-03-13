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

public class Main2 {

   public static void main(String[] args) throws Exception {
      Locale.setDefault(new Locale(args[1],args[2]));
      System.setOut(new java.io.PrintStream(new File("output"),args[3]));
      String[] teststring = null;
      {
          String propfile = args[0]+"_"+args[1]+"_"+args[2]+"."+args[3]+".properties";
          try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
              Properties prop = new Properties();
              prop.load(is);
              teststring = prop.getProperty(args[4]).split(" ");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
      {
          String propfile = "jdbc41_"+args[0]+"_"+args[1]+"_"+args[2]+"."+args[3]+".properties";
          try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
              Properties prop = new Properties();
              prop.load(is);
              for(String k : prop.keySet().toArray(new String[0])) {
                  System.setProperty(k, (String)prop.get(k));
              }
              System.setProperty("DERBY_HOME", System.getProperty("DERBY_HOME")
                                 .replace("%CD%", System.getProperty("user.home")));
          } catch (Exception e) {
              e.printStackTrace();
          }
      }
      System.out.println("LOCALE = " + args[1] + " ");
      System.out.println("JDBC41_TABLE_NAME = " + System.getProperty("JDBC41_TABLE_NAME") + " ");
      System.out.println("JDBC41_CNAME = " +  System.getProperty("JDBC41_CNAME") + " ");
      System.out.println("--- Create Table and Insert test data in JavaDB. ");
      jdbc41autoclose.main(teststring);
      jdbc41RowSetProvider.main(new String[0]);
      System.out.println("--- Drop table in JavaDB. ");
      jdbc41droptb.main(new String[0]);
      System.out.close();
   }
}
