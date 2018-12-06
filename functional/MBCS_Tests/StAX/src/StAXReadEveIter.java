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
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class StAXReadEveIter {
	public static void main(String[] args) {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		BufferedInputStream stream;
		Map<Integer, EventItf> maps = new HashMap<Integer, EventItf>();
		maps.put(XMLEvent.START_DOCUMENT, new StartDocumentHandler());
		maps.put(XMLEvent.START_ELEMENT, new StartElementHandler());
		maps.put(XMLEvent.CHARACTERS, new CharactersHandler());
		maps.put(XMLEvent.END_ELEMENT, new EndElementHandler());

		try {
			stream = new BufferedInputStream(new FileInputStream(args[0]));
			XMLEventReader reader = factory.createXMLEventReader(stream);

			EventFilter filter = new EventFilter() {
				public boolean accept(XMLEvent event) {
					return event.isStartElement() || event.isCharacters()
							|| event.isStartDocument() || event.isEndElement() ;
				}
			};
			reader = factory.createFilteredReader(reader, filter);
			while (reader.hasNext()) {
				XMLEvent event = reader.nextEvent();
				EventItf eitf = maps.get(event.getEventType());
				eitf.handleEvent(event);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}
}
