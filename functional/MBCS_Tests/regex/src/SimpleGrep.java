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

import java.util.regex.*;
import java.io.*;

public class SimpleGrep {
	public SimpleGrep(String regex, String fname, String enc) {
		// First, compile the given regular expression.  It is efficient 
		// since it allows the compiled pattern to be reused. 
		Pattern p = Pattern.compile(regex);

		Matcher m = null;
		String input = null;
		int nline = 0;
		BufferedReader reader = null;
		try {
			//BufferedReader reader = new BufferedReader(new FileReader(fname));
			if (enc == null){
				reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(fname)));	
			}else{
				reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(fname), enc));
			}
			while (reader.ready()) {
				++nline;
				input = reader.readLine();

				m = p.matcher(input);

				// attempt to find the next subsequence of the input sequence 
				// that matches the pattern.
				if (m.find()) {
					//if (m.matches()) {     // attempt to match the entire input 
					// sequence against the pattern. 
					//if (m.lookingAt()) {   // attempt to match the input sequence, 
					// starting at the beggining, against the pattern.
					System.out.println(nline + ": " + input);
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try{
				reader.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		if (!(args.length > 1 && args.length < 5)) {
			printUsage();
			System.exit(1);
		}
		String pattern = "";
		String filename = "";
		String enc = null;

		if (args.length > 2 && args[0].equals("-f")) {
			filename = args[2];
			if (args.length == 4) {
				enc = args[3];
			}
			pattern = readPatternFile(args[1], enc);
		} else {
			pattern = args[0];
			filename = args[1];
			if (args.length == 3) {
				enc = args[2];
			}
		}
		//System.out.println("Pattern: " + pattern);
		new SimpleGrep(pattern, filename, enc);
	}
	private static String readPatternFile(String fname, String enc) {
		String p = "";
		BufferedReader reader = null;
		try {
			if (enc == null){
				reader =
					new BufferedReader(
						new InputStreamReader(new FileInputStream(fname)));	
			}else{
				reader =
					new BufferedReader(
						new InputStreamReader(new FileInputStream(fname), enc));	
			}
			
			p = reader.readLine();
			int len = p.length();
			if (p.indexOf(len - 1) == '\n') {
				p = p.substring(0, len - 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}finally{
			try{
				reader.close();
			}catch(Exception ex){
				ex.printStackTrace();	
			}
		}

		return p;
	}
	private static void printUsage() {
		System.err.println("Usage: java SimpleGrep PATTERN FILE [ENCODING]");
		System.err.println("       java SimpleGrep -f FILE FILE [ENCODING]");
		System.err.println(" -f FILE");
		System.err.println(
			"   Obtains a pattern from FILE in specified encoding.");
		System.err.println("Example: java SimpleGrep \"abc*\" test.txt UTF-8");
	}
}
