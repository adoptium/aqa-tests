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
      String propfile = "setup_"+args[1]+".properties";
      if ( args[2].equals("CN") || args[2].equals("TW") ){
         propfile = "setup_" + args[1] + "-" + args[2] + ".properties";
      }

      String[] teststring = null;
      try (InputStream is = Main.class.getClassLoader().getResourceAsStream(propfile);){
         Properties prop = new Properties();
         prop.load(is);
         teststring = prop.getProperty("TEST_HOSTNAME").split(" ");
      } catch (Exception e) {
         e.printStackTrace();
      }
      
      System.setOut(new java.io.PrintStream(new FileOutputStream("output", true), false, args[3]));
      System.out.println("URL=http://" + teststring[0] + " ");
      System.out.println("converting URL from UNICODE to ACE... ");
      
      System.setOut(new java.io.PrintStream(new File("toASCII.tmp"),args[3]));
      IDNtoASCII.main(teststring);
      
      System.setOut(new java.io.PrintStream(new FileOutputStream("output", true), false, args[3]));
      try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("toASCII.tmp"), args[3]));){
            String str;
            List<String> lines = new ArrayList<>();
            while ((str = br.readLine()) != null) {
                System.out.println("ACE=http://" + str + " ");
                System.out.println("URL=http://" + str + " ");
                teststring[0] = str;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


      System.out.println("converting URL from ACE to UNICODE... ");
      System.setOut(new java.io.PrintStream(new File("toUNICODE.tmp"),args[3]));
      IDNtoUNICODE.main(teststring);

      System.setOut(new java.io.PrintStream(new FileOutputStream("output", true), false, args[3]));
      try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("toUNICODE.tmp"), args[3]));){

            String str;
            List<String> lines = new ArrayList<>();
            while ((str = br.readLine()) != null) {
                System.out.println("UNICODE=http://" + str + " ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
      
      System.out.close();
   }
}
