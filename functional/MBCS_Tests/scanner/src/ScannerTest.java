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
import java.io.*;

public class ScannerTest {

   public static void main(String[] args){
      String testString = "\ud84d\ude3a\ud85b\udff6\ud867\ude3d\ud868\udc2f\ud869\udeb2"; // test string containing extension B character
      char delim = ' ';
      if(args.length < 1){
         delim = testString.charAt(0);
         for(int j=0 ; j<2 ; j++){
            testString += " " + delim + " " + testString;
         }
      }else{
         delim = args[0].charAt(0);
         testString = new String("");
         for(int j=0 ; j<args.length ; j++){
            if(j > 0)
               testString += " " + delim + " ";
            testString += args[j];
         }
      }

      PrintWriter ps = null;
      try{
         ps = new PrintWriter("log.txt","UTF-8");
      }catch(Exception e){
         e.printStackTrace(System.err);
         System.exit(1);
      }

      System.out.println();
      System.out.println("Tokenizing Test:");
      ps.println();
      ps.println("Tokenizing Test:");
      {
         System.out.printf("%ntest string: %s%ndelimiter: %s%n", testString, "(space)");
         ps.printf("%ntest string: %s%ndelimiter: %s%n", testString, "(space)");
         Scanner sc = new Scanner(testString);
         sc.useDelimiter("\\s* \\s*");
         for(int j=1 ; sc.hasNext() ; j++){
            String ss = sc.next();
            System.out.printf("item %d: %s%n", j, ss);
            ps.printf("item %d: %s%n", j, ss);
         }
      }
      {
         System.out.printf("%ntest string: %s%ndelimiter: %s%n", testString, delim);
         ps.printf("%ntest string: %s%ndelimiter: %s%n", testString, delim);
         Scanner sc = new Scanner(testString);
         sc.useDelimiter("\\s*" + delim + "\\s*");
         for(int j=1 ; sc.hasNext() ; j++){
            String ss = sc.next();
            System.out.printf("item %d: %s%n", j, ss);
            ps.printf("item %d: %s%n", j, ss);
         }
      }
      System.out.println();
      System.out.println("Pattern Matching Test:");
      ps.println();
      ps.println("Pattern Matching Test:");
      {
         String pat = new String("" + delim + "[^ ]*");
         System.out.printf("%ntest string: %s%npattern: %s%n", testString, pat);
         ps.printf("%ntest string: %s%npattern: %s%n", testString, pat);
         Scanner sc = new Scanner(testString);
         for(int j=1 ; ; j++){
            String res = sc.findInLine(pat);
            if(res != null && !res.equals("")){
               System.out.printf("item %d: %s%n", j, res);
               ps.printf("item %d: %s%n", j, res);
            }else{
               break;
            }
         }
      }
      {
         String pat = new String("" + delim + " [^ ]*");
         System.out.printf("%ntest string: %s%npattern: %s%n", testString, pat);
         ps.printf("%ntest string: %s%npattern: %s%n", testString, pat);
         Scanner sc = new Scanner(testString);
         for(int j=1 ; ; j++){
            String res = sc.findInLine(pat);
            if(res != null && !res.equals("")){
               System.out.printf("item %d: %s%n", j, res);
               ps.printf("item %d: %s%n", j, res);
            }else{
               break;
            }
         }
      }
      ps.flush();
   }
}
