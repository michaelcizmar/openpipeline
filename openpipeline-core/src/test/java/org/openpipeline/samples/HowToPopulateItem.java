/*******************************************************************************
 * Copyright 2010 Dieselpoint, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openpipeline.samples;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.util.FastStringBuffer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Sample code for populating an Item manually and by using SAX and Stax XML parsers.
 */
public class HowToPopulateItem {

	private static final String sampleXML = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
			+ "<!-- a comment  -->"
			+ "<root>" + "Some text" 
			+ "<an_element name0=\"value0\" name1=\"value1\">" 
			+ "<another_element/>"
			+ "More text" 
			+ "</an_element>" 
			+ "<an_element name0=\"value0\" name1=\"value1\">" 
			+ "More text"
			+ "<another_element/>" 
			+ "</an_element>" 
			+ "</root>";

	public static void main(String[] args) throws Exception {
		HowToPopulateItem pop = new HowToPopulateItem();
		pop.run();
	}

	private void run() throws Exception {
		Item item = new Item();
		populateManually(item);
		parseWithStax();
		parseWithSAX();
	}

	private void populateManually(Item item) {
		/* The following code represents the scheme below.
		 * <root>
		 * 	<element name="value" foo="bar">
		 * 		some text
		 * 		<anothertag>more text</anothertag>
		 * 	</element>
		 * </root>
		 */

		item.clear();
		Node root = item.getRootNode();
		Node element = root.addNode("element");
		element.addAttribute("name", "value");
		element.addAttribute("foo", "bar");
		element.addValue("some text");
		
		// when you add a new child to a node, any text previously added to that
		// node becomes a child itself. So the line below actually creates
		// two new nodes.
		element.addNode("anothertag", "more text");

		System.out.println("Manually populated:");
		System.out.println(item.toString());

		// remove <anothertag>
		Node elem = root.getChildren().get(0);
		Node anotherTag = elem.getChildren().get(1);
		anotherTag.remove();

		System.out.println("With anothertag removed:");
		System.out.println(item.toString());
	}

	/**
	 * Parse the sample xml with Stax and populate an item. This
	 * code is a simplified version of the code in 
	 * org.openpipeline.pipeline.docfilter.XMLFilter.
	 */
	@SuppressWarnings("null")
	private void parseWithStax() throws Exception {
		
		Item item = new Item();

		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty("javax.xml.stream.isCoalescing", new Boolean(true));
		XMLStreamReader parser = factory.createXMLStreamReader(new StringReader(sampleXML));

		Node currentTag = null;
		
		while (parser.hasNext()) {
			int event = parser.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:

				String name = parser.getName().toString();
				
				if (currentTag == null) {
					currentTag = item.getRootNode();
					currentTag.setName(name);
				
				} else {
					// create a child and make it current
					currentTag = currentTag.addNode(name);
				}

				for (int i = 0; i < parser.getAttributeCount(); i++) {
					String attrName = parser.getAttributeName(i).toString();
					String attrValue = parser.getAttributeValue(i);
					currentTag.addAttribute(attrName, attrValue);
				}

				break;

			case XMLStreamConstants.CHARACTERS:

				char[] value = parser.getTextCharacters();
				int off = parser.getTextStart();
				int size = parser.getTextLength();
				currentTag.addValue(value, off, size);
				break;

			case XMLStreamConstants.END_ELEMENT:
				currentTag = currentTag.getParent();
				break;
			}
		}
		parser.close();

		System.out.println("Populated with Stax:");
		System.out.println(item.toString());
	}

	/**
	 * Parse the sample xml with a SAX parser.
	 */
	private void parseWithSAX() throws Exception {

		/*
		 * This is all just setup for the real code, which is in the ItemContentHandler
		 */
		InputSource inputSource = new InputSource(new StringReader(sampleXML));
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader saxXmlReader = sp.getXMLReader();

		ItemContentHandler handler = new ItemContentHandler();

		// create an item to populate and pass it to the handler
		Item item = new Item();
		handler.setItem(item);

		saxXmlReader.setContentHandler(handler);
		saxXmlReader.parse(inputSource);

		System.out.println("Populated with SAX:");
		System.out.println(item.toString());
	}

}

@SuppressWarnings("unused")
/**
 * The SAX parser calls methods in this class as it parses the XML. This class
 * takes the events and inserts them as Nodes in the Item.
 */
class ItemContentHandler implements ContentHandler {

	private Item item;
	private Node currentTag;
	private FastStringBuffer buf = new FastStringBuffer();

	public void setItem(Item item) {
		this.item = item;
	}

	public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {

		saveTextNode();
		
		if (currentTag == null) {
			currentTag = item.getRootNode();
			currentTag.setName(name);
		
		} else {
			// create a child and make it current
			currentTag = currentTag.addNode(name);
		}

		for (int i = 0; i < atts.getLength(); i++) {
			String attrName = atts.getQName(i);
			String attrValue = atts.getValue(i);
			currentTag.addAttribute(attrName, attrValue);
		}
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		saveTextNode();
		currentTag = currentTag.getParent();
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		buf.append(ch, start, length);
	}

	private void saveTextNode() {
		buf.trim();
		if (buf.size() > 0) {
			currentTag.addValue(buf.getArray(), 0, buf.size());
		}
		buf.clear();
	}

	/*
	 * The callback methods below are ignored for the purpose of this sample
	 */
	public void startDocument() throws SAXException {
	}

	public void endPrefixMapping(String prefix) throws SAXException {
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	public void processingInstruction(String target, String data) throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) throws SAXException {
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	public void endDocument() throws SAXException {
	}
}
