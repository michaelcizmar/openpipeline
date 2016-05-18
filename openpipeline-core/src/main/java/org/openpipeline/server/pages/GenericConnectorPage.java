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

import javax.servlet.jsp.PageContext;

import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.util.XMLConfig;

/**
 * Helper class for the connector configuration pages. 
 */
public class GenericConnectorPage extends AdminPage {

	private boolean redirect = false;
	private String jobName;

	public boolean redirect() {
		return redirect;
	}

	/**
	 * Process a request for the connector config page.
	 */
	public void processPage(PageContext pageContext, String className) {

		super.processPage(pageContext);

		try {

			JobScanner jobScanner = new JobScanner();
			jobName = pageContext.getRequest().getParameter("jobname");
			XMLConfig conf = jobScanner.getJobFromDisk(jobName);
			
			String next = super.getParam("next");
			if (next == null || next.length() == 0) {

				// hitting page for first time. populate the page variables
				super.convertXMLConfigToParams(conf, super.getParamMap());
				
			} else {
				// if the user hit "next>>", save and redirect.
				
				super.getParamMap().remove("next");

				super.convertParamsToXMLConfig(super.getParamMap(), conf);
				
				// just in case these haven't already been set
				conf.setName("job");
				conf.setProperty("jobname", jobName);
				conf.setProperty("jobclass", className);

				try {
					jobScanner.saveAndLoadJob(conf);
				} catch (Exception e) {
					super.handleError("Error committing job", e);
				}

				redirect = true;
			}

		} catch (Exception e) {
			super.handleError("Error in connector page", e);
		}
	}
	

	public String getJobName() {
		return jobName;
	}

}
