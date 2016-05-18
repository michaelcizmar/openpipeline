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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.openpipeline.util.FastStringBuffer;


/**
 * Holds the definition of an attribute. An "attribute" in an item
 * is akin to a field in a database. The name of the attribute 
 * (known as "attributeId") is the full path to the field in an
 * XML structure. For example:
 * 
 * <code><pre>
 * &lt;item>
 *   &lt;title>A title&lt;/title>
 *   &lt;authors>
 *     &lt;author>Bob&lt;/author>
 *     &lt;author>Bill&lt;/author>
 *   &lt;/authors>
 * &lt;/item>
 * </pre></code>
 * 
 * There are two attributes in the structure above: "title" and "authors/author".
 * The root &lt;item> tag is usually omitted from the attributeId. There are
 * three attribute values:
 * <pre>
 * title = "A title"
 * authors/author = "Bob"
 * authors/author = "Bill"
 * </pre>
 * This class contains a definition for an attribute. A definition consists
 * of an attributeId and some properties. The attributeId
 * field is required and serves as the unique ID for 
 * an AttributeDef. It's typical to have properties like "datatype" 
 * and "description".
 */
public class AttributeDef implements Comparable<AttributeDef> {
	
	private Map<String, String> map = new HashMap();
    private String attributeId;
    
    /**
     * Get the ID of this attribute.
     */
	public String getAttributeId() {
		return attributeId;
	}

	/**
	 * Get the ID of this attribute
	 * @param attributeId the ID of this attribute definition
	 */
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

	/**
	 * Set a property.
	 * @param name the name of the property
	 * @param value the value of the property
	 */
	public void put(String name, String value) {
		map.put(name, value);
	}
	
	/**
	 * Get a property.
	 * @param name the name of the property
	 * @return the value of the property, or null if it doesn't exist
	 */
	public String get(String name) {
		return map.get(name);
	}
	
	/**
	 * Return a map with all of the properties.
	 * @return a map
	 */
	public Map getPropertiesMap() {
		return map;
	}

	public String toString() {
		return attributeId;
	}
	
	/**
	 * Append the contents of this object to the buffer as XML
	 * @param buf the buffer to receive the XML
	 */
	public void toXML(FastStringBuffer buf) {
		buf.append("<attribute");
		Iterator it = map.entrySet().iterator();

		appendValue(buf, "attribute_id", attributeId);
		while (it.hasNext()) {
			Map.Entry entry = (Entry) it.next();
			appendValue(buf, (String)entry.getKey(), (String)entry.getValue());
		}
		buf.append("/>");
	}
	
	private void appendValue(FastStringBuffer buf, String name, String value) {
		if (value != null) {
			buf.append(" ");
			buf.append(name);
			buf.append("=\"");
			buf.append(value);
			buf.append('"');
		}
	}
	
	public int compareTo(AttributeDef o) {
		return attributeId.compareTo(o.attributeId);
	}

	
}
