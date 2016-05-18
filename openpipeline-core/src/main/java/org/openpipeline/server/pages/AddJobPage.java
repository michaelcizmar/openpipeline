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

import java.io.File;
import java.util.Iterator;

import javax.servlet.jsp.PageContext;

import org.openpipeline.pipeline.connector.ConnectorFactory;
import org.openpipeline.server.Server;
import org.openpipeline.util.FastStringBuffer;

/**
 * Helper class for the add_job.jsp page in the admin. 
 */
public class AddJobPage extends AdminPage {

	private String redirectPage;
	
	public boolean redirect() {
		return (redirectPage != null);
	}
	
	public String redirectPage() {
		return redirectPage;
	}
	
	public void processPage(PageContext pageContext) {
		super.processPage(pageContext);
		
		// if the user selected a connector, then create a config object and 
		// pass it to the next page
		String pageName = pageContext.getRequest().getParameter("conpage");
		if (pageName != null) {
			String jobName = pageContext.getRequest().getParameter("jobname");
			
			FastStringBuffer buf = new FastStringBuffer();
			buf.append("plugins/");
			buf.appendWithURLEncode(pageName);
			buf.append("?jobname=");
			buf.appendWithURLEncode(jobName);
			redirectPage = buf.toString();
		}
	}

	/**
	 * Get the next incremental number for a connector. Helps create a unique name for it.
	 */
	public String getNextConnectorNum() {
		Server server = Server.getServer();
		String homeDir = server.getHomeDir();
		File jobsDir = new File(homeDir, "config/jobs");

		int maxNum = -1;
		
		File [] jobFiles = jobsDir.listFiles();
		if (jobFiles != null) {
			for (int i = 0; i < jobFiles.length; i++) {
				File jobFile = jobFiles[i];
				
				// a filename takes the form "ConnectorName99.xml", where 99 is 
				// a unique number for each connector
				String filename = jobFile.getName();
				
				// strip off the trailing .xml
				filename = filename.substring(0, filename.length() - 4);
				
				// count backwards until there are no more digits
				int firstDigit = -1;
				for (int j = filename.length() - 1; j >= 0; j--) {
					char ch = filename.charAt(j);
					if (Character.isDigit(ch)) {
						firstDigit = j;
					} else {
						break;
					}
				}
				
				if (firstDigit > -1) {
					String numStr = filename.substring(firstDigit, filename.length());
					int num = Integer.parseInt(numStr);
					maxNum = Math.max(num, maxNum);
				}
			}
		}
		
		return (maxNum + 1) + "";
	}

	/**
	 * Return the list of available connectors
	 */
	public Iterator getConnectors() {
		return ConnectorFactory.getConnectors();
	}
	
}
