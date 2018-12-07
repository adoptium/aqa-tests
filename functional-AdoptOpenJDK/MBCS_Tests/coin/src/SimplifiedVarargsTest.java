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

public class SimplifiedVarargsTest{
	static String[] getTestStrings(){
		String envString = System.getenv("TEST_STRINGS_SED");
		if (envString == null){
		    System.err.println("Error: Env TEST_STRINGS is empty.");
		    System.exit(-1);
		}
		String[] testStrings = envString.split(" ");
/*
		for (int i=0; i< testStrings.length; i++) {
			testStrings[i] = CompilerAPI.convIdentifier(testStrings[i]);
		}
*/
		return testStrings;
	}

	public static void main(String[] args) {
	// For easy to read, using '+' instead of append...
		String className = "SimplifiedVarargsTestCode";
		StringBuilder builder=new StringBuilder();
		String[] testStrings = getTestStrings();

		builder.append(
		"import java.util.*;\n"+
		"import java.util.AbstractMap.SimpleEntry;\n"+
		"import java.util.Map.Entry;\n"+
		"public class "+className+" {\n"+
		"  @SafeVarargs\n"+
		"  static <K, V> boolean contains(final Map<K, V> m, final Entry<K, V>... entries) {\n"+
		"    boolean result = false;\n"+
		"    for (Entry<K, V> e : entries) {\n"+
		"      K key = e.getKey();\n"+
		"      if (!m.containsKey(key)){\n"+
		"          System.err.println(\"Error: not found key=\"+key);\n"+
		"      }else{\n"+
		"          V expected = e.getValue();\n"+
		"          V actual = m.get(key);\n"+
		"          if (expected.equals(actual)){\n"+
		"              result = true;\n"+
		"          }else{\n"+
		"              System.err.println(\"Error: Not found key=\"+key+\" value=\"+expected+\" actual=\"+actual);\n"+
		"          }\n"+
		"      }\n"+
		"    }\n"+
		"    return result;\n"+
		"  }\n\n"+
		"  @SuppressWarnings(value = \"unchecked\")\n"+
		"  static <K, V> Entry<K, V> entry(final K key, final V value) {\n"+
		"    return new SimpleEntry<K, V>(key, value);\n"+
		"  }\n\n"+
		"  public static void main(String[] args){\n"+
		"    HashMap<String,Integer> map = new HashMap<>();\n"
		);
	    for (int i=0; i< testStrings.length; i++) {
		builder.append(
		"    map.put(\""+testStrings[i]+"\","+i+");\n"
		);
	    }
		builder.append(
		"    boolean result = contains(map, "
		);
	    for (int i=0; i< testStrings.length; i++) {
	      if (i==0){
		builder.append(
		"entry(\""+testStrings[i]+"\", "+i+")"
		);
	      }else{
		builder.append(
		", entry(\""+testStrings[i]+"\", "+i+")"
		);
	      }
	    }
		builder.append(
		");\n"+
		"    if (result) {\n"+
		"        System.out.println(\"Simplified Varargs test passed\");\n"+
		"    }else{\n"+
		"        System.out.println(\"Simplified Varargs test failed\");\n"+
		"    }\n"+
		"  }\n"+
		"}\n"
		);

		System.out.println(builder.toString());
		CompilerAPI compiler = new CompilerAPI(className, builder.toString());
	}
}
