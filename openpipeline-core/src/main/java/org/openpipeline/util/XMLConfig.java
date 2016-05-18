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
package org.openpipeline.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A class for reading and writing XML-based configuration files. Intended as a 
 * general replacement for java.util.Properties files.
 * <p>
 * This class has advantages over properties files:<br>
 * 1. Names and values can be hierarchical, which means they can be grouped logically.<br>
 * 2. There can be multiple entries with the same name.<br>
 * 3. Names do not need to have values. This makes it easy to store simple lists.<br>
 * 4. The sequence of name/value pairs is preserved.<br>
 * <p>
 * These advantages make storing and retrieving configurations much more flexible. A sample
 * configuration file can look like this:
 * <p>
 * <code>
 * <pre>
 * <?xml version="1.0" encoding="UTF-8"?>
 * &lt;root>
 *   &lt;admin>Sam&lt;/admin>
 *   &lt;timeout>60&lt;/timeout>
 *   &lt;users>
 *     &lt;username>bob &amp;amp; bill&lt;/username>
 *     &lt;username>fred&lt;/username>
 *   &lt;/users>
 *   &lt;server>
 *     &lt;gigahertz>3.0&lt;/gigahertz>
 *   &lt;/server>
 * &lt;/root>
 * </pre>
 * </code>
 *
 * To read a config file from disk:<br>
 * <code><pre>
 * XMLConfig config = new XMLConfig();
 * config.load("/path/to/configfile.xml");
 * </pre></code>
 * 
 * To retrieve a value:<br>
 * <code><pre>
 * config.getValue("admin") -> "Sam"
 * </pre></code>
 * 
 * To retrieve a value as an integer:
 * <code><pre>
 * config.getIntValue("timeout") -> 60
 * </pre></code>
 * 
 * To iterate through some entries:
 * <code><pre>
 * List list = config.getChildren();
 * for (int i = 0; i < list.size(); i++) {
 *     XMLConfig child = (XMLConfig)list.get(i);
 *     if (child.getName().equals("user")) {
 *         String username = child.getValue("username");
 *         // etc	
 *     }
 * }
 * </pre></code>
 * 
 * To get a value in a hierarchy:
 * <code><pre>
 * double gigahertz = config.getChild("server").getDoubleValue("gigahertz");
 * </pre></code>
 * 
 * To add children, call addChild(). To remove or insert them, call getChildren()
 * and use the remove() and add() methods of the List interface.
 * 
 */
public class XMLConfig implements Comparable {
	private static final String SEP = System.getProperty("line.separator");
	
	private String name;
	private String value;
	private ArrayList<XMLConfig> children = new ArrayList<XMLConfig>();
	
	public XMLConfig() {
		name = "root";
	}
	
	public XMLConfig(String name) {
		this.name = name;
	}

	/**
	 * Load this configuration file from disk.
	 * @param filename the name of the file
	 * @throws IOException
	 */
	public void load(String filename) throws IOException {
		FileInputStream stream = new FileInputStream(filename);
		load(stream);
	}

	/**
	 * Load this configuration file from disk.
	 * @param file the file to load
	 * @throws IOException 
	 */
	public void load(File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		load(stream);
	}
	
	/**
	 * Load this configuration file from an InputStream.
	 * @param in the stream to read. Should contain a file in xml format
	 * @throws IOException
	 */
	public void load(InputStream in) throws IOException {
		try {
			InputStreamReader reader = new InputStreamReader(in, "UTF-8");
			load(reader);
		} catch (UnsupportedEncodingException e) {
			// should never get here.
			throw new Error();
		} finally {
			in.close();
		}
	}
	

	/**
	 * Load this configuration file from a URL.
	 * @param the URL to fetch. Contents should contain data in 
	 * UTF-8-encoded xml format
	 * @throws IOException
	 */
	public void load(URL url) throws IOException {
		InputStream in = null;
		try {
			URLConnection con = url.openConnection();
			in = con.getInputStream();
			if (in != null) {
				load(in);
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
	

	/**
	 * Load this configuration file from a Reader. Makes it easy to
	 * parse the xml if it's already in the form of a String:
	 * <p>
	 * <code>
	 *  load(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>XML goes here..."));
	 * </code>
	 * </p>
	 * @param reader the reader that streams the XML
	 * @throws IOException
	 */
	public void load(Reader reader) throws IOException {
		try {
			InputSource inputSource = new InputSource(reader);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader saxXmlReader = sp.getXMLReader();
			ConfigContentHandler handler = new ConfigContentHandler();
			saxXmlReader.setContentHandler(handler);
			saxXmlReader.parse(inputSource);
			reader.close();
			
			XMLConfig root = handler.getRoot();
			this.name = root.getName();
			this.value = root.getValue();
			this.children = root.children;

		} catch (SAXException se) {
			IOException ioe = new IOException();
			ioe.initCause(se);
			throw ioe;
		} catch (ParserConfigurationException pce) {
			IOException ioe = new IOException();
			ioe.initCause(pce);
			throw ioe;
		} finally {
			reader.close();
		}

	}

	/**
	 * Write this configuration object's data out to filename.
	 * @param filename the fully-qualified path of the output file
	 * @throws IOException
	 */
	public void save(String filename) throws IOException {
		File file = new File(filename);
		save(file);
	}
	

	/**
	 * Write this configuration object's data out to the file.
	 * @param file the output file 
	 * @throws IOException
	 */
	public void save(File file) throws IOException {
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}
		FileOutputStream out = null;
		try {
			// TODO if there is an error when assembling the buffer, a file still gets created. should not happen.
			out = new FileOutputStream(file);
			save(out);
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	
	/**
	 * Write this configuration object out to the OutputStream. Uses UTF-8 encoding.
	 * @param out the destination OutputStream.
	 * @throws IOException
	 */
	public void save(OutputStream out) throws IOException {
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(out, "UTF-8");
			save(writer);
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	/**
	 * Write this configuration object out to the Writer object.
	 * @param writer the destination Writer
	 * @throws IOException
	 */
	public void save(Writer writer) throws IOException {
		String out = this.toString();
		writer.write(out);
	}
	
	/**
	 * Return the entire file as a String.
	 */
	public String toString() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SEP);
		writeEntry(this, buf, "");
		return buf.toString();
	}
	
	/**
	 * Recursive function to write the file.
	 */
	private void writeEntry(XMLConfig node, FastStringBuffer buf, String indent) {

		buf.append(indent + "<" + node.getName() + ">");

		// a node either has a value or children, but not both
		List<XMLConfig> children = node.getChildren();
		if (children.size() > 0) {
			buf.append(SEP);
			for (XMLConfig child: children) {
				// recurse
				writeEntry(child, buf, indent + "  ");
			}
			buf.append(indent);
		} else {
			buf.appendWithXMLEncode(node.value);
		}
		
		buf.append("</" + node.getName() + ">" + SEP);
	}

	
	/**
	 * Returns the value associated with the name. Does not search any child nodes
	 * in the tree.
	 * 
	 * @param name the name of the value to search for
	 * @return the value, or null if the name is not found
	 */
	public String getProperty(String name) {
		XMLConfig child = getChild(name);
		// if we're found a leaf node
		if (child != null && child.getChildren().size() == 0) {
			return child.value;
		}
		return null;
	}

	/**
	 * Returns the value associated with the name. Does not search any child nodes
	 * in the tree.
	 * 
	 * @param name the name of the value to search for
	 * @param defaultValue the value to return if the name is not found
	 * @return the value, or defaultValue if the name is not found
	 */
	public String getProperty(String name, String defaultValue) {
		String value = getProperty(name);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	

	/**
	 * Calls getProperty(String name) and converts the value found from a String to an integer.
	 * @param name the name of the value to return
	 * @return the value as an integer, or -1 if the name is not found
	 */
	public int getIntProperty(String name) {
		return getIntProperty(name, -1);
	}

	/**
	 * Calls getProperty(String name) and converts the value found from a String to an integer.
	 * @param name the name of the value to return
	 * @param defaultValue the value to return if the name is not found
	 * @return the value as an integer, or -1 if the name is not found
	 */
	public int getIntProperty(String name, int defaultValue) {
		String value = getProperty(name);
		if (value == null)
			return defaultValue;
		return Integer.parseInt(value);
	}

	/**
	 * Calls getProperty(String name) and converts the value found from a String to a long integer.
	 * @param name the name of the value to return
	 * @param defaultValue the value to return if the name is not found
	 * @return the value as a long, or -1 if the name is not found
	 */
	public long getLongProperty(String name, long defaultValue) {
		String value = getProperty(name);
		if (value == null)
			return defaultValue;
		return Long.parseLong(value);
	}
	
	
	/**
	 * Calls getProperty(String name) and converts the value found from a String to a double.
	 * @param name the name of the value to return
	 * @return the value as a double, or -1 if the name is not found
	 */
	public double getDoubleProperty(String name) {
		return getDoubleProperty(name, -1);
	}

	/**
	 * Calls getProperty(String name) and converts the value found from a String to a double.
	 * @param name the name of the value to return
	 * @param defaultValue the value to return if the name is not found
	 * @return the value as a double, or -1 if the name is not found
	 */
	public double getDoubleProperty(String name, double defaultValue) {
		String value = getProperty(name);
		if (value == null)
			return defaultValue;
		return Double.parseDouble(value);
	}


	/**
	 * Calls getProperty(String name) and converts the value found from a String 
	 * to a boolean. 
	 * @param name the name of the value to return
	 * @param defaultValue the value to return if the name is not found
	 * @return the value as a boolean, or false
	 */
	public boolean getBooleanProperty(String name, boolean defaultValue) {
		String value = getProperty(name);
		return Util.toBoolean(value, defaultValue);
	}
	
	
	/**
	 * Searches the children of this node and returns the first child that matches the name.
	 * @param name the name of the child to find
	 * @return the child, or null if the name is not found
	 */
	public XMLConfig getChild(String name) {
		int childNum = findChild(name);
		if (childNum > -1) {
			return children.get(childNum);
		}
		return null;
	}
	
	/**
	 * Searches the children of this node and returns the index of the
	 * first child that matches the name.
	 * @param name the name of the child to find
	 * @return the child number, or -1 if the name is not found
	 */
	public int findChild(String name) {
		final int count = children.size();
		for (int i = 0; i < count; i++) {
			XMLConfig node = children.get(i);
			if (node.name.equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Remove the child or children with the specified name.
	 * @param name the name to look for
	 * @return true if any children were removed
	 */
	public boolean removeChild(String name) {
		boolean found = false;
		final int count = children.size();
		
		// must traverse backward to remove nodes
		for (int i = count - 1; i >= 0; i--) {
			XMLConfig node = children.get(i);
			if (node.name.equals(name)) {
				children.remove(i);
				found = true;
			}
		}
		return found;
	}
	
	/**
	 * Returns a List of the children of this node, or an empty list if there are none.
	 */
	public List<XMLConfig> getChildren() {
		if (children == null) {
			return Collections.EMPTY_LIST;
		}
		return children;
	}

	/**
	 * Set the name of this node. The root node in the tree is called "root" by default.
	 * @param name the name to assign to this node
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
	
	/**
	 * Create a child, add it to this node, and return it.
	 * @return the child
	 */
	public XMLConfig addChild(String name) {
		XMLConfig child = new XMLConfig(name);
		children.add(child);
		return child;
	}

	public void addChild(XMLConfig child) {
		children.add(child);
	}
	
	
	/**
	 * Set the name/value pair. If the name already exists, replace the value. If there
	 * is more than one instance of the name, this only affects the first one. 
	 * @param name the name of the property
	 * @param value the value of the property
	 */
	public void setProperty(String name, String value) {
		XMLConfig child = getChild(name);
		if (child == null) {
			addProperty(name, value);
		} else {
			child.value = value;
		}
	}

	/**
	 * Add a name/value pair. It's ok to add more than one property
	 * with the same name.
	 * @param name the name of the property
	 * @param value the value associated with the name
	 */
	public void addProperty(String name, String value) {
		XMLConfig node = new XMLConfig(name);
		node.value = value;
		children.add(node);
	}

	public void addBooleanProperty(String name, boolean value) {
		if (value) {
			addProperty(name, "Y");
		} else {
			addProperty(name, "N");
		}
	}
	

	/**
	 * Returns the name of this node
	 */
	public String getName() {
		return name;
	}
	
	protected void setValue(String value) {
		this.value = value;
	}

	/**
	 * When a property has more than one value, return them in a List.
	 * @param name name of the value to retrieve
	 * @return an List of the String values found. If none found, returns an
	 * empty List
	 */
	public List<String> getValues(String name) {
		ArrayList vals = new ArrayList();
		for (int i = 0; i < children.size(); i++) {
			XMLConfig node = children.get(i);
			if (node.getName().equals(name)) {
				if (node.value != null) {
					vals.add(node.value);
				}
			}
		}
		return vals;
	}
	
	/**
	 * When a property has more than one value, return them in an array of String.
	 * @param name name of the value to retrieve
	 * @return an array of the String values found. If none found, returns an
	 * array of length 0.
	 */
	public String [] getValuesArray(String name) {
		List<String> list = getValues(name);
		String [] arr = new String [list.size()];
		list.toArray(arr);
		return arr;
	}
	
	/**
	 * Get the value associated with this node. Nodes that have children should
	 * not have a value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Merge another config file into this one. Replaces any
	 * existing params with new ones that have the same name.
	 * @param newConf new configuration params to add
	 */
	public void merge(XMLConfig newConf) {
		for (XMLConfig newChild: newConf.getChildren()) {
			String name = newChild.getName();
			int childNum = findChild(name);
			if (childNum == -1) {
				addChild(newChild);
			} else {
				children.set(childNum, newChild);
			}
		}
	}

	/**
	 * Compares one property to another by doing string comparisons
	 * on name and then value.
	 */
	public int compareTo(Object o) {
		XMLConfig other = (XMLConfig) o;
		int cmp = this.getName().compareTo(other.getName());
		if (cmp == 0) {
			if (this.getValue() != null && other.getValue() != null) {
				cmp = this.getValue().compareTo(other.getValue());
			}
		}
		return cmp;
	}

	/**
	 * Use Java reflection to find setter methods in the object,
	 * look for matching parameter names in this XMLConfig,
	 * and set the values. Will call a setter only if
	 * the parameter is String, boolean, int, long, float, or double.
	 * @param target the object whose setters will be called
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public void applyTo(Object target) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		
		for (Method method: target.getClass().getMethods()) {

			int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				continue;
			}
			
			String methodName = method.getName();
			if (!methodName.startsWith("set")) {
				continue;
			}
			
			Class [] types = method.getParameterTypes();
			if (types.length != 1) {
				continue;
			}
			
			String paramName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
			String value = getProperty(paramName);
			if (value == null) {
				continue;
			}
			
			Class type = types[0];
			//String typeName = type.getName();
			
			if (type.equals(String.class)) {
				method.invoke(target, value);
			
			/*
			} else if (type.equals(String[].class)) {
				String [] arr = getValuesArray(paramName);
				method.invoke(target, (Object)arr);
			*/

			} else if (type.equals(int.class)) {
				method.invoke(target, Integer.parseInt(value));
				
			} else if (type.equals(boolean.class)) {
				method.invoke(target, Util.toBoolean(value, false));
				
			} else if (type.equals(float.class)) {
				method.invoke(target, Float.parseFloat(value));
				
			} else if (type.equals(double.class)) {
				method.invoke(target, Double.parseDouble(value));
				
			} else if (type.equals(long.class)) {
				method.invoke(target, Long.parseLong(value));
				
			}
		}
	}

	
}

@SuppressWarnings("unused")
class ConfigContentHandler implements ContentHandler {
	private XMLConfig root;
	private Stack stack = new Stack();
	private FastStringBuffer buf = new FastStringBuffer();
	
	public ConfigContentHandler() {
	}

	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

		if (stack.size() == 0) {
			root = new XMLConfig(qName);
			stack.push(root);
		} else {
			XMLConfig parent = (XMLConfig) stack.peek();
			XMLConfig node = parent.addChild(qName);
			stack.push(node);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		buf.trim();
		if (buf.size() > 0) {
			XMLConfig node = (XMLConfig) stack.peek();
			node.setValue(buf.toString());
			buf.clear();
		}
		stack.pop();
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		buf.append(ch, start, length);
	}

	public XMLConfig getRoot() {
		return root;
	}
	
	public void endDocument() throws SAXException {}
	public void endPrefixMapping(String prefix) throws SAXException {}
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {	}
	public void processingInstruction(String target, String data) throws SAXException {	}
	public void setDocumentLocator(Locator locator) {}
	public void skippedEntity(String name) throws SAXException {}
	public void startDocument() throws SAXException {}
	public void startPrefixMapping(String prefix, String uri) throws SAXException {	}
}

