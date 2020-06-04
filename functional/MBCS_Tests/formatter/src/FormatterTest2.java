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
import java.io.*;
import java.util.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static java.util.Calendar.*;

public class FormatterTest2 {

   public static void main(String[] args){
      String testString = "\ud84d\ude3a\ud85b\udff6\ud867\ude3d\ud868\udc2f\ud869\udeb2"; // test string containing extension B character
      if(args.length > 0)
         testString = args[0];

      FileOutputStream fos = null;
      try{
         fos = new FileOutputStream("log.txt");
      }catch(java.io.FileNotFoundException e){
         e.printStackTrace(System.err);
         System.exit(1);
      }
      PrintWriter ps = new PrintWriter(fos);

      Formatter formatter = null;
      try{
         formatter = new Formatter(fos, "UTF-8");
      }catch(java.io.UnsupportedEncodingException e){
         formatter = new Formatter(fos);
      }

      if ("".equals(System.getProperty("user.timezone"))) {
         Locale current_locale = Locale.getDefault();
         if (Locale.getDefault().equals(Locale.JAPAN)){
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
         } else if (Locale.getDefault().equals(Locale.KOREA)){
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
         } else if (Locale.getDefault().equals(Locale.CHINA)){
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
         } else if (Locale.getDefault().equals(Locale.TAIWAN)){
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
         }
      }

      Calendar c1 = new GregorianCalendar(2007,0,1);
      Calendar c2 = new GregorianCalendar(2007,7,1);
      Calendar c3 = new GregorianCalendar(2007,1,29);
      Calendar c4 = new GregorianCalendar(2008,1,29);
      Calendar c5 = new GregorianCalendar(2007,3,31);
      Calendar c6 = new GregorianCalendar(2007,11,32);

      System.out.println();
      System.out.println("numeric format:");
      System.out.format("123456.789 = %(,10.3f%n", 123456.789);
      System.out.format("PI         = %+10.8f%n", Math.PI);
      System.out.format("65535(hex) =  %#10x%n" , 65535);
      System.out.format("65535(oct) =  %#10o%n" , 65535);
      // string
      System.out.println();
      System.out.println("string format:");
      System.out.format("test string: %s%n" , testString);
      System.out.format("upper case : %S%n" , testString);
      // date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2007/01/01");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c1);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c1);
      System.out.format("month(full) : %1$tB%n", c1);
      System.out.format("month(short): %1$tb%n", c1);
      System.out.format("day of week : %1$tA%n", c1);
      // date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2007/08/01");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c2);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c2);
      System.out.format("month(full) : %1$tB%n", c2);
      System.out.format("month(short): %1$tb%n", c2);
      System.out.format("day of week : %1$tA%n", c2);
      // date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2007/02/29 which does not exist");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c3);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c3);
      System.out.format("month(full) : %1$tB%n", c3);
      System.out.format("month(short): %1$tb%n", c3);
      System.out.format("day of week : %1$tA%n", c3);
// date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2008/02/29");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c4);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c4);
      System.out.format("month(full) : %1$tB%n", c4);
      System.out.format("month(short): %1$tb%n", c4);
      System.out.format("day of week : %1$tA%n", c4);
// date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2007/04/31 which does not exist");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c5);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c5);
      System.out.format("month(full) : %1$tB%n", c5);
      System.out.format("month(short): %1$tb%n", c5);
      System.out.format("day of week : %1$tA%n", c5);
// date/time
      System.out.println();
      System.out.println("date/time format:");
      System.out.println("test for 2007/12/32 which does not exist");
      System.out.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c6);
      System.out.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c6);
      System.out.format("month(full) : %1$tB%n", c6);
      System.out.format("month(short): %1$tb%n", c6);
      System.out.format("day of week : %1$tA%n", c6);

// To log file
      // numeric
      ps.println();
      ps.println("numeric format:");
      formatter.format("123456.789 = %(,10.3f%n", 123456.789);
      formatter.format("PI         = %+10.8f%n", Math.PI);
      formatter.format("65535(hex) =  %#10x%n" , 65535);
      formatter.format("65535(oct) =  %#10o%n" , 65535);
      ps.flush();
      formatter.flush();
      // string
      ps.println();
      ps.println("string format:");
      formatter.format("test string: %s%n" , testString);
      formatter.format("upper case : %S%n" , testString);
      ps.flush();
      formatter.flush();
      // date/time
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2007/01/01");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c1);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c1);
      formatter.format("month(full) : %1$tB%n", c1);
      formatter.format("month(short): %1$tb%n", c1);
      formatter.format("day of week : %1$tA%n", c1);
      ps.flush();
      formatter.flush();
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2007/08/01");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c2);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c2);
      formatter.format("month(full) : %1$tB%n", c2);
      formatter.format("month(short): %1$tb%n", c2);
      formatter.format("day of week : %1$tA%n", c2);
      ps.flush();
      formatter.flush();
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2007/02/29 which does not exist");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c3);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c3);
      formatter.format("month(full) : %1$tB%n", c3);
      formatter.format("month(short): %1$tb%n", c3);
      formatter.format("day of week : %1$tA%n", c3);
      ps.flush();
      formatter.flush();
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2008/02/29");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c4);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c4);
      formatter.format("month(full) : %1$tB%n", c4);
      formatter.format("month(short): %1$tb%n", c4);
      formatter.format("day of week : %1$tA%n", c4);
      ps.flush();
      formatter.flush();
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2007/04/31 which does not exist");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c5);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c5);
      formatter.format("month(full) : %1$tB%n", c5);
      formatter.format("month(short): %1$tb%n", c5);
      formatter.format("day of week : %1$tA%n", c5);
      ps.flush();
      formatter.flush();
      ps.println();
      ps.println("date/time format:");
      ps.println("test for 2007/12/32 which does not exist");
      formatter.format("current date: %1$tY/%1$tm/%1$td (%1$ta) %1$tZ%n", c6);
      formatter.format("current time: %1$tp %1$tI:%1$tM:%1$tS GMT%1$tz%n", c6);
      formatter.format("month(full) : %1$tB%n", c6);
      formatter.format("month(short): %1$tb%n", c6);
      formatter.format("day of week : %1$tA%n", c6);
      ps.flush();
      formatter.flush();

   }
}

