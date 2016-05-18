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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.util.Util;
import org.openpipeline.util.XMLConfig;

/**
 * Returns an instance of a DocFilter object based on a filename.
 * <p>
 * DocFilterFactory objects should be held in memory and reused by a single thread.
 * They are not multi-threaded. There is an internal cache that *may* hold
 * reusable DocFilter objects. If multiple threads access the getFilter() method, then
 * different threads may get the same DocFilter object at the same time with 
 * unpredictable results. So, allocate one DocFilterFactory per thread.
 */
public class DocFilterFactory {

	// not static, used only by the current thread
	
	private ArrayList<DocFilter> filters; // a list of *all* the filters, even those not used
	private HashMap <String, DocFilter> extensionMap; // maps extensions to filters
	private HashMap <String, DocFilter> mimeTypeMap; // maps mimetypes to filters
	private XMLConfig config;
	private DocFilter defaultFilter; // to be used if nothing else matches


	public DocFilterFactory() {
		init();
	}
	
	/**
	 * Create a factory which is configured by the specified config object.
	 * It's ok if this object is null.
	 * @param config the object that contains the config parameters
	 */
	public DocFilterFactory(XMLConfig config) {
		/*
		 * Config file format:
		 * <stage>
		 *   <id>cb69f4f4-1233-4233-8574-8692030cb18a</id>
		 *   <classname>org.openpipeline.pipeline.stage.DocFilterStage</classname>
		 *   <docfilters>
		 *     <docfilter>
		 *       <classname>org.openpipeline.pipeline.docfilter.PlainTextFilter</classname>
		 *       <enabled>Y</enabled>
		 *       <extensions>txt,log</extensions>
		 *       <mimetypes>text/plain</mimetypes>
		 *     </docfilter>
		 *     <docfilter>
		 *       <name>com.domain.AnotherFilter</name>
		 *       <extensions>*</extensions>
		 *     </docfilter>
		 *   </docfilters>
		 * </stage>
		 */
		
		if (config != null) {
			if (!"docfilters".equals(config.getName())) {
				config = config.getChild("docfilters");
			}
		}
		
		this.config = config; 
		
		init();
	}
	
	
	/**
	 * Sets up the hashmaps, loads the list of filters.
	 */
	private void init() {
		filters = new ArrayList();
		extensionMap = new HashMap();
		mimeTypeMap = new HashMap();
		
		Iterator it = ServiceRegistry.lookupProviders(DocFilter.class);
		while (it.hasNext()) {
			// The iterator returns actual instances of the filters
			DocFilter filter = (DocFilter) it.next();
			filters.add(filter);
		}
		
		// sort them alphabetically by name
		Collections.sort(filters, new DocFilterComparator());
		
		if (config != null) {
			
			for (DocFilter filter: filters) {
				
				// match available filters against the configured list of filters
				// if no params are found for a filter, then it just uses its defaults
				for (XMLConfig child: config.getChildren()) {
					if (filter.getClass().getName().equals(child.getProperty("classname"))) {
						
						if (!child.getBooleanProperty("enabled", true)) {
							// disable it
							filter.setExtensions(new String [0]);
							filter.setMimeTypes(new String [0]);
							break; 
						}
						
						String [] extensions = strToArray(child.getProperty("extensions"));
						filter.setExtensions(extensions);
						
						String [] mimeTypes = strToArray(child.getProperty("mimetypes"));
						filter.setMimeTypes(mimeTypes);
						
						filter.setParams(child);
						break;
					}
				}
			}
		}
		
		// load up the hashmaps
		for (DocFilter filter: filters) {
			String[] extensions = filter.getExtensions();
			for (String ext: extensions) {
				extensionMap.put(ext, filter);
			}
			String[] mimeTypes = filter.getMimeTypes();
			for (String mimeType: mimeTypes) {
				mimeTypeMap.put(mimeType, filter);
			}
		}
		
		// if a default has been defined, set it.
		defaultFilter = extensionMap.get("*");
	}

	/**
	 * Convert a string containing comma and space-separated entries into
	 * an array of strings.
	 * @param str string in the form "entry, entry,entry, etc."
	 * @return trimmed strings
	 */
	private String [] strToArray(String str) {
		if (str == null) {
			return new String [0];
		}
		str = str.toLowerCase();
		String [] parts = str.split("[, ]");
		
		// trim, remove empty entries
		ArrayList list = new ArrayList(parts.length);
		for (String part: parts) {
			part = part.trim();
			if (part.length() > 0) {
				list.add(part);
			}
		}
		
		String [] out = new String[list.size()];
		list.toArray(out);
		return out;
	}
	
	/**
	 * Returns an iterator over all the DocFilters available in the system. Each element
	 * returned is a DocFilter object. 
	 * @return an iterator over the available DocFilters
	 */
	public List<DocFilter> getDocFilters() {
		if (filters == null) {
			return Collections.EMPTY_LIST;
		}
		return filters;
	}
	

	/**
	 * Return an appropriate DocFilter object based on the filename's extension.
	 * @param filename the name of the file to filter.
	 * @return an instance of a DocFilter object, or null if the extension is not found and there is no default filter
	 */
	public DocFilter getFilterByFilename(String filename) {
		DocFilter filter = null;
		if (filename != null) {
			String ext = Util.getExtension(filename);
			if (ext != null) {
				filter = extensionMap.get(ext);
			}
		}
		
		// if there's no filter for this extension, see if there's a default
		if (filter == null) {
			return defaultFilter;
		}
		
		return filter;
	}

	/**
	 * Returns the appropriate DocFilter object based on the filename's mimetype.
	 * @param mimeType the mimetype of the file
	 * @return an instance of a DocFilter object, or null if the mimetype is not found
	 */
	public DocFilter getFilterByMimeType(String mimeType) {
		
		DocFilter filter = null;
		if (mimeType != null) {
			filter = mimeTypeMap.get(mimeType);
		}
		
		// if there's no filter for this mimetype, see if there's a default
		if (filter == null) {
			return defaultFilter;
		}
		
		return filter;
	}
	
	/**
	 * Returns the appropriate DocFilter object for the DocBinary object.
	 * @param docBinary the docBinary to examine
	 * @return an instance of a DocFilter object, or null if no appropriate one found
	 */
	public DocFilter getFilter(DocBinary docBinary) {
		DocFilter filter = null;

		String mimeType = docBinary.getMimeType();
		if (mimeType != null) {
			filter = mimeTypeMap.get(mimeType);
		}
		if (filter == null) {
			String ext = docBinary.getExtension();
			if (ext != null) {
				filter = extensionMap.get(ext);
			}
		}
		
		// if there's no filter available, see if there's a default
		if (filter == null) {
			return defaultFilter;
		}
		return filter;
	}

	private class DocFilterComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			DocFilter df1 = (DocFilter) o1;
			DocFilter df2 = (DocFilter) o2;
			return df1.getDisplayName().compareTo(df2.getDisplayName());
		}
	}
}
