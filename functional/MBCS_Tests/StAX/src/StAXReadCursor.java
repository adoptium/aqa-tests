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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class StAXReadCursor {
	public static void main(String[] args) {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		BufferedInputStream stream;

		try {
			stream = new BufferedInputStream(new FileInputStream(args[0]));
			XMLStreamReader reader = factory.createXMLStreamReader(stream);
			for (; reader.hasNext(); reader.next()) {
				int eventType = reader.getEventType();
				if (eventType == XMLStreamConstants.START_DOCUMENT) {
					System.out.println("<TABLE border=1>");
					System.out.println("<TBODY>");
					System.out.println("<TR>");
					System.out.println("<TD>Version</TD><TD>"
							+ reader.getVersion() + "</TD></TR>");
					System.out.println("<TR>");
					System.out.println("<TD>Encoding</TD><TD>"
							+ reader.getEncoding() + "</TD></TR>");
					System.out.println("<TR>");
					System.out.println("<TD>Standalone</TD><TD>"
							+ reader.isStandalone() + "</TD></TR>");
					System.out.println("</TBODY></TABLE><BR>");
				}

				if (eventType == XMLStreamConstants.START_ELEMENT) {
					
					System.out.println("<TABLE border=1>");
					System.out.println("<TBODY>");
					System.out.println("<TR>");

					System.out.println("<TD>Element(QName)</TD><TD>"
							+ reader.getName() + "</TD></TR>");
					System.out.println("<TR>");
					System.out.println("<TD>Element(Local Name)</TD><TD>"
							+ reader.getLocalName() + "</TD></TR>");

					String ns = reader.getNamespaceURI();
					int count = reader.getNamespaceCount();

					if (ns != null || count != 0) {
						System.out.println("<TR>");
						System.out.println("<TD>Namespace</TD></TR>");
						System.out.println("<TR>");
						System.out.println("<TD>Namespace(URI)</TD><TD>" + ns
								+ "</TD></TR>");
					}

					for (int i = 0; i < count; i++) {
						System.out.println("<TR>");
						System.out.println("<TD>Namespace(Prefix)</TD><TD>"
								+ reader.getNamespacePrefix(i) + "</TD></TR>");
						System.out.println("<TR>");
						System.out.println("<TD>Namespace(URI)</TD><TD>"
								+ reader.getNamespaceURI(i) + "</TD></TR>");
					}

					count = reader.getAttributeCount();

					if (count != 0) {
						// System.out.println("<TR>");
						// System.out.println("<TD>Attribute</TD><TD>&nbsp;</TD></TR>");

						for (int i = 0; i < count; i++) {
							System.out.println("<TR>");
							System.out
									.println("<TD>Attribute(QName)</TD><TD>"
											+ reader.getAttributeName(i)
											+ "</TD></TR>");
							System.out.println("<TR>");
							System.out
									.println("<TD>Attribute(Local Name)</TD><TD>"
											+ reader.getAttributeLocalName(i)
											+ "</TD></TR>");
							System.out.println("<TR>");
							System.out
									.println("<TD>Attribute(Type)</TD><TD>"
											+ reader.getAttributeType(i)
											+ "</TD></TR>");
							System.out.println("<TR>");
							System.out.println("<TD>Attribute(Value)</TD><TD>"
									+ reader.getAttributeValue(i)
									+ "</TD></TR>");
							System.out.println("<TR>");
							String prefix = reader.getAttributePrefix(i);
							if (prefix == "") {
								prefix = "-";
							}
							System.out.println("<TD>Attribute(Prefix)</TD><TD>"
									+ prefix + "</TD></TR>");
							System.out.println("<TR>");
							System.out.println("<TD>Attribute(Namespace)</TD><TD>"
											+ reader.getAttributeNamespace(i)
											+ "</TD></TR>");
						}
					}
				}
				if (eventType == XMLStreamConstants.CHARACTERS) {
					System.out.println("<TR>");
					System.out.println("<TD>Content</TD><TD>"
							+ reader.getText() + "</TD></TR>");
				}
				if (eventType == XMLStreamConstants.END_ELEMENT) {
					System.out.println("</TBODY></TABLE><BR>");
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}
}
