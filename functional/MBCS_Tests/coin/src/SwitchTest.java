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

public class SwitchTest{
        static String getenv(String key){
           String val = System.getenv(key);
           if (null == val)
           val = System.getProperty(key);
           return val;
        }

	static String[] getTestStrings(){
		String envString = getenv("TEST_STRINGS_SED");
		if (envString == null){
		    System.err.println("Error: Env TEST_STRINGS_SED is empty.");
		    System.exit(-1);
		}
		String[] testStrings = envString.split(" ");

		return testStrings;
	}

	public static void main(String[] args) {
	// For easy to read, using '+' instead of append...
		String className = "SwitchTestCode";
		StringBuilder builder=new StringBuilder();
                String[] testStrings = getTestStrings();

		builder.append(
		"public class SwitchTestCode {\n"+
		"  public static String exec(String str) {\n"+
		"    String result;\n"+
		"    switch(str) {\n"
		);

	    for (String target: testStrings) {
		builder.append(
		"        case \""+target+"\":\n"+
		"            result=\""+target+"\"; break;\n"
		);
	    }

		builder.append(
		"        default: result=\"default\"; break;\n"+
		"    }\n"+
		"    return result;\n"+
		"  }\n"+
		"  public static void main(String[] args) {\n"+
		"      boolean flag = false;\n"
		);

	    for (String target: testStrings) {
		builder.append(
		"      if(!"+className+".exec(\""+target+"\").equals(\""+target+"\")) {\n" +
		"          System.out.println(\"Error: unmatch "+target+"\");\n" +
		"          flag = true;\n" +
		"      }\n"
		);
	    }

		builder.append(
		"      if(!"+className+".exec(\"otherabc\").equals(\"default\")) {\n" +
		"          System.out.println(\"Error: unmatch default\");\n" +
		"          flag = true;\n" +
		"      }\n" +
		"      try{\n" +
		"          String result = "+className+".exec(null);\n" +
		"          System.out.println(\"Error unexpected Null handler. result=\"+result);\n" +
		"          flag=true;\n" +
		"      }catch(NullPointerException e){\n" +
		"      }\n" +
		"      if (flag == false) {\n" +
		"          System.out.println(\"Switch test passed\");\n" +
		"      }else{\n"+
		"          System.out.println(\"Switch test failed\");\n" +
		"      }\n" +
		"    }\n" +
		"}\n"
		);

		System.out.println(builder.toString());
		CompilerAPI compiler = new CompilerAPI(className, builder.toString());
	}
}
