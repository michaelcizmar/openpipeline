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
package org.openpipeline.pipeline.docfilter;

import java.io.InputStream;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openpipeline.pipeline.item.AttributeDef;
import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.TextValue;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.Util;
import org.openpipeline.util.XMLConfig;




/**
 * Implementation of the {@link DocFilter} class for XML files.
 * By default, it treats an XML file as a single item, and reads it directly 
 * into an Item. If the first tag in the file is "&lt;items>", it treats 
 * it as a multi-item file. It will look for "&lt;item>" tags and 
 * treat the contents of each as an individual item. 
 */
public class XMLFilter extends DocFilter {
	
	public static final String DEFAULT_ITEMS_TAG = "items";
	public static final String DEFAULT_ITEM_TAG = "item";
	public static final String DEFAULT_ITEMID_TAG = "item_id";
	public static final String DEFAULT_ATTRIBUTE_TAG = "attribute";
	public static final boolean DEFAULT_OMIT_WHITESPACE = true;
	
	private String itemsTag = DEFAULT_ITEMS_TAG;
	private String itemTag = DEFAULT_ITEM_TAG;
	private String itemIdTag = DEFAULT_ITEMID_TAG;
	private String attributeTag = DEFAULT_ATTRIBUTE_TAG;
	private boolean omitWhitespace = DEFAULT_OMIT_WHITESPACE;

	private XMLInputFactory factory = XMLInputFactory.newInstance();
	private Reader reader;
	private InputStream in;
	
	// this is true if we're in MultiItemMode, that is,
	// we acknowledge <item> and <item_id> tags.
	private boolean multiItemMode;
	private boolean initialized;
	private boolean debug;
	
	public XMLFilter() {
		factory.setProperty("javax.xml.stream.isCoalescing", new Boolean(true));
		
		// Prevents XML parsing from going into an infinite loop.
		factory.setProperty("javax.xml.stream.supportDTD", new Boolean(false));
		debug = Server.getServer().getDebug();
	}
	

	@Override
	public void setParams(XMLConfig params) {
		super.setParams(params);
		itemsTag = params.getProperty("items-tag", DEFAULT_ITEMS_TAG);
		itemTag = params.getProperty("item-tag", DEFAULT_ITEM_TAG);
		itemIdTag = params.getProperty("itemid-tag", DEFAULT_ITEMID_TAG);
		attributeTag = params.getProperty("attribute-tag", DEFAULT_ATTRIBUTE_TAG);
		omitWhitespace = params.getBooleanProperty("omit-whitespace", omitWhitespace);
	}
	
	/**
	 * Set a Reader with the XML to process. Useful if the Item does
	 * not carry a DocBinary annotation.
	 * @param reader a Reader containing XML 
	 */
	public void setReader(Reader reader) {
		this.reader = reader;
		this.in = null;
	}

	/**
	 * Set an InputStream with the XML to process. Useful if the Item does
	 * not carry a DocBinary annotation.
	 * @param in the InputStream containing the document content
	 */
	public void setInputStream(InputStream in) {
		this.in = in;
		this.reader = null;
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		multiItemMode = false;
		initialized = false;
		
		DocBinary docBinary = item.getDocBinary();
		XMLStreamReader parser = null;
		
		try {
			
			parser = getParser(docBinary);
			
			int count = 0;
			while (true) {
				if (!parseWithStax(item, parser)) {
					break;
				}
				if (debug) {
					count++;
					Server.getServer().getLogger().debug("XMLFilter doc processed. id=" + item.getItemId() + " count=" + count);
				}
				if (nextStage != null) {
					nextStage.processItem(item);
				}
			}

		} catch (Throwable t) {
			throw new PipelineException(t);
		
		} finally {
			try {
				if (parser != null) {
					parser.close();
				}
			} catch (Throwable t) {
				// ok to ignore
			}
		}
	}

	private XMLStreamReader getParser(DocBinary docBinary)
			throws XMLStreamException {

		XMLStreamReader parser;

		if (reader != null) {
			parser = factory.createXMLStreamReader(reader);
			
		} else if (in != null) {
			parser = factory.createXMLStreamReader(in);
			
		} else if (docBinary != null) {
			String encoding = docBinary.getEncoding();
			if(encoding == null){
				parser = factory.createXMLStreamReader(docBinary.getInputStream());
			}else{
				parser = factory.createXMLStreamReader(docBinary.getInputStream(),encoding);
			}
			
		} else {
			throw new IllegalStateException(
					"You must call either setReader() or setInputStream() or the Item must carry a docBinary object");
		}

		return parser;
	}

	/**
	 * Fetch the next item from the reader.
	 * @return true if there was anything to parse. 
	 */
	private boolean parseWithStax(Item item, XMLStreamReader parser) throws Exception {

		Node currentTag = null;
		boolean hasTag = false;
		
		OUTER_LOOP:
		while (parser.hasNext()) {
			int event = parser.next();

			String name;
			
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				
				// don't clear the item until we're sure that there's any data. Makes it easier
				// for calling programs to deal with single-item files
				if (!hasTag) {
					hasTag = true;
					item.clear(true);
				}

				/* If the prefix is null add the tag in the following format
				 * <TAGNAME></TAGNAME>
				 * If the prefix is not null add the tag in the following format
				 * <PREFIX:TAGNAME></PREFIX:TAGNAME>
				 */
				name = parser.getLocalName();
				String prefix = parser.getPrefix();
				if (prefix != null && prefix.length() > 0) {
					name = prefix + ":" + name;
				}				

				if (!initialized) {
					initialized = true;
					
					// check to see if the first tag is <items>
					if (itemsTag.equals(name)) {
						multiItemMode = true;
						break;
					}
				}
				
				
				if (currentTag == null) {
					
					if (multiItemMode) {
						
						// if we get here, we're before the first item or between items
						
						if (attributeTag.equals(name)) {
							AttributeDef def = new AttributeDef();
							
							// populate it
							int attrCount = parser.getAttributeCount();
							for (int i = 0; i < attrCount; i++) {
								String propName = parser.getAttributeLocalName(i);
								String propValue = parser.getAttributeValue(i);
								if (propName.equals("attribute_id")) {
									def.setAttributeId(propValue);
								} else {
									def.put(propName, propValue);
								}
							}	
							
							item.getAttributeDefs().put(def.getAttributeId(), def);
							break;
						}
						
						if (itemTag.equals(name)) {
							
							String action = parser.getAttributeValue(null, "action");
							if ("delete".equals(action)) {
								item.setAction(Item.ACTION_DELETE);
							} else if ("update".equals(action)) {
								item.setAction(Item.ACTION_UPDATE);
							} else {
								item.setAction(Item.ACTION_ADD);
							}
						
						} else {
							// skip data until we reach the start of an item
							break;
						}
					}
					
					currentTag = item.getRootNode();
					currentTag.setName(name);
				
				} else {
					// create a child and make it current
					currentTag = currentTag.addNode(name);
				}

				// treat namespaces just like attributes
				int nsCount = parser.getNamespaceCount();
				for (int i = 0; i < nsCount; i++) {
					String nsPrefix = parser.getNamespacePrefix(i);
					String nsURI = parser.getNamespaceURI(i);
					if (nsPrefix.length() > 0) {
						currentTag.addAttribute("xmlns:" + nsPrefix, nsURI);
					} else {
						currentTag.addAttribute("xmlns", nsURI);
					}
				}
				
				// Add attribute values to the current tag.
				int attrCount = parser.getAttributeCount();
				for (int i = 0; i < attrCount; i++) {
					QName qName = parser.getAttributeName(i);
					String attrValue = parser.getAttributeValue(i);
					
					// Only adding values that are not empty
					if (attrValue.length() > 0) {
						String attrPrefix = qName.getPrefix();
						String localPart = qName.getLocalPart();
						
						String attrName;
						if (attrPrefix == null || attrPrefix.length() == 0) {
							attrName = localPart;
						} else {
							attrName = attrPrefix + ":" + localPart;
						}

						currentTag.addAttribute(attrName, attrValue);
						if (multiItemMode && attrName.equals(itemIdTag)) {
							item.setItemId(attrValue);
						}
					}
				}				

				break;

			case XMLStreamConstants.CHARACTERS:
				
				if (currentTag == null) {
					continue;
				}
				
				char[] value = parser.getTextCharacters();
				int off = parser.getTextStart();
				int size = parser.getTextLength();
				
				if (!omitWhitespace || !Util.isWhitespace(value, off, size)) {
					currentTag.addValue(value, off, size);
				}
				break;

			case XMLStreamConstants.END_ELEMENT:

				if (currentTag == null) {
					continue;
				}

				name = parser.getLocalName();

				if (multiItemMode) {
					
					// if we hit the end of the item, quit
					if (itemTag.equals(name)) {
						break OUTER_LOOP;
					}
					
					if (itemIdTag.equals(name)) {
						TextValue val = currentTag.getValue();
						if (val == null) {
							throw new Exception("Item is missing an item_id in: " + name);
						}
						
						String itemId = currentTag.getValue().toString();
						item.setItemId(itemId);
					}
				}
				
				/*
				// remove empty tags
				if (currentTag.isEmpty()) {
					Node parent = currentTag.getParent();
					currentTag.remove();
					currentTag = parent;
					
				} else {
				*/
					currentTag = currentTag.getParent();
				//}
				
				break;
			}
		}
		
		// returns true if we found anything to parse
		return hasTag;
	}

	
	@Override
	public String[] getDefaultExtensions() {
		String [] exts = { "xml" };
		return exts;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		String[] mimetypes = { "application/xml" };
		return mimetypes;
	}

	@Override
	public String getDescription() {
		return "Parses XML files";
	}

	@Override
	public String getDisplayName() {
		return "XMLFilter";
	}

	@Override
	public String getDocType() {
		return "xml";
	}

	@Override
	public String getConfigPage() {
		return "docfilter_xmlfilter.jsp";
	}



}
