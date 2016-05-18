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
package org.openpipeline.pipeline.stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openpipeline.pipeline.docfilter.DocFilter;
import org.openpipeline.pipeline.docfilter.DocFilterFactory;
import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.StandardAttributeNames;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Stage that wraps the available DocFilters and applies them to items.
 * A DocFilter is a class that converts a binary document (like PDF)
 * to plain text and metadata. This stage intercepts binary documents
 * that have been attached to an item, converts them to text, 
 * and pushes items with the text down the pipeline.
 */
public class DocFilterStage extends Stage {
	
	public static final String PARAM_PREFIX = "dfparam"; // all docfilter params on the config page start with this
	
	private DocFilterFactory docFilterFactory;
	private Logger logger;
	private boolean debug;
	private boolean addMetadata = true;
	
	@Override
	public void initialize() throws PipelineException {
		logger = Server.getServer().getLogger();
		debug = Server.getServer().getDebug();
		if (params != null) {
			addMetadata = params.getBooleanProperty("add-metadata", true);
		}

		docFilterFactory = new DocFilterFactory(params);

		// each filter needs to push its item down the pipeline
		for (DocFilter filter: docFilterFactory.getDocFilters()) {
			filter.setNextStage(nextStage);
		}
	}

	@Override
	public void processItem(Item item) throws PipelineException {

		if (item.getAction() == Item.ACTION_DELETE) {
			super.pushItemDownPipeline(item);
			return;
		}
		
		DocBinary docBinary = item.getDocBinary();
		if (docBinary == null) {
			// if it's null, then just do the next stage. if not,
			// then the filter is responsible for passing the doc down
			// the pipeline to the next stage
			super.pushItemDownPipeline(item);
			return;
		}
		
		DocFilter filter = docFilterFactory.getFilter(docBinary);
		if (filter == null) {
			super.warn("No DocFilter found for: " + docBinary.getName());

		} else {

			try {
				
				if (addMetadata) {

					String url = docBinary.getName();
					long lastUpdate = docBinary.getTimestamp();
					long fileSize = docBinary.getSize();

					item.getRootNode().addNode(StandardAttributeNames.DOCTYPE,
							filter.getDocType());
					item.getRootNode().addNode(StandardAttributeNames.URL, url);
					item.getRootNode()
							.addNode(StandardAttributeNames.LAST_UPDATE,
									lastUpdate + "");
					item.getRootNode().addNode(
							StandardAttributeNames.FILE_SIZE, fileSize + "");
				}
				
				// IMPORTANT: the filter is responsible for passing the item
				// on to the next stage.
				filter.processItem(item);

				if (debug) {
					logger.debug("Doc: " + docBinary.getName()
							+ " filtered by: " + filter.getDisplayName());
				}

			} catch (Throwable t) {
				String msg = "Error parsing " + docBinary.getName() + ": "
						+ t.getMessage();
				if (debug) {
					logger.error(msg, t);
				} else {
					logger.error(msg);
				}
				throw new PipelineException(t);
			}
		}
		
		if (docBinary.getInputStream() != null) {
			try {
				docBinary.getInputStream().close();
			} catch (IOException e) {
				logger.error("Could not close " + docBinary.getName(), e);
			}
		}		
		
	}

	@Override
	public String getDescription() {
		return "Converts binary documents that have been attached to an Item to text and metadata.";
	}

	@Override
	public String getDisplayName() {
		return "DocFilter Stage";
	}

	@Override
	public String getConfigPage() {
		return "stage_docfilters.jsp";
	}

	@Override
	public void loadParamsFromXML(Map<String, String[]> params, XMLConfig stageRoot) {
		
		/*
		the naming convention for parameters is:
		DisplayName.paramName
		for example:
		PlainTextFilter.mimetype
		See DocFilterFactory for the xml file format.
		*/
		
		XMLConfig docFiltersRoot = stageRoot.getChild("docfilters");
		if (docFiltersRoot == null) {
			// if there's nothing in the conf, then load the defaults
			DocFilterFactory factory = new DocFilterFactory();
			List<DocFilter> list = factory.getDocFilters();
			for (DocFilter docFilter: list) {
				String name = PARAM_PREFIX + "." + docFilter.getClass().getName();
				
				String mimetypes = commaSeparate(docFilter.getDefaultMimeTypes());
				params.put(name + ".mimetypes", toStrArray(mimetypes));
				
				String exts = commaSeparate(docFilter.getDefaultExtensions());
				params.put(name + ".extensions", toStrArray(exts));
			}
			return;
		}
		
		// a filterConf is a <docfilter> section
		List<XMLConfig> filterConfs = docFiltersRoot.getChildren();
		for (XMLConfig filterConf: filterConfs) {
			String filterName = filterConf.getProperty("classname");
			
			List<XMLConfig> confParams = filterConf.getChildren();
			for (XMLConfig confParam: confParams) {
				String paramName = confParam.getName();
				String paramValue = confParam.getValue();
				if (paramName.equals("classname")) {
					continue;
				}
				params.put(PARAM_PREFIX + "." + filterName + "." + paramName, toStrArray(paramValue));
			}
		}
	}

	/**
	 * Create String array and assign the string to the first element.
	 */
	private String[] toStrArray(String str) {
		String [] arr = new String[1];
		arr[0] = str;
		return arr;
	}

	/**
	 * Convert an array of strings into a comma-separated list.
	 */
	private String commaSeparate(String[] strs) {
		FastStringBuffer buf = new FastStringBuffer();
		for (String str: strs) {
			if (buf.size() > 0) {
				buf.append(',');
			}
			buf.append(str);
		}
		return buf.toString();
	}

	@SuppressWarnings("null")
	@Override
	public void saveParamsToXML(Map<String, String[]> params, XMLConfig stageRoot) {
		
		XMLConfig rawConf = new XMLConfig();
		super.saveParamsToXML(params, rawConf); // handles checkboxes, among other things
		
		// now transform the raw config into a better-organized one
		// rawConf should contain add-metadata plus all the docfilt params

		// remove docfilt params from the conf to a separate list 
		ArrayList<XMLConfig> docFiltParams = new ArrayList();
		List<XMLConfig> rawChildren = rawConf.getChildren();
		for (int i = rawChildren.size() - 1; i >= 0; i--) {
			XMLConfig rawChild = rawChildren.get(i);
			if (rawChild.getName().startsWith(PARAM_PREFIX)) {
				docFiltParams.add(rawChild);
				rawChildren.remove(i);
			}
		}
		
		// sorts on name
		Collections.sort(docFiltParams);

		XMLConfig docFiltersRoot = rawConf.addChild("docfilters");
		XMLConfig currFilter = null;
		
		Iterator<XMLConfig> it = docFiltParams.iterator();
		String prevFilterName = null;
		
		while (it.hasNext()) {
			XMLConfig entry = it.next();
			String name = entry.getName();
			String value = entry.getValue();
			
			int firstDot = name.indexOf('.');
			int lastDot = name.lastIndexOf('.');
			
			String filterName = name.substring(firstDot + 1, lastDot);
			String paramName = name.substring(lastDot + 1);
			
			if (!filterName.equals(prevFilterName)) {
				currFilter = docFiltersRoot.addChild("docfilter");
				currFilter.addProperty("classname", filterName);
				prevFilterName = filterName;
			}
			
			currFilter.addProperty(paramName, value);
		}
		
		stageRoot.merge(rawConf);
	}

	
	
}
