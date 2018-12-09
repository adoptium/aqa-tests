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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.IDN;

public class IDNFromFile {

	public static void main(String[] args) {

		FileInputStream fis = null;
		FileOutputStream fos = null ;
		FileOutputStream fos2 = null ;
		InputStreamReader isr = null;
		OutputStreamWriter osw = null;
		OutputStreamWriter osw2 = null;
		BufferedReader br = null;
		BufferedWriter bw = null ;
		BufferedWriter bw2 = null ;
		PrintWriter pw = null ;
		PrintWriter pw2 = null ;
		String enc = null;
		enc = System.getProperty("file.encoding");
		
		if(args.length !=1){
			System.out.println("Usage :  java IDNfromFile <list file>");
			System.exit(0);
		}

		try {

			fis = new FileInputStream(args[0]);
			fos = new FileOutputStream("toUnicode.txt");
			fos2 = new FileOutputStream("toAscii.txt");
			isr = new InputStreamReader(fis,enc);

			osw = new OutputStreamWriter(fos,enc);
			osw2 = new OutputStreamWriter(fos2);
			br = new BufferedReader(isr);
			bw = new BufferedWriter(osw);
			pw = new PrintWriter(bw);
			bw2 = new BufferedWriter(osw2);
			pw2 = new PrintWriter(bw2);
			
			String line = null;
			String toascii = null;
			String tounicode = null;

			while ((line = br.readLine()) != null) {
				toascii = IDN.toASCII(line);
				tounicode = IDN.toUnicode(toascii);
				pw.println(tounicode);
				pw.flush();
				pw2.println(toascii);
				pw2.flush();
			}

		} catch (FileNotFoundException fr) {
			fr.printStackTrace();
		} catch (IOException i) {
			i.printStackTrace();
		} finally {

			try {

				pw.close();
				bw.close();
				br.close();
				osw.close();
				fos.close();
				isr.close();
				fis.close();

			} catch (IOException o) {
				o.printStackTrace();
			}
		}

	}

}
