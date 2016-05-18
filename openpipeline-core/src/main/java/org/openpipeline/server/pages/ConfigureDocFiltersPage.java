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

import java.io.IOException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.openpipeline.pipeline.docfilter.DocFilter;
import org.openpipeline.pipeline.docfilter.DocFilterFactory;
import org.openpipeline.pipeline.stage.StageUtil;
import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.XMLConfig;
import org.quartz.SchedulerException;

public class ConfigureDocFiltersPage extends AdminPage {
	
	
	
	
	
	
	
	
	
	private String name;
	private String desc;
	private String extensions;
	private String mimetypes;
	private DocFilterFactory factory;
	private int currFilter = -1;
	private List<DocFilter> filters;
	private boolean redirect;
	private String jobName;
	
	/**
	 * Process a request for the server_properties page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {
		super.processPage(pageContext);

		try {

			jobName = pageContext.getRequest().getParameter("jobname");
			XMLConfig jobXML = StageUtil.getJobFromRequest(pageContext.getRequest());
			XMLConfig filterConfig = jobXML.getChild("docfilters");
			
			factory = new DocFilterFactory(filterConfig);
			filters = factory.getDocFilters();
			
			String next = super.getParam("next");
			if (next != null && next.length() > 0) {
				
				// if the user hit "next>>", save and redirect.
				save(pageContext.getRequest(), jobXML);
				redirect = true;
			}

		} catch (Throwable t) {
			super.handleError("Error on configure docfilters page", t);
		}
	}

	
	private void save(ServletRequest request, XMLConfig jobXML) throws IOException, SchedulerException, ParseException {
		
		// the file format for the docfilters section is documented
		// in the DocFilterFactory class
		
		XMLConfig newNode = new XMLConfig();
		newNode.setName("docfilters");
		
		Enumeration e = request.getParameterNames();
		while (e.hasMoreElements()) {
			String paramName = (String) e.nextElement();
			if (paramName.startsWith("ext_")) {
				String filterName = paramName.substring("ext_".length());
				String extStr = request.getParameter(paramName);
				String mimeStr = request.getParameter("mime_" + filterName);
				
				XMLConfig filtNode = newNode.addChild("docfilter");
				filtNode.addProperty("name", trim(filterName));
				filtNode.addProperty("extensions", trim(extStr));
				filtNode.addProperty("mimetypes", trim(mimeStr));
			}
		}
		
		jobXML.removeChild("docfilters");
		jobXML.addChild(newNode);

		JobScanner jobScanner = new JobScanner();
		jobScanner.saveAndLoadJob(jobXML);
	}

	private String trim(String str) {
		if (str == null) {
			return "";
		}
		return str.trim();
	}
	
	
	public boolean redirect() {
		return redirect;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public void beforeFirst() {
		currFilter = -1;
	}
	
	public boolean next() {
		currFilter++;
		if (currFilter >= filters.size()) {
			return false;
		}

		DocFilter filt = filters.get(currFilter);
		name = filt.getDisplayName();
		desc = filt.getDescription();
		extensions = arrayToString(filt.getExtensions()); 
		mimetypes =  arrayToString(filt.getMimeTypes());
		
		return true;
	}
	
	private String arrayToString(String[] arr) {
		if (arr == null)
			return "";
		FastStringBuffer buf = new FastStringBuffer();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				buf.append(",");
			}
			buf.append(arr[i]);
		}
		return buf.toString();
	}

	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public String getExtensions() {
		return extensions;
	}
	
	public String getMimeTypes() {
		return mimetypes;
	}
	


}
