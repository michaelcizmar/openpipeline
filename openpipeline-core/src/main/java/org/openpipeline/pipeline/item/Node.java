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

import java.util.HashMap;
import java.util.Map;

import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastStringBuffer;


/**
 * A Node represents an element in a XML tree. A Node can be a tag or an 
 * attribute (name/value pair) within a tag. It can also have a block of text (a "value"). 
 * <p>
 * All node types can carry annotations, though it's most common to
 * add them only to leaf nodes. An annotation is really
 * just an arbitrary named object. It can be anything that should be attached
 * to the node.
 * <p>
 * By convention, there are certain standard types of annotations. An
 * object named "token" is a TokenList of the tokens in the text. An
 * object named "skip" is a TokenList of skip tokens. (Skip tokens
 * are spans of the text that should be skipped for text analytics 
 * purposes.)
 * <p>
 * Annotations can be helpful when passing an item from stage to stage; an 
 * earlier stage can attach some information to the node which can be 
 * consumed by a later stage. To attach information to the Item itself,
 * attach it to the root node.
 */
public class Node {
	
	private HashMap annotationsMap;
	private NodeList attributes;
	private NodeList children;
	private Item item;
	private String name;			// name of this node, correspond to tag name
	private Node parent; 
	private TextValue value;
	private boolean isAttribute = false;

	/* The rules:
	 * A node can have name and attributes, and either children or value but not both
	 * It can only have attributes if it also has a name.
	 * It can only have children if it also has a name.
	 */
	
	public Node(Item item, Node parent) {
		this.item = item;
		this.parent = parent;
	}
	
	public Node addAttribute(String name, String value) {
		Node attr = new Node(item, this);
		attr.setName(name);
		attr.setValue(value);
		addAttribute(attr);
		return attr;
	}
	

	public void addAttribute(Node attr) {
		getAttributes().append(attr);
		attr.setIsAttribute(true);
	}
	
	public void setIsAttribute(boolean isAttribute) {
		this.isAttribute = isAttribute;
	}
	
	public boolean isAttribute() {
		return isAttribute;
	}

	public NodeList getAttributes() {
		if (attributes == null) {
			attributes = new NodeList();
		}
		return attributes;
	}
	

	public TextValue getAttribute(String name) {
		if (attributes == null) {
			return null;
		}
		for (Node attr: attributes) {
			if (name.equals(attr.getName())) {
				return attr.getValue();
			}
		}
		return null;
	}
	
	
	
	public Node addNode(String name) {
		
		// if this node already contains text, make it a child and demote it
		if (value != null) {
			demoteValue();
		}
		
		Node node = new Node(item, this);
		node.setName(name);
		getChildren().append(node);
		return node;
	}

	/**
	 * Make any existing text assigned to this node a child.
	 */
	private void demoteValue() {
		Node textNode = new Node(item, this);
		textNode.value = this.value;
		getChildren().append(textNode);
		this.value = null;
	}

	public Node addNode(String name, char[] array, int offset, int size) {
		Node node = addNode(name);
		node.setValue(array, offset, size);
		return node;
	}


	/**
	 * Add a node with the given name and value. The name can
	 * be null for a text-only node.
	 * @param name the name of the node. If not null, becomes a tag in the xml.
	 * @param value the text underneath the tag
	 * @return the node that was added
	 */
	public Node addNode(String name, String value) {
		Node node = addNode(name);
		node.setValue(value);
		return node;
	}
	

	/**
	 * Adds a text value to this node. If the node already has
	 * a text value, then the existing value is demoted to a child,
	 * and the new one is also added as a child. If the node does not have an existing
	 * value, then this method behaves like setValue() and assigns it.
	 * @param array a char array that contains the text to add
	 * @param off the offset within the char array where the text to add starts
	 * @param size the number of chars to add
	 */
	public void addValue(char[] array, int off, int size) {
		
		// if this node already contains text, make it a child and demote it
		if (value != null) {
			demoteValue();
			addNode(null, array, off, size);
			
		} else if (hasChildren()) {
			// if it has children, text has to be added as another child
			addNode(null, array, off, size);
			
		} else {
			setValue(array, off, size);
		}
	}
	
	
	/**
	 * Adds a text value to this node. If the node already has
	 * a text value, then the existing value is demoted to a child,
	 * and the new one is also added as a child. If the node does not have an existing
	 * value, then this method behaves like setValue() and assigns it.
	 * @param text the text to add
	 */
	public void addValue(String text) {
		// if this node already contains text, make it a child and demote it
		if (value != null) {
			demoteValue();
			addNode(null, text);
			
		} else if (hasChildren()) {
			// if it has children, text has to be added as another child
			addNode(null, text);
			
		} else {
			setValue(text);
		}
	}
	
	/**
	 * Append the contents of this object to the specified buffer in
	 * the form of a fragment of XML.
	 * @param xmlBuf the buffer to receive the data
	 */
	public void appendXMLtoBuffer(FastStringBuffer xmlBuf, boolean includeAnnotations) {
		
		// if name is null, then this is a text-only node
		if (name == null) {
			if (value != null) {
				xmlBuf.appendWithXMLEncode(value.getArray(), value.getOffset(), value.size());
				if (includeAnnotations) {
					Item.appendAnnotations(xmlBuf, getAnnotationsMap());
				}
			}
			return;
		}
		
		// treat this node as a tag
		xmlBuf.append('<');
		xmlBuf.append(name);

		// add the attributes to the tag
		if (hasAttributes()) {
			int attrCount = getAttributeCount();
			NodeList attrs = getAttributes();
			for (int i = 0; i < attrCount; i++) {
				Node attr = attrs.get(i);
				xmlBuf.append(' ');
				attr.appendXMLtoBufferAsAttr(xmlBuf);
			}
		}
		xmlBuf.append('>');

		if (value != null) {
			xmlBuf.appendWithXMLEncode(value.getArray(), value.getOffset(), value.size());
			if (includeAnnotations) {
				Item.appendAnnotations(xmlBuf, getAnnotationsMap());
			}
		}
		
		if (hasChildren()) {
			int childCount = getChildCount();
			NodeList children = getChildren();
			for (int i = 0; i < childCount; i++) {
				Node node = children.get(i);
				node.appendXMLtoBuffer(xmlBuf, includeAnnotations);
			}
		}

		xmlBuf.append("</");
		xmlBuf.append(name);
		xmlBuf.append('>');
	}

	public int getAttributeCount() {
		if (attributes == null) {
			return 0;
		}
		return attributes.size();
	}

	public boolean hasAttributes() {
		return (attributes != null && attributes.size() > 0);
	}

	/**
	 * Append this node to the buffer in attribute form, that is, 'name="value"'.
	 * @param xmlBuf
	 */
	private void appendXMLtoBufferAsAttr(FastStringBuffer xmlBuf) {
		xmlBuf.append(name);
		xmlBuf.append("=\"");
		xmlBuf.appendWithXMLEncode(value.getArray(), value.getOffset(), value.size());
		xmlBuf.append('"');
	}
	


	/**
	 * Retrieve an object that was attached to this node by a call to putAnnotations().
	 * @param name the name of the object
	 * @return the object, or null if no object with this name is found
	 */
	public Object getAnnotations(String name) {
		if (annotationsMap == null) {
			return null;
		}
		return annotationsMap.get(name);
	}

	/**
	 * Return the object's internal map that maps names to annotations.
	 * @return a Map, or null if there are no annotations
	 */
	public Map getAnnotationsMap() {
		return annotationsMap;
	}


	public int getChildCount() {
		if (children == null) {
			return 0;
		}
		return children.size();
	}

	public NodeList getChildren() {
		if (children == null) {
			children = new NodeList();
		}
		return children;
	}

	public Item getItem() {
		return item;
	}


	public Node getParent() {
		return parent;
	}

	/**
	 * Returns the text associated with this node. May be null.
	 * @return the span of text
	 */
	public TextValue getValue() {
		return value;
	}
	
	public boolean hasChildren() {
		return (children != null && children.size() > 0);
	}



	/**
	 * Attach a named object to this node. 
	 * @param name the name of the object
	 * @param annotations any arbitrary object
	 */
	public void putAnnotations(String name, Object annotations) {
		if (annotationsMap == null) {
			annotationsMap = new HashMap();
		}
		annotationsMap.put(name, annotations);
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setParent(Node tag) {
		this.parent = tag;
	}

	public void setValue(String text) {
		if (value == null) {
			value = new TextValue(item.getBuffer());
		}
		value.setOffset(value.getBuffer().size());
		value.getBuffer().append(text);
		value.setSize(text.length());
	}


	public void setValue(CharSpan text) {
		if (text == null) {
			value = null;
			return;
		}
		setValue(text.getArray(), text.getOffset(), text.size());
	}
	
	private void setValue(char[] array, int offset, int size) {
		if (value == null) {
			value = new TextValue(item.getBuffer());
		}
		
		// if the new value is in the existing array, then we just
		// point to it. Otherwise we append it to the end and point to it
		if (value.getArray() == array) {
			value.setOffset(offset);
			value.setSize(size);
		} else {
			value.setOffset(value.getBuffer().size());
			value.getBuffer().append(array, offset, size);
			value.setSize(size);
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the full path to this node, including 
	 * parent nodes separated by "/". Excludes the root node.
	 * Does not start with a "/".
	 * @return
	 */
	public String getNamePath() {
		FastStringBuffer buf = new FastStringBuffer();
		getNamePathInternal(buf, this);
		return buf.toString();
	}
	
	private void getNamePathInternal(FastStringBuffer buf, Node node) {
		// this works by recursing all the way up, and then
		// accumulating the path on the way back down
		if (node.parent == null) {
			// if this is the root node, return, don't store "item"
			return;
		}
		
		// recurse up the path
		getNamePathInternal(buf, node.parent);
		
		// on the way back down...
		if (buf.size() > 0) {
			buf.append('/');
		}
		if (node.isAttribute) {
			buf.append('@');
		}
		buf.append(node.name);
	}
	

	/**
	 * Remove this node from the parent. Effectively deletes it from the item.
	 */
	public void remove() {
		NodeList list = parent.getChildren();
		int count = parent.getChildCount();
		for (int i = 0; i < count; i++) {
			if (this == list.get(i)) {
				list.remove(i);
				break;
			}
		}
	}

	/**
	 * Returns true if this node has no children, attributes, or text.
	 * @return true if this is an empty node
	 */
	public boolean isEmpty() {
		if (children != null && children.size() > 0) {
			return false;
		}
		if (attributes != null && attributes.size() > 0) {
			return false;
		}
		if (value != null) {
			return false;
		}
		return true;
	}

	/**
	 * Search for a node with the specified name and return it.
	 * @param name name of the node (tag) to find
	 * @return the node, or null if not found
	 */
	public Node getChild(String name) {
		if (this.hasChildren()) {
			NodeList children = this.getChildren();
			for (Node child: children) {
				if (name.equals(child.getName())) {
					return child;
				}
				
				// else recurse
				Node node = child.getChild(name);
				if (node != null) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Search for a child with the specified name and return
	 * any text value associated with it as a String.
	 * @param name the name of the node to find
	 * @return the value as a String, or null if the node is not found, or null if
	 * the node is found, but does not have a text value
	 */
	public String getChildValue(String name) {
		Node child = getChild(name);
		if (child == null) {
			return null;
		}
		
		TextValue val = child.getValue();
		if (val == null) {
			return null;
		}
		return val.toString();
	}

	public String toString() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<");
		buf.append(name);
		if (attributes != null) {
			buf.append(' ');
			buf.append(attributes.toString());
		}
		if (value == null) {
			buf.append("/>");
		} else {
			buf.append(">");
			buf.append(value.toString());
			buf.append("</");
			buf.append(name);
			buf.append(">");
		}
		return buf.toString();
	}
	
}
