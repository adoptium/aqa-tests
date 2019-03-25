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

public class ExceptionTest{
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
		String className = "ExceptionTestCode";
		StringBuilder builder=new StringBuilder();
                String[] testStrings = getTestStrings();

		builder.append(
		"public class ExceptionTestCode {\n"
		);
	    for (String target: testStrings) {
		builder.append(
		"  static class "+target+" extends Exception {}\n"
		);
	    }
		builder.append(
		"  public ExceptionTestCode(){}\n"+
		"  public static void exec(int index) throws Exception{\n"+
		"    try{\n"+
		"      switch(index) {\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
		builder.append(
		"        case "+i+": throw new "+testStrings[i]+"(); //break;\n"
		);
	    }
		builder.append(
		"        default: throw (Throwable) new Exception(); //break;\n"+
		"      }\n"+
		"    }catch("
		);
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append(testStrings[i]);
		}else{
		    builder.append("|"+testStrings[i]);
		}
	    }
		builder.append(
		              " e){\n"+
		"      throw e;\n"+
		"    }catch(Exception e){\n"+
		"      throw e;\n"+
		"    }catch(Throwable e){\n"+
		"      System.err.println(\"Error: unexpected Throwable was catched.\");\n"+
		"    }\n"+
		"  }\n"+
		"  public static void main(String[] args) {\n"+
		"      ExceptionTestCode test = new ExceptionTestCode();\n"+
		"      boolean flag = false;\n"
		);
	    for (int i=0; i < testStrings.length; i++) {
		builder.append(
		"        try{\n"+
		"          test.exec("+i+");\n"+
		"          System.err.println(\"Error: Exception "+testStrings[i]+" wasn't casted.\");\n"+
		"          flag = true;\n"+
		"        }catch(Exception e){\n"+
		"          //System.out.println(e);\n"+
		"          if (!e.getClass().equals("+className+"."+testStrings[i]+".class)) {\n"+
		"            System.err.println(\"Error: "+testStrings[i]+" class mismatch\");\n"+
		"            flag = true;\n"+
		"          }\n"+
		"        }\n"
		);
	    }
		builder.append(
		"        try{\n"+
		"          test.exec("+testStrings.length+");\n"+
		"          System.err.println(\"Error: Exception() wasn't casted.\");\n"+
		"          flag = true;\n"+
		"        }catch(Exception e){\n"+
		"          // System.out.println(e);\n"+
		"          if (!e.getClass().equals(Exception.class)) {\n"+
		"            System.err.println(\"Error: Exception class mismatch\");\n"+
		"            flag = true;\n"+
		"          }\n"+
		"        }\n"+
		"        if (flag == false) {\n"+
		"            System.out.println(\"Exception test passed\");\n"+
		"        }else{\n"+
		"            System.out.println(\"Exception test failed\");\n"+
		"        }\n"+
		"    }\n" +
		"}\n"
		);

		System.out.println(builder.toString());
		CompilerAPI compiler = new CompilerAPI(className, builder.toString());
	}
}
