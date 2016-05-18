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
package org.openpipeline.pipeline.connector;

import java.io.File;
import java.util.List;

import org.openpipeline.pipeline.connector.filesystem.DiskFileSystem;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueueFactory;
import org.openpipeline.server.Server;
import org.openpipeline.util.Util;
import org.openpipeline.util.WildcardMatcher;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * A connector that scans a file system and processes the files it finds.
 */
public class FileScanner extends Connector {
	
	public static final long MAX_FILE_SIZE_DEFAULT = 100; // 100mb

	private Logger logger;
	private String linkQueueName;
	private GenericScanner scanner;

	// parameters
	private List fileRoots;
	private List includes;
	private List excludes;
	private boolean scanSubDirs;
	private boolean scanCompressedFiles;
	private int docLoggingCount = 1;
	private long maxFileSize;

	public void execute() {
		try {

			logger = super.getLogger();
			logger.info("Starting " + super.getJobName() + "...");
			super.setLastMessage("Running");

			if (super.getParams() == null) {
				String msg = "setParams() was not called for FileScanner.";
				throw new IllegalStateException(msg);
			}
			extractParams();

			boolean debug = Server.getServer().getDebug();

			WildcardMatcher wildcardMatcher = new WildcardMatcher();
			wildcardMatcher.setIncludePatterns(includes);
			wildcardMatcher.setExcludePatterns(excludes);

			LinkQueue linkQueue = LinkQueueFactory.getLinkQueueByName(linkQueueName);
			if (linkQueue != null) {
				linkQueue.setParams(super.getParams()); // inits the queue, could throw error
			}

			if (fileRoots.size() == 0) {
				String msg = "No files or directories defined";
				throw new IllegalStateException(msg);
			}

			scanner = new GenericScanner();
			scanner.setParentConnector(this);
			scanner.setStartOfCrawl(System.currentTimeMillis());
			scanner.setDebug(debug);
			scanner.setDocLoggingCount(docLoggingCount);
			scanner.setLinkQueue(linkQueue);
			scanner.setLogger(logger);
			scanner.setStageList(super.getStageList());
			scanner.setScanSubDirs(scanSubDirs);
			scanner.setScanCompressedFiles(scanCompressedFiles);
			scanner.setWildcardMatcher(wildcardMatcher);
			scanner.setMaxFileSize(maxFileSize);

			// start scanning here
			for (int i = 0; i < fileRoots.size(); i++) {
				String filename = (String) fileRoots.get(i);
				File file = new File(filename);

				if (file.exists()) {
					DiskFileSystem diskFile = new DiskFileSystem(file);
					scanner.scan(diskFile);
				} else {
					logger.warn("File or directory does not exist:" + file.toString());
				}
			}

			// the crawl is complete. Now roll through the linkqueue and find
			// deleted items
			scanner.lookForDeletes();

			super.setLastMessage("Ended");

		} catch (Exception t) {
			super.error("Error executing FileScanner", t);
			super.setLastMessage("Error: " + t.toString());
		}
		
		int docsProcessed = 0;
		String elapsed = "";
		if (scanner != null) {
			docsProcessed = scanner.getDocsProcessed();
			elapsed = Util.getFormattedElapsedTime(scanner.getElapsed());
		}
		
		logger.info("FileScanner ended. Docs scanned: " + docsProcessed + " Elapsed time: " + elapsed);
	}

	public void interrupt() {
		super.interrupt();
		if (scanner != null) {
			scanner.interrupt();
		}
	}


	public void extractParams() {
		XMLConfig params = super.getParams();
		scanSubDirs = params.getBooleanProperty("subdirs", true);
		scanCompressedFiles = params.getBooleanProperty("compressed-files", true);
		fileRoots = params.getValues("fileroots");
		includes = params.getValues("include-patterns");
		excludes = params.getValues("exclude-patterns");
		docLoggingCount = params.getIntProperty("doc-logging-count", 1);
		linkQueueName = params.getProperty("linkqueue-name");
		maxFileSize =  params.getLongProperty("max-file-size", MAX_FILE_SIZE_DEFAULT);
	}

	public String getLastMessage() {
		String msg = super.getLastMessage();
		if (scanner != null) {
			int docsProcessed = scanner.getDocsProcessed();
			if (docsProcessed > 0) {
				msg += "/docs processed=" + docsProcessed;
			}
		}
		return msg;
	}

	public String getDisplayName() {
		return "File Scanner";
	}

	public String getDescription() {
		return "Scans a file system and processes files";
	}

	public String getPageName() {
		return "connector_file_scanner.jsp";
	}

	public String getShortName() {
		return "FileScanner";
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}


}
