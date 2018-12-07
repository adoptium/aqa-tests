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
// BreakIteratorTest.java

import java.util.*;
import java.io.*;
import java.text.*;

class BreakIteratorTest {
	public BreakIteratorTest(String fname) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fname));
			while (reader.ready()) {
				String stringToExamine = new String("");
				String line = null;
				while((line = reader.readLine())!=null){
					stringToExamine+=line;
				}
				byte buf[]=new byte[10];
				// character boundary test
				BreakIterator boundary = BreakIterator.getCharacterInstance();  //default locale
				boundary.setText(stringToExamine);
				System.out.println("Character Boundary Test :");
				printEachForward(boundary, stringToExamine);
				System.out.println("End of this test.");
				/*System.out.println("Hit Enter");
				System.in.read(buf);*/
				// word boundary test
				boundary = BreakIterator.getWordInstance();  //default locale
				boundary.setText(stringToExamine);
				System.out.println("Word Boundary Test :");
				printEachForward(boundary, stringToExamine);
				System.out.println("End of this test.");
				/*System.out.println("Hit Enter");
				System.in.read(buf);*/
				// sentence boundary test
				boundary = BreakIterator.getSentenceInstance();  //default locale
				boundary.setText(stringToExamine);
				System.out.println("Sentence Boundary Test :");
				printEachForward(boundary, stringToExamine);
				System.out.println("End of this test.");
				/*System.out.println("Hit Enter");
				System.in.read(buf);*/
				// line boundary test
				boundary = BreakIterator.getLineInstance();  //default locale
				boundary.setText(stringToExamine);
				System.out.println("Line Boundary Test :");
				printEachForward(boundary, stringToExamine);
				System.out.println("End of this test.");
				/*System.out.println("Hit Enter");
				System.in.read(buf);*/
				//printEachBackward(boundary, stringToExamine);
				//printFirst(boundary, stringToExamine);
				//printLast(boundary, stringToExamine);
				
				/*
				boundary = BreakIterator.getSentenceInstance(Locale.US);
				boundary.setText(stringToExamine);
				printEachBackward(boundary, stringToExamine);
				*/
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printEachForward(BreakIterator boundary, String source) {
		int p = boundary.first();
		for (int q = boundary.next(); q != BreakIterator.DONE; q = boundary.next()) {
			System.out.println(source.substring(p, q));
			p = q;
		}
		System.out.println();
	}
	
	public static void printEachBackward(BreakIterator boundary, String source) {
		int p = boundary.last();
		for (int q = boundary.previous(); q != BreakIterator.DONE; q = boundary.previous()) {
			System.out.println(source.substring(q, p));
			p = q;
		}
		System.out.println();
	}
	
	public static void printFirst(BreakIterator boundary, String source) {
		int p = boundary.first();
		int q = boundary.next();
		System.out.println(source.substring(p, q));
	}
	
	public static void printLast(BreakIterator boundary, String source) {
		int p = boundary.last();
		int q = boundary.previous();
		System.out.println(source.substring(q, p));
	}
	
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("usage: java BreakIteratorTest fname");
			return;
		}
		
		new BreakIteratorTest(args[0]);
	}
}
