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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;

public class StAXWriteCursor {
	public static void main(String[] args) {
		final String version = "1.0";
		final String encoding = "UTF-8";

		XMLOutputFactory xofactory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer;

		String testString = "\ud84d\ude3a\ud85b\udff6\ud867\ude3d\ud868\udc2f\ud869\udeb2";
                // test string containing extension B character

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		org.w3c.dom.Document document;

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			ArrayList<String> namec = new ArrayList<String>();

			if (args.length < 4) {
				System.err.println("Less than four argument string specified. Use pre-defined string.");
				namec.add(testString);
			} else {
				for (int i = 0; i < args.length; i++) {
					try {
						Element test = (Element) document
								.createElement(args[i]);
						namec.add(args[i]);
					} catch (Exception e) {
						System.err.println(args[i] + " contains XML invalid character. skipping...");
					}
				}
			}

			if (namec.size() == 0) {
				System.err.println("Cannot obtain sufficient(at least four) valid strings. exiting...");
				System.exit(1);
			} else if (namec.size() < 4) {
				for (int i = namec.size(); i < 4; i++) {
					namec.add(namec.get(0) + "_" + Integer.toString(i));
				}
			}

			String names[] = new String[namec.size()];
			namec.toArray(names);

			System.err.print("Element names to use: ");
			for (int i = 0; i < names.length; i++) {
				System.err.print(names[i] + " ");
			}

			System.err.println("");

			writer = xofactory.createXMLStreamWriter(new FileOutputStream(
					"write_cursor.xml"), encoding);

			writer.writeStartDocument(encoding, version);
			writer.writeStartElement(names[0] + "root");

			for (int i = 0; i < 5; i++) {
				writer.writeStartElement(names[1] + "elm");
				writer.writeAttribute(names[2] + "attr", names[2] + "val" + i);

				for (int j = 3; j < names.length; j++) {
					writer.writeStartElement(names[j] + "elm");
					writer.writeCharacters(names[j] + "val");
					writer.writeEndElement();
				}
				writer.writeEndElement();
			}

			writer.writeComment(args[0] + " comment");
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.close();

			System.out.println("");
			System.out.println("The write_cursor.xml was created.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
}
