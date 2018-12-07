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

import java.util.Iterator;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public interface EventItf {
	public void handleEvent(XMLEvent event);
}

class StartDocumentHandler implements EventItf {

	public void handleEvent(XMLEvent event) {
		StartDocument document = (StartDocument) event;
		System.out.println("<TABLE border=1>");
		System.out.println("<TBODY>");
		System.out.println("<TR>");
		System.out.println("<TD>Version</TD><TD>" + document.getVersion()
				+ "</TD></TR>");
		System.out.println("<TD>Encoding</TD><TD>"
				+ document.getCharacterEncodingScheme() + "</TD></TR>");
		System.out.println("<TD>Standalone</TD><TD>" + document.isStandalone()
				+ "</TD></TR>");
		System.out.println("</TBODY></TABLE><BR>");
	}

}

class StartElementHandler implements EventItf {
	public void handleEvent(XMLEvent event) {
		StartElement element = event.asStartElement();
		System.out.println("<TABLE border=1>");
		System.out.println("<TBODY>");
		System.out.println("<TR>");

		System.out.println("<TD>Element(QName)</TD><TD>" + element.getName()
				+ "</TD></TR>");
		System.out.println("<TR>");
		System.out.println("<TD>Element(Local Name)</TD><TD>"
				+ element.getName().getLocalPart() + "</TD></TR>");

		for (Iterator i = element.getNamespaces(); i.hasNext();) {
			int j = 0;
			if (j == 0) {
				// System.out.println("Namespace");
			}

			Namespace ns = (Namespace) i.next();
			System.out.println("<TR>");
			System.out.println("<TD>Namespace(Prefix)</TD><TD>"
					+ ns.getPrefix() + "</TD></TR>");
			System.out.println("<TR>");
			System.out.println("<TD>Namespace(URI)</TD><TD>"
					+ ns.getNamespaceURI() + "</TD></TR>");

			j++;

		}

		for (Iterator i = element.getAttributes(); i.hasNext();) {
			int j = 0;
			if (j == 0) {
				// System.out.println("Attribute");
			}

			Attribute attr = (Attribute) i.next();
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(QName)</TD><TD>" + attr.getName()
					+ "</TD></TR>");
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(Local Name)</TD><TD>"
					+ attr.getName().getLocalPart() + "</TD></TR>");
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(Type)</TD><TD>"
					+ attr.getDTDType() + "</TD></TR>");
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(Value)</TD><TD>"
					+ attr.getValue() + "</TD></TR>");
			String prefix = attr.getName().getPrefix();
			if (prefix == "") {
				prefix = "-";
			}
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(Prefix)</TD><TD>" + prefix
					+ "</TD></TR>");
			String nsURI = attr.getName().getPrefix();
			if (nsURI == "") {
				nsURI = "-";
			}
			System.out.println("<TR>");
			System.out.println("<TD>Attribute(Namespace)</TD><TD>" + nsURI + "</TD></TR>");
			j++;
		}
	}
}

class CharactersHandler implements EventItf {
	public void handleEvent(XMLEvent event) {
		Characters characters = event.asCharacters();
		System.out.println("<TR>");
		System.out.println("<TD>Content</TD><TD>" + characters.getData()
				+ "</TD></TR>");
	}
}

class EndElementHandler implements EventItf {
	public void handleEvent(XMLEvent event) {
		System.out.println("</TBODY></TABLE><BR>");
	}
}
