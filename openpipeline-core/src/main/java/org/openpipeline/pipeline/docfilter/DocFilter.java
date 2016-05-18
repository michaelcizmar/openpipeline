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

import java.util.Collections;
import java.util.List;

import org.openpipeline.pipeline.stage.Stage;

/**
 * An abstract class that all document filters must extend. A document filter is a
 * class that transforms a binary document into a text-based item.
 * <p>
 * Important: a DocFilter needs to be a lightweight object, quickly instantiated, with a 
 * zero-argument constructor. Any heavy initialization should be placed in the 
 * initialize() method, immediately before parsing is started. 
 * <p>
 * DocFilter extends Stage. The system will autodiscover it like other stages.
 * DocFilters are not expected to be thread-safe.
 */
public abstract class DocFilter extends Stage {
	
	protected String[] exts = getDefaultExtensions();
	protected String[] mimeTypes = getDefaultMimeTypes();
	
	/**
	 * Returns a List of any links found in the document. In HTML, for example,
	 * links are found in &lt;a href=""> tags. The ArrayList contains Link objects.
	 * @return a list of links. If none, returns an empty list.
	 */
	public List getLinks() {
		return Collections.EMPTY_LIST;
	}

	/**
	 * Return an array of file extensions that this filter can handle.
	 * The extensions should be lower case and omit the dot, for example,
	 * <p>
	 * <code>
	 * {"htm", "html", "jsp", "asp"}
	 * </code>
	 * @return a list of extensions
	 */
	public String[] getExtensions() {
		return exts;
	}

	/**
	 * Set the extensions that this filter handles. This value doesn't
	 * usually change the behavior of this class; it's just stored
	 * here and used for display purposes.
	 * @param exts extensions this class should handle
	 */
	public void setExtensions(String [] exts) {
		this.exts = exts;
	}
	
	
	/**
	 * Return an array of mimetypes that this filter can handle. For example,
	 * <p>
	 * <code>
	 * {"text/html", "text/plain"}
	 * </code>
	 * <p>
	 * Other common mimetypes include application/pdf, application/msword,
	 * application/vnd.ms-excel, etc.
	 * 
	 * @return a list of mimetypes
	 */
	public String[] getMimeTypes() {
		return mimeTypes;
	}

	/**
	 * Set the mimetypes that this filter handles. This value doesn't
	 * usually change the behavior of this class; it's just stored
	 * here and used for display purposes.
	 * @param mimeTypes mimetypes this class should handle
	 */
	public void setMimeTypes(String [] mimeTypes) {
		this.mimeTypes = mimeTypes;
	}

	@Override
	public String getConfigPage() {
		return "docfilter_default.jsp";
	}
	
	
	/**
	 * Return the default extensions that this filter handles. This
	 * list is normally hard-coded in implementing classes, but
	 * can be modified by calling setParams() with new values.
	 * @return a list of extensions, for example, {"html", "htm"}
	 */
	public abstract String [] getDefaultExtensions();
	
	/**
	 * Return the default mimeTypes that this filter handles. This
	 * list is normally hard-coded in implementing classes, but
	 * can be modified by calling setParams() with new values.
	 * @return a list of extensions, for example, { "text/html" }
	 */
	public abstract String [] getDefaultMimeTypes();
	

	/**
	 * Return a String that describes the type of document.
	 * For example, "txt" for a text document, "pdf" for a PDF,
	 * etc.. 
	 * @return a String describing the document type
	 */
	public abstract String getDocType();

	
}
