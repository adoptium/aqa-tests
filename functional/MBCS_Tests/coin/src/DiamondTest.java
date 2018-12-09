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

public class DiamondTest{
	static String[] getTestStrings(){
		String envString = System.getenv("TEST_STRINGS");
		if (envString == null){
		    System.err.println("Error: Env TEST_STRINGS is empty.");
		    System.exit(-1);
		}

		return CompilerAPI.convIdentifier(envString.split(" "));
	}

	public static void main(String[] args) {
	// For easy to read, using '+' instead of append...
		String className = "DiamondTestCode";
		StringBuilder builder=new StringBuilder();
		String[] testStrings = getTestStrings();

		builder.append(
		"public class DiamondTestCode {\n"
		);
	    for (String target: testStrings) {
		builder.append(
		"  class "+target+" {}\n"
		);
	    }
		builder.append(
		"  class Foo<"
		);
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append("T"+i);
		}else{
		    builder.append(",T"+i);
		}
	    }
		builder.append("> {\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
		builder.append("      T"+i+" t"+i+";\n");
	    }
		builder.append(
		"      public Foo("
		);
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append("T"+i+" t"+i);
		}else{
		    builder.append(",T"+i+" t"+i);
		}
	    }
		builder.append("){\n");
	    for (int i=0; i< testStrings.length; i++) {
		builder.append("        this.t"+i+"=t"+i+";\n");
	    }
		builder.append(
		"      }\n"+
		"      public String toString(){\n"+
		"        return ");
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append("t"+i+".getClass().getName()");
		}else{
		    builder.append("+\",\"+t"+i+".getClass().getName()");
		}
	    }
		builder.append(";\n"+
		"    }\n"+
		"  }\n"+
		"  public DiamondTestCode() {\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
	        builder.append("    "+testStrings[i]+" t"+i+" = new "+testStrings[i]+"();\n");
	    }
		builder.append(
		"    Foo<"
		);
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append(testStrings[i]);
		}else{
		    builder.append(","+testStrings[i]);
		}
	    }
		builder.append("> foo = new Foo<>(");
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append("t"+i);
		}else{
		    builder.append(",t"+i);
		}
	    }
		builder.append(");\n");
		builder.append(
		"    if (foo.toString().equals(\""
		);
	    for (int i=0; i< testStrings.length; i++) {
		if (i==0){
		    builder.append(className+"$"+testStrings[i]);
		}else{
		    builder.append(","+className+"$"+testStrings[i]);
		}
	    }
		builder.append("\")){\n"+
		"      System.out.println(\"Diamond test passed\");\n"+
		"    }else{\n"+
		"      System.out.println(\"Error: Diamond Test failed:\"+foo);\n"+
		"    }\n"+
		"  }\n"+
		"  public static void main(String[] args) {\n"+
		"    DiamondTestCode dia = new DiamondTestCode();\n"+
		"  }\n"+
		"}\n"
		);

		System.out.println(builder.toString());
		CompilerAPI compiler = new CompilerAPI(className, builder.toString());
	}
}
