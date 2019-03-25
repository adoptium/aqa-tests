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

public class TryWithResourcesTest{
        static String getenv(String key){
           String val = System.getenv(key);
           if (null == val)
           val = System.getProperty(key);
           return val;
        }

	static String[] getTestStrings(){
		String envString = getenv("TEST_STRINGS");
		if (envString == null){
		    System.err.println("Error: Env TEST_STRINGS is empty.");
		    System.exit(-1);
		}

		return CompilerAPI.convIdentifier(envString.split(" "));
	}

	public static void main(String[] args) {
	// For easy to read, using '+' instead of append...
		String className = "TryWithResourcesTestCode";
		StringBuilder builder=new StringBuilder();
                String[] testStrings = getTestStrings();

		builder.append(
		"public class "+className+" {\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
		builder.append(
		"  static boolean s"+i+"=false;\n"
		);
	    }
	    for (int i=0; i< testStrings.length; i++) {
		builder.append(
		"  class "+testStrings[i]+" implements AutoCloseable{\n"+
		"    "+testStrings[i]+"() {\n"+
		"       //System.out.println(\""+testStrings[i]+"\");\n"+
		"    }\n"+
		"    public String toString(){\n"+
		"        return \""+testStrings[i]+"\";\n"+
		"    }\n"+
		"    public void close() throws Exception{\n"+
		"       s"+i+" = true;\n"+
		"	//System.out.println(\"closing "+testStrings[i]+"\");\n"+
		"    }\n"+
		"  }\n"
		);
	    }
		builder.append(
		"  public "+className+"() {\n"+
		"    boolean flag = false;\n"+
		"    try(\n"
		);
	    for (int i=0; i< testStrings.length-1; i++) {
	        builder.append(
		"     "+testStrings[i]+" s"+i+" = new "+testStrings[i]+"();\n"
		);
	    }
		// The lastone should not have ';'.
		int index = testStrings.length-1;
	        builder.append(
		"     "+testStrings[index]+" s"+index+" = new "+testStrings[index]+"()\n"+
		"    ){\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
	        builder.append(
		"      if (!s"+i+".toString().equals(\""+testStrings[i]+"\")){\n"+
		"        System.out.println(\"Error: mismatch \"+s"+i+"+\" - "+
		                             className+"$"+testStrings[i]+"\");\n"+
		"        flag = true;\n"+
		"      }\n"
		);
	    }
	        builder.append(
		"    }catch(Exception e){\n"+
		"        System.out.println(e);\n"+
		"    }\n"
		);

	    for (int i=0; i< testStrings.length; i++) {
	        builder.append(
		"    if (!s"+i+"){\n"+
		"        System.out.println(\"Error: Not closed of "+testStrings[i]+"\");\n"+
		"        flag = true;\n"+
		"    }\n"
		);
	    }

		builder.append(
		"    if (!flag){\n"+
		"      System.out.println(\"Try-with-resources test passed\");\n"+
		"    }else{\n"+
		"      System.out.println(\"Try-with-resources test failed\");\n"+
		"    }\n"+
		"  }\n"+
		"  public static void main(String[] args) {\n"+
		"    "+className+" test = new "+className+"();\n"+
		"  }\n"+
		"}\n"
		);

		System.out.println(builder.toString());
		CompilerAPI compiler = new CompilerAPI(className, builder.toString());
	}
}
