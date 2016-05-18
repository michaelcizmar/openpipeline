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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.openpipeline.server.Server;
import org.openpipeline.util.FastStringBuffer;


/**
 * Helper class for the Server Log page in the admin.
 */
public class StatusLogPage extends AdminPage {

	private String logTypeSelect;
	private String logFilenamesSelect;
	private String fetchSizeSelect;
	private String logLines;
	private String defaultLogType;

	public void setDefaultLogType(String defaultLogType) {
		this.defaultLogType = defaultLogType;
	}
	
	/**
	 * Process a request for the status log page and populate various controls that
	 * appear on the page.
	 */
	public void processRequest(HttpServletRequest request) throws FileNotFoundException, IOException {
		
		String logDir = (new File(Server.getServer().getHomeDir(), "logs")).toString();
		
		// create the dropdown that lets the user select which type of log to view
		
		/*
		File [] dirs = new File(logDir).listFiles();
		ArrayList list = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			if (dirs[i].isDirectory()) {
				String dirName = dirs[i].getName();
				list.add(dirName);
			}
		}
		*/
		ArrayList list = new ArrayList();
		getDirectories(new File(logDir), null, list);
		
		String logType = request.getParameter("logtype");
		if (logType == null || logType.length() == 0) {
			logType = defaultLogType;
		}
		logTypeSelect = getSelect(list, logType, "logtype");
		
		
		// get the dropdown that lets a user select a log file
		list.clear();
		String logFileName = request.getParameter("logfilename");
		File [] logFiles = (new File(logDir, logType)).listFiles();
		if (logFiles != null) {
			Arrays.sort(logFiles); // sort alpha
			for (int i = 0; i < logFiles.length; i++) {
				String name = logFiles[i].getName();
				list.add(name);
			}
			if (list.size() > 0 && (logFileName == null || !logFileName.startsWith(logType))) {
				logFileName = (String) list.get(list.size() - 1);
			}
		}
		logFilenamesSelect = getSelect(list, logFileName, "logfilename");
		
		
		// create the dropdown that contains the number of kb to fetch from the log file
		list.clear();
		list.add("100");
		list.add("1000");
		list.add("10000");
		String fetchSize = request.getParameter("fetchsize");
		if (fetchSize == null) {
			fetchSize = "100";
		}
		fetchSizeSelect = getSelect(list, fetchSize, "fetchsize");
		
		// get the data from the log to display
		if (logFileName == null) {
			logLines = "";
		} else {
			File currentLogFile = new File(new File(logDir, logType), logFileName);
			logLines = fetchLogLines(currentLogFile, fetchSize);
		}
		
	}
	
	/**
	 * Recursively get the directories starting at the root.
	 */
	private void getDirectories(File root, String parentName, ArrayList list) {
		if (root.isDirectory()) {
			
			File [] files = root.listFiles();
			
			boolean hasSubdirs = false;
			for (File file: files) {
				if (file.isDirectory()) {
					String name = file.getName();
					if (parentName != null) {
						name = parentName + "/" + name;
					}
					getDirectories(file, name, list);
					hasSubdirs = true;
				}
			}
			if (!hasSubdirs) {
				list.add(parentName);
			}
		}
	}
	

	/**
	 * Get a select dropdown containing the specified options.
	 */
	private String getSelect(ArrayList options, String selectedOption, String name) {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<select name=\"" + name + "\" onchange=\"document.logform.submit()\">");
		
		for (int i = 0; i < options.size(); i++) {
			String option = (String) options.get(i);
			
			buf.append("<option");
			if (option.equals(selectedOption)) {
				buf.append(" selected");
			}
			buf.append(">");
			buf.append(option);
		}
		buf.append("</select>");
		return buf.toString();
	}

	
	/**
	 * Return a block of text at the end of the specified log file.
	 * @param logFile the log file
	 * @param fetchSizeStr the number of kilobytes to return
	 */
	private String fetchLogLines(File logFile, String fetchSizeStr) throws FileNotFoundException, IOException {
		
		if (!logFile.exists()) {
			return "";
		}
		
		int fetchSize = Integer.parseInt(fetchSizeStr);
		
		long fileLen = logFile.length();
		int bufCapacity = (int)((1024 * fetchSize) * 1.1f); // extra 10% for xml encoding
		FastStringBuffer buf = new FastStringBuffer(bufCapacity);
		long start = fileLen - bufCapacity;
		if (start < 0)
			start = 0;

		FileInputStream fis = new FileInputStream(logFile);
		fis.skip(start);
		
		InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
		int size = reader.read(buf.getArray());
		fis.close();
		reader.close();

		if (size == -1) {
			return "";
		} else {
			buf.setSize(size);
			buf.xmlEncode(0, buf.size());
			return buf.toString();
		}
	}
	
	
	public String getLogTypeSelect() {
		return logTypeSelect;
	}
	
	public String getLogFilenamesSelect() {
		return logFilenamesSelect;
	}
	
	public String getFetchSizeSelect() {
		return fetchSizeSelect;
	}
	
	public String getLogLines() {
		return logLines;
	}
}
