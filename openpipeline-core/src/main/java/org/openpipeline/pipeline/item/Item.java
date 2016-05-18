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
package org.openpipeline.pipeline.item;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.openpipeline.pipeline.docfilter.XMLFilter;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.FastStringBuffer;

/**
 * An Item is the main object that the pipeline operates on. An Item is a document,
 * database record, or other data structure that contains fields and text.
 * The pipeline passes an Item from one {@link org.openpipeline.pipeline.stage.Stage} to the next where it gets
 * transformed in some manner at each step.
 * <p>
 * An Item is similar to a DOM tree. It maps very closely to an XML-like 
 * data structure. Each tag in an XML document is a {@link Node}. See the
 * Node class for more on the structure of Nodes.
 * <p>
 * Each item can carry a unique ID. See {@link #setItemId(String)}/{@link #getItemId()}.
 * <p>
 * Each item can carry attribute definitions. An "attribute" in this case is a path
 * through the tags of the document. See {@link AttributeDef} for more.
 * <p>
 * Each item can carry an "action". An action can be ACTION_ADD, ACTION_UPDATE, 
 * or ACTION_DELETE. The action is intended to tell the last stage in the pipeline 
 * what to do with the item. See {@link #setAction(int)}.
 */
public class Item {

	public static final int ACTION_ADD = 0; // default
	public static final int ACTION_UPDATE = 1;
	public static final int ACTION_DELETE = 2;

	private int action;
	private Node root;
	private FastStringBuffer buf;
	private String itemId;
	private Map<String, AttributeDef> attributeDefs;
	private DocBinary docBinary;
	private XMLFilter xmlFilter;

	public Item() {
		this(16);
	}
	
	/**
	 * Constructor
	 * @param bufSize the starting size of the internal text buffer
	 */
	public Item(int bufSize) {
		buf = new FastStringBuffer(bufSize);
		clear();
	}

	public Node getRootNode() {
		return root;
	}
	
	public void setRootNode(Node newRoot) {
		root = newRoot;
	}

	/**
	 * Clears and resets all internal variables.
	 */
	public void clear() {
		clear(false);
	}
	
	/**
	 * Clears and resets all internal variables, optionally preserving
	 * the docBinary object.
	 * @param keepDocBinary set true to refrain from clearing the docBinary object
	 */
	public void clear(boolean keepDocBinary) {
		this.root = new Node(this, null);
		this.root.setName("item");
		this.buf.clear();
		this.action = ACTION_ADD;
		this.itemId = null;
		this.attributeDefs = null;
		if (!keepDocBinary) {
			this.docBinary = null;
		}
	}

	public FastStringBuffer getBuffer() {
		return buf;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	/**
	 * Set the itemId, the primary key for this item.
	 * @param itemId the itemId
	 */
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	/**
	 * Get the itemId, the primary key for this item.
	 * @return the itemId, or null if one has not been set
	 */
	public String getItemId() {
		return itemId;
	}

	/**
	 * Convenience method equivalent to <code>importXML(new StringReader(xml));</code>
	 * @see XMLFilter
	 * @param xml the XML to import
	 * @throws XMLStreamException 
	 */
	public void importXML(String xml) throws PipelineException {
		importXML(new StringReader(xml));
	}

	/**
	 * Parses the specified XML and populates this item, creating nodes
	 * as needed. Replaces any existing data.
	 * @see XMLFilter
	 * @param in a stream of characters that contains XML.
	 * @throws XMLStreamException 
	 */
	public void importXML(Reader in) throws PipelineException {
		clear();
		if (xmlFilter == null) {
			xmlFilter = new XMLFilter();
		}
		xmlFilter.setReader(in);
		xmlFilter.processItem(this);
	}

	public String toString() {
		FastStringBuffer xmlBuf = new FastStringBuffer(buf.size() + 1024);
		appendXMLtoBuffer(xmlBuf);
		return xmlBuf.toString();
	}
	
	public void appendXMLtoBuffer(FastStringBuffer xmlBuf) {
		root.appendXMLtoBuffer(xmlBuf, false);
	}
	
	public void appendXMLtoBuffer(FastStringBuffer xmlBuf, boolean includeAnnotations) {
		xmlBuf.append("<items>");
		appendAttributeDefs(xmlBuf);
		root.appendXMLtoBuffer(xmlBuf, includeAnnotations);
		xmlBuf.append("</items>");
	}
	
	private void appendAttributeDefs(FastStringBuffer xmlBuf) {
		if (attributeDefs == null) {
			return;
		}
		
		int defCount = attributeDefs.size();
		AttributeDef [] arr = new AttributeDef[defCount];
		attributeDefs.values().toArray(arr);
		Arrays.sort(arr);
		
		for (int i = 0; i < defCount; i++) {
			arr[i].toXML(xmlBuf);
		}
	}

	static protected void appendAnnotations(FastStringBuffer xmlBuf, Map map) {
		if (map != null && !map.isEmpty()) {
			xmlBuf.append("<annotations>");
			
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Entry) it.next();
				String name = (String) entry.getKey();

				xmlBuf.append("<annot name=\"");
				xmlBuf.append(name);
				xmlBuf.append("\">");

				// don't xmlencode; it should come back as xml
				xmlBuf.append(entry.getValue().toString());

				xmlBuf.append("</annot>");
			}
			xmlBuf.append("</annotations>");
		}
	}
	
	/**
	 * This method makes it easier for a class to process all
	 * the nodes in an item. The class should implement
	 * the NodeVisitor interface. This method will cause the 
	 * NodeVisitor.processNode() method to be called once for each
	 * node in this item. 
	 * @param visitor the class that will visit each node
	 * @throws PipelineException some recoverable exception
	 */
	public void visitNodes(NodeVisitor visitor) throws PipelineException {
		visitNodes(visitor, root);
	}
		
	private void visitNodes(NodeVisitor visitor, Node node) throws PipelineException {
		visitor.processNode(node);
		if (node.hasChildren()) {
			// TODO make it so the visitor can add and delete children. Not possible now.
			int childCount = node.getChildCount();
			NodeList children = node.getChildren();
			for (int i = 0; i < childCount; i++) {
				visitNodes(visitor, children.get(i));
			}
		}
	}
	
	/**
	 * Return a Map of AttributeDef objects, where the key
	 * is the attributeId and the value is the AttributeDef.
	 * @return the Map, or null if none has been defined
	 */
	public Map <String, AttributeDef>getAttributeDefs() {
		if (attributeDefs == null) {
			attributeDefs = new LinkedHashMap();
		}
		return attributeDefs;
	}
	
	/**
	 * Set a Map of AttributeDef objects, where the key
	 * is the attributeId and the value is the AttributeDef.
	 * @param attributeDefs the Map of AttributeDefs
	 */
	public void setAttributeDefs(Map<String, AttributeDef> attributeDefs) {
		this.attributeDefs = attributeDefs;
	}

	/**
	 * Return true if this item has any AttributeDefs attached to it.
	 * @return true if AttributeDefs have been defined
	 */
	public boolean hasAttributeDefs() {
		return (attributeDefs != null) && (attributeDefs.size() > 0);
	}
	
	/**
	 * Return the attached DocBinary object. A DocBinary object contains
	 * the binary content of a file or other data item.
	 * @return the docBinary object, or null if none available
	 */
	public DocBinary getDocBinary() {
		return docBinary;
	}
	
	/**
	 * Set the attached DocBinary object. A DocBinary object contains
	 * the binary content of a file or other data item.
	 * @param docBinary the docBinary to attach
	 */
	public void setDocBinary(DocBinary docBinary) {
		this.docBinary = docBinary;
	}
	
}
