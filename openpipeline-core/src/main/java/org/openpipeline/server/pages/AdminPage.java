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
package org.openpipeline.server.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.jsp.PageContext;

import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Helper class for processing pages in the Admin UI. Most pages in the Admin have their
 * own helper class that extends this one. Manages populating controls on the form,
 * the page header, messages, and error handling.
 * <p>
 * This class is in openpipeline-core because Stage depends on it. Eventually, this class
 * will be replaced with something else.
 */
public class AdminPage {

	private static final String SEP = System.getProperty("line.separator");
	
	// paramMap is a modifiable version of the parameter map stored in 
	// an HttpServletRequest. keys are String and values are String[].
	private Map <String, String[]>paramMap = new HashMap();
	
	private ArrayList messages = new ArrayList();

	/**
	 * Populate the parameter map with existing params. Useful for prepopulating
	 * the controls on a page.
	 * @param map a map of name/value pairs
	 */
	public void populateParams(Map map) {
		if (map != null)
			paramMap.putAll(map);
	}

	public Map<String, String[]> getParamMap() {
		return paramMap;
	}

	/**
	 * Initialize the page
	 * @param pageContext the pageContext variable from the jsp page
	 */
	public void processPage(PageContext pageContext) {
		paramMap.putAll(pageContext.getRequest().getParameterMap());
	}


	/**
	 * Add a checkbox to the form. Also adds a hidden field, cbname, with a 
	 * value equal to the checkbox name. The purpose of the hidden field is to handle
	 * cases where the user unchecks the box. If there is no check, then it's
	 * as if the form doesn't contain this field at all, and no parameter is sent.
	 * The hidden field just lets the processXXXPage() method know
	 * that the field exists.
	 */
	public String checkbox(String name, boolean defaultValue) {
		return checkbox(name, defaultValue, true);
	}

	/**
	 * Same as checkbox(name, defaultValue), includes an option to omit
	 * the cbname field.
	 */
	public String checkbox(String name, boolean defaultValue, boolean includeCbName) {

		boolean checked = getBooleanParam(name, defaultValue);
		StringBuffer buf = new StringBuffer();
		buf.append("<input type=checkbox value='Y' name=\"");
		buf.append(name);
		buf.append('"');
		if (checked)
			buf.append(" checked");
		buf.append(">");

		if (includeCbName) {
			buf.append("<input type=hidden name=cbname value=\"");
			buf.append(name);
			buf.append("\">");
		}
		return buf.toString();
	}
	
	/**
	 * Add a radio button to the form.
	 * @param name name of the button. If other buttons have the same name, they 
	 * will be part of the same radio group
	 * @param value the value the name will have when this control is checked 
	 * @param defaultChecked set this true if this button should be checked by default,
	 * when the name does not have a value
	 * @return the html for the radio button
	 */
	public String radioButton(String name, String value, boolean defaultChecked) {
		
		String currValue = getParam(name, null);

		// check this radio button if it matches the current value,
		// or if there is no value and it's the default
		boolean checked = false;
		if (currValue == null) {
			if (defaultChecked) {
				checked = true;
			}
		} else {
			if (currValue.equals(value))
				checked = true;
		}

		StringBuffer buf = new StringBuffer();
		buf.append("<input type='radio' name='");
		buf.append(name);
		buf.append("'");
		buf.append(" id='");
		buf.append(value);
		buf.append("'");
		buf.append(" value='");
		buf.append(value);
		buf.append("'");
		if (checked)
			buf.append(" checked");
		buf.append(">");
		return buf.toString();
	}

	/**
	 * Add a text field
	 */
	public String textField(String name) {
		return textField(name, -1);
	}

	/**
	 * Add a sized text field.
	 */
	public String textField(String name, int size) {
		return textField(name, size, -1);
	}
	
	/**
	 * Add a sized text field with a maximum number of allowed characters.
	 */
	public String textField(String name, int size, int maxLength) {
		return textField(name, size, maxLength, "");
	}

	public String textField(String name, int size, int maxLength, String defaultVal) {
	
		String value = getParam(name, defaultVal);

		StringBuilder buf = new StringBuilder();
		buf.append("<input type=\"text\" name=\"");
		buf.append(name);

		if (value.startsWith("\""))
			buf.append("\" value='" + value + "'");
		else
			buf.append("\" value=\"" + value + "\"");

		if (size > 0) {
			buf.append(" size=\"" + size + "\"");
		}
		
		if (maxLength > -1) {
			buf.append(" maxlength=\"" + maxLength + "\"");
		}
		
		buf.append('>');
		return buf.toString();
	}
	
	
	

	/**
	 * Add a password field
	 */
	public String passwordField(String name) {
		String value = getParam(name, null);
		if (value == null) {
			value = "";
		}

		StringBuilder buf = new StringBuilder();
		buf.append("<input type=password name=\"");
		buf.append(name);
		buf.append("\" value=\"" + value + "\">");
		return buf.toString();
	}

	/**
	 * Convenience method for <code>selectField(name, options, null);</code>
	 * @param name name of the field
	 * @param options visible options
	 * @return a select field suitable for inclusion in html
	 */
	public String selectField(String name, String options) {
		return selectField(name, options, null);
	}

	/**
	 * Add a select dropdown to the form. Call it like this:
	 * <p>
	 * <code>
	 * selectField("myname", "None,Red,Blue", ",red,blue"); 
	 * </code>
	 * </p>
	 * 
	 * optionValues can be null, but if it's not, it must have the same number of
	 * elements as options.
	 * 
	 * @param name name of the field
	 * @param options visible options, must not be null
	 * @param optionValues hidden values for each option, can be null
	 * @return a select field suitable for inclusion in html
	 */
	public String selectField(String name, String options, String optionValues) {
	
		if (options == null) {
			throw new IllegalArgumentException("options must not be null");
		}
		
		String [] optionArr = options.split(",");
		String [] optionValueArr = null;
		if (optionValues != null) {
			optionValueArr = optionValues.split(",");
			if (optionValueArr.length != optionArr.length)
				throw new IllegalArgumentException("options lists do not contain the same number of elements");
		}
		
		String currValue = getParam(name, null);
		
		StringBuilder buf = new StringBuilder();
		buf.append("<select name=\"");
		buf.append(name);
		buf.append("\">");

		for (int i = 0; i < optionArr.length; i++) {
			buf.append("<option");
			
			if (optionValueArr == null) {
				if (optionArr[i].equals(currValue)) {
					buf.append(" selected");
				}

			} else {
				if (optionValueArr[i].equals(currValue)) {
					buf.append(" selected");
				}
				
				buf.append(" value=\"");
				buf.append(optionValueArr[i]);
				buf.append("\"");
			}
			
			buf.append(">");

			buf.append(optionArr[i]);
		}
		buf.append("</select>");
		return buf.toString();
	}

	/**
	 * Add a text area control to the form. 
	 * @param name name of param
	 * @param cols columns in the control
	 * @param rows rows in the control
	 * @param wrap set true if word wrap should be on
	 * @return a textarea input field
	 */
	public String textArea(String name, String cols, String rows, boolean wrap) {

		StringBuilder buf = new StringBuilder();
		buf.append("<textarea name='");
		buf.append(name);
		buf.append("' cols='");
		buf.append(cols);
		buf.append("' rows='");
		buf.append(rows);
		buf.append("'");
		if (!wrap) {
			buf.append(" wrap='off'");
		}
		buf.append(">");
		
		if (name.endsWith("_SPLIT")) {
			name = name.substring(0, name.indexOf("_SPLIT"));
		}

		// concatenate all the values together with line separators
		String [] vals = getParams(name);
		if (vals != null) {
			for (int i = 0; i < vals.length; i++) {
				if (i > 0) {
					buf.append(SEP);
				}
				buf.append(vals[i]);
			}
		}
		
		buf.append("</textarea>");
		
		return buf.toString();
	}
	

	/**
	 * Get a string parameter.
	 */
	public String getParam(String name, String defaultVal) {
		
		Object obj = paramMap.get(name);
		if (obj == null)
			return defaultVal;
		
		if (obj instanceof String) {
			return (String) obj;
			
		} else if (obj instanceof String []) {
			String [] vals = (String[]) obj;
			if (vals.length == 0) {
				return defaultVal;
			} else {
				String val = vals[0];
				if (val == null)
					return defaultVal;
				else
					return val;
			}
			
		} else {
			return obj.toString(); // this probably means an error
		}
	}

	/**
	 * Get a string parameter
	 */
	public String getParam(String name) {
		return getParam(name, null);
	}

	/**
	 * Return an array of values for the name
	 */
	public String[] getParams(String name) {
		return paramMap.get(name);
	}

	/**
	 * Get a boolean parameter out of the request.
	 */
	public boolean getBooleanParam(String name, boolean defaultVal) {
		String value = getParam(name);
		if (value == null || value.length() == 0)
			return defaultVal;

		if (value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("T") || value.equalsIgnoreCase("yes")
				|| value.equalsIgnoreCase("true"))
			return true;
		else
			return false;
	}

	/* Rules for converting params to XMLConfig and back:
	 * - Params in the request object always get copied to paramMap.
	 * - Elements in param map are always String name, String [] values.
	 * - We do not deal with hierarchical values.
	 * - If an incoming parameter name ends with "_SPLIT", then we split
	 *   it on the line breaks and add it as multiple values in the XML
	 * - If an XMLConfig has multiple values with the same name, they
	 *   get concatenated with line breaks.
	 * - If a page that extends AdminPage needs to do any preprocessing on
	 *   params before they get written to disk, the procedure is first to
	 *   convert the map to an XMLConfig, then convert back and forth using
	 *   methods named:
	 *   convertToParamFormat() & convertToXMLFormat();
	 */
	
	
	
	/**
	 * Convert the parameters to an XMLConfig object.
	 * @param conf 
	 */
	static public void convertParamsToXMLConfig(Map paramMap, XMLConfig conf) {
		
		/* If a checkbox is unchecked, then it won't come through in 
		 * the parameters. We add a multi-valued hidden field to the
		 * form, cbname, that has the names of all the checkboxes. The 
		 * names that are missing from the request get added back
		 * with a value of "N". 
		 */
		String[] checkboxNames = (String [])paramMap.get("cbname");
        if (checkboxNames != null) {
            for (int i = 0; i < checkboxNames.length; i++) {
                if (!paramMap.containsKey(checkboxNames[i])) {
                    paramMap.put(checkboxNames[i], new String[] {"N"});
                }
            }
        }
        paramMap.remove("cbname");
		
		Iterator it = paramMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Entry) it.next();
			String name = (String) entry.getKey();
			
			boolean mustSplit = false;
			if (name.endsWith("_SPLIT")) {
				name = name.substring(0, name.indexOf("_SPLIT"));
				mustSplit = true;
			}
			conf.removeChild(name); // replace any existing values
			
			String[] values = (String[]) entry.getValue();
			if (values != null) {
				if (mustSplit) {
					for (int i = 0; i < values.length; i++) {
						String val = values[i];
						if (val != null && val.length() > 0) {
							String [] vals = val.split("[\r\n]");
							addValues(name, vals, conf);
						}
					}
					
				} else {
					addValues(name, values, conf);
				}
			}
		}
	}
	
	static private void addValues(String name, String [] values, XMLConfig conf) {
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				String val = values[i];
				if (val != null && val.length() > 0) {
					conf.addProperty(name, val);
				}
			}
		}
	}
	
	
	/**
	 * Convert the XMLConfig object to parameters and add them to the existing parameters.
	 */
	static public void convertXMLConfigToParams(XMLConfig conf, Map paramMap) {
		
		for (XMLConfig child: conf.getChildren()) {

			String name = child.getName();
			String value = child.getValue();
			
			String [] vals = (String[]) paramMap.get(name);
			if (vals == null) {
				vals = new String[1];
				vals[0] = value;
				
			} else {
				// concatenate with existing val
				//vals[0] = vals[0] + SEP + value;
				
				String [] newVals = new String [vals.length + 1];
				System.arraycopy(vals, 0, newVals, 0, vals.length);
				newVals[vals.length] = value;
				vals = newVals;
			}
			paramMap.put(name, vals);
		}
	}

	public List getMessages() {
		return messages;
	}

	public void addMessage(String msg) {
		messages.add(msg);
	}
	
	public void addMessages(List list) {
		messages.addAll(list);
	}

	public void handleError(String message, Throwable t) {
		addMessage(message);

		// errors should always be logged in the main server log
		Logger logger = Server.getServer().getLogger();
		logger.error(message, t);
	}
	
	/**
	 * Set the value of a param.
	 * @param name name of the param
	 * @param value the value of the param
	 */
	public void setParam(String name, String value) {
		// values are String arrays
		String [] vals = new String[1];
		vals[0] = value;
		paramMap.put(name, vals);
	}

	
}
