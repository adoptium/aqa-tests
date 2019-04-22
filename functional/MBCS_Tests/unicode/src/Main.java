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
      System.out.println("Checking UnicodeData ...");
      PrintStream ps = new PrintStream(new File("err1.txt"));
      System.setErr(ps);
      UnicodeChecker.main(new String[0]);
      System.out.println("Checking Blocks ...");
      ps = new PrintStream(new File("err2.txt"));
      System.setErr(ps);
      UnicodeBlockChecker.main(new String[0]);
      System.out.println("Checking Scripts ...");
      ps = new PrintStream(new File("err3.txt"));
      System.setErr(ps);
      UnicodeScriptChecker.main(new String[0]);
      System.out.println("Checking PropertyValueAliases ...");
      ps = new PrintStream(new File("err4.txt"));
      System.setErr(ps);
      UnicodeScriptChecker3.main(new String[0]);
      System.out.println("Checking NormalizationTest ...");
      ps = new PrintStream(new File("err5.txt"));
      System.setErr(ps);
      NormalizerTest.main(new String[0]);
      System.out.close();
   }
}
