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

import org.openpipeline.server.Server;
import org.openpipeline.util.Util;

/**
 * Code behind server_stats.jsp, which reports server statistics.
 */
public class ServerStatsPage extends AdminPage {


	
	/**
	 * Process a request for the server_properties page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {
		super.processPage(pageContext);
	}

	public String getJavaVersion() {
		return System.getProperty("java.version");
	}
	
	public String getAvailProcessors() {
		return Runtime.getRuntime().availableProcessors() + "";
	}
	
	public String getServerHomeDir() {
		return Server.getServer().getHomeDir();
	}
	
	public String getUptime() {
		return Server.getServer().getUptimeString();
	}
	
	/**
	 * Calls gc() and returns the JVM's memory usage in bytes.
	 */
	public String getMemUsage() {
		long memUsage = java.lang.Runtime.getRuntime().totalMemory() - java.lang.Runtime.getRuntime().freeMemory();
		return Util.getFormattedDataSize(memUsage);
	}

	/**
	 * Returns the maximum memory that will be used by the JVM.
	 */
	public String getMaxMemory() {
		return Util.getFormattedDataSize(java.lang.Runtime.getRuntime().maxMemory());
	}

	/**
	 * Return the current classpath (formatted for display).
	 * @return the classpath
	 */
	public String getClassPath() {
		String classPath = System.getProperty("java.class.path");
		// a class path can get really long and wrap funny. This adds spaces so it wraps better
		classPath = classPath.replaceAll(";", "; ");
		return classPath;
	}

}
