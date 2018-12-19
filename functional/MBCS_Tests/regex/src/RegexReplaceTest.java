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

public class RegexReplaceTest {
	private final String NEWLINE = System.getProperty("line.separator");
	public RegexReplaceTest(
		String regex,
		String replacement,
		String fname,
		String enc,
		boolean verbose) {
		// First, compile the given regular expression.  It is efficient 
		// since it allows the compiled pattern to be reused. 
		Pattern p = Pattern.compile(regex);
		Matcher m = null;
		String input = null;
		int nline = 0;
		BufferedReader reader = null;
		try {
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
					// Replace every subsequence of the input sequence that 
					// matches the pattern with the given replacement string.
					String output = m.replaceAll(replacement);
					if (verbose == true) {
						System.out.print(
							nline + ": " + input + NEWLINE + "==> ");
					}
					System.out.println(output);
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

	public RegexReplaceTest(
		String regex,
		String replacement,
		String fname,
		String enc) {
		this(regex, replacement, fname, enc, false);
	}

	public static void main(String[] args) {
		if (args.length < 4 || args.length > 7) {
			printUsage();
			System.exit(1);
		}
		String pattern = "";
		String filename = "";
		String replacement = "";
		String enc = null;

		if (args[0].equals("-f")) {
			replacement = args[2];
			filename = args[3];
			if (args.length >= 3 && !args[4].equals("-v")){
				enc = args[4];
			}
			pattern = readPatternFile(args[1], enc);
		} else {
			pattern = args[0];
			replacement = args[1];
			filename = args[2];
			if (args.length >= 4 && !args[3].equals("-v")){
				enc = args[3];
			}
		}

		if (args[args.length - 1].equals("-v")) {
			new RegexReplaceTest(pattern, replacement, filename, enc, true);
		} else {
			new RegexReplaceTest(pattern, replacement, filename, enc);
		}
	}
	private static void printUsage() {
		System.err.println("Usage: java RegexReplaceTest PATTERN REPLACEMENT FILE [ENCODING] [-v]");
		System.err.println("       java RegexReplaceTest -f FILE REPLACEMENT FILE [ENCODING] [-v]");
		System.err.println(" -f FILE");
		System.err.println(			"   Obtains a pattern from FILE in specified encoding.");
		System.err.println(			"Example: java RegexReplaceTest \"abc*\" \"ABC\" test.txt UTF-8");
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
}
