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
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.openpipeline.pipeline.connector.filesystem.FileIterator;
import org.openpipeline.pipeline.connector.filesystem.FileSystem;
import org.openpipeline.pipeline.connector.filesystem.ZipFileSystem;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.stage.StageList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.WildcardMatcher;
import org.slf4j.Logger;

/**
 * This class crawls any data source that implements the FileSystem interface.
 * It's a helper class that gets embedded in Connector classes.
 */
public class GenericScanner {

	private WildcardMatcher wildcardMatcher;
	private boolean scanSubDirs;
	private boolean scanCompressedFiles;
	private Item item = new Item();
	private boolean debug;
	private int docsProcessed;
	private int docLoggingCount;
	private LinkQueue linkQueue;
	private long startOfCrawl;
	private long elapsed;
	private FileSystem root;
	private StageList stageList;
	private long maxFileSizeInBytes = Long.MAX_VALUE;
	private Connector connector;
	private Logger logger;
	private volatile boolean interrupted;

	/**
	 * Set the timestamp, in millis, when this crawl started.
	 * 
	 * @param startOfCrawl
	 *            usually set to System.currentTimeMillis()
	 */
	public void setStartOfCrawl(long startOfCrawl) {
		this.startOfCrawl = startOfCrawl;
	}

	/**
	 * Scan the file system, looking for files to process.
	 * <p>
	 * Exception handling: DocFilters trap exceptions internally. If there is an
	 * error parsing a document, it just gets logged and the connector
	 * continues. Any other exception should probably abort the connector.
	 */
	public void scan(FileSystem file) throws Exception {
		if (startOfCrawl == 0) {
			throw new IllegalStateException("startOfCrawl has not been set");
		}

		root = file;
		try {
			scanInternal(file);
		} finally {
			elapsed = System.currentTimeMillis() - startOfCrawl;
		}
	}

	/**
	 * Internal, recursively-called scan function.
	 */
	private void scanInternal(FileSystem file) throws Exception {
		if (interrupted)
			return;

		String filename = null;
		try {

			// Process the file if it's a file. Note that a node can be both a
			// file AND a directory -- this can happen with JCR repositories,
			// for example
			if (file.isFile()) {

				// process the file only if it matches the wildcard filters
				filename = file.getFullName();
				if (wildcardMatcher == null
						|| wildcardMatcher.isIncluded(filename)) {

					if (!handleContainerFiles(file)) {
						processFile(file);
					}

				} else {
					if (debug) {
						logger.debug("File rejected by include/exclude filter:"
								+ filename);
					}
				}
			}

			if (file.isDirectory()) {
				FileIterator it = file.getIterator();
				while (it.hasNext()) {
					FileSystem subFile = it.next();
					if (subFile.isDirectory() && !scanSubDirs) {
						if (debug) {
							logger.debug("scanSubDirs is off, skipping subdir:"
									+ subFile.getFullName());
						}
					} else {
						scanInternal(subFile);
					}
				}
			}
		} catch (Throwable t) {
			// don't die, just log it
			connector.error("Error processing file:" + filename, t);
		}

	}

	/**
	 * Certain files, like .zip files, contain other files. This method digs
	 * into them.
	 * 
	 * @param file
	 * @return true if the file was handled; false if it should be treated as a
	 *         normal file
	 * @throws Exception
	 */
	private boolean handleContainerFiles(FileSystem file) throws Exception {

		String filename = file.getFullName();
		String lowerFilename = filename.toLowerCase();

		if (lowerFilename.endsWith(".zip")) {
			if (scanCompressedFiles) {
				ZipFileSystem zipFile = new ZipFileSystem(new File(filename));
				scanInternal(zipFile);
			}
			return true;

		} else if (lowerFilename.endsWith(".tar.gz")
				|| lowerFilename.endsWith(".tgz")) {
			if (scanCompressedFiles) {
				// TODO the ant jar file has an implementation we can use
				// ZipFileSystem zipFile = new ZipFileSystem(new
				// File(filename));
				// scanInternal(zipFile);
			}
			return true;

		}

		/*
		 * TODO if we implement MultiItemFileSystem, put it here else if
		 * (lowerFilename.endsWith(".xml")) { // if xml is multi-item // do it,
		 * return true // else fall through, return false }
		 */

		return false;
	}

	private void processFile(FileSystem file) throws IOException,
			PipelineException {
		if (interrupted)
			return;

		String fullname = file.getFullName();
		long sig = file.getSignature();

		// check to see if the doc has changed.
		// also updates the linkqueue
		if (!hasChanged(fullname, sig)) {
			if (debug) {
				logger.debug("File is unchanged:" + fullname);
			}
			return;
		}

		/*
		 * Normally, we won't get here if a file is deleted. It's possible on
		 * some file systems, though, that we'll see a file system entry, but
		 * the contents will be deleted. Just ignore the file; the linkqueue
		 * will zap it if it doesn't get updated
		 */

		item.clear();

		// if this file system produces items...
		if (file.getItem(item)) {
			if (debug) {
				logger.debug("Got item from file object " + fullname);
			}

		} else {

			long fileSize = file.getSize();
			if (fileSize > maxFileSizeInBytes) {
				logger.error("File size exceeds maximum. size=" + fileSize
						+ " max in bytes=" + maxFileSizeInBytes + " doc:"
						+ fullname);

			} else {
				DocBinary docBinary = new DocBinary();

				String lowerFilename = file.getFullName().toLowerCase();
				/**
				 * If the filename ends with .gz then use the GZIPInputStream to
				 * decompress the file. As a convention the original filename is
				 * appended with as .gz suffix. For example, sample.txt, when
				 * compressed using GZIP tool is renamed to sample.txt.gz.
				 */
				if (lowerFilename.endsWith(".gz") && scanCompressedFiles) {
					GZIPInputStream gzipStream = new GZIPInputStream(
							file.getInputStream());
					int index = lowerFilename.indexOf(".gz");
					String newFilename = file.getFullName().substring(0, index);
					docBinary.setName(newFilename);
					docBinary.setTimestamp(file.getLastUpdate());
					docBinary.setInputStream(gzipStream);
				} else {

					docBinary.setName(file.getFullName());
					docBinary.setTimestamp(file.getLastUpdate());
					docBinary.setSize(file.getSize());
					docBinary.setInputStream(file.getInputStream());
				}

				item.setDocBinary(docBinary);
				
			}
		}

		processItem(item, fullname);
	}

	private void processItem(Item item, String fullname)
			throws PipelineException {

		// push it down the pipeline
		stageList.processItem(item);

		docsProcessed++;
		if (docLoggingCount == 1) {
			logger.info(" Doc: " + fullname + " processed");

		} else if (docsProcessed % docLoggingCount == 0) {
			logger.info("Docs processed: " + docsProcessed);
		}
	}

	/**
	 * Return true if the name was found in the linkqueue and the signature has
	 * changed. Also updates the queue with the current startOfCrawl timestamp.
	 */
	private boolean hasChanged(String name, long sig) {
		if (linkQueue != null) {
			long prevSig = linkQueue.getSignature(name);
			linkQueue.update(name, sig, startOfCrawl);
			if (prevSig == sig) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Crawl all the items that didn't get touched, and remove them if not
	 * found.
	 */
	public void lookForDeletes() throws Exception {
		if (linkQueue == null)
			return;

		if (startOfCrawl == 0) {
			throw new IllegalStateException("startOfCrawl has not been set");
		}

		while (true) {
			String name = linkQueue.fetchNextUncrawled(startOfCrawl);
			if (name == null) {
				break;
			}

			// try to crawl it
			FileSystem file = root.fetch(name);
			if (file != null) {
				// could get here if they removed one of the file root entries,
				// or added some excludes
				processFile(file);

			} else {
				item.clear();
				item.setItemId(name);
				item.setAction(Item.ACTION_DELETE);

				// push it down the pipeline
				stageList.processItem(item);

				// remove it from the queue
				linkQueue.remove(name);
			}
		}
	}

	public void interrupt() {
		interrupted = true;
	}

	public void setWildcardMatcher(WildcardMatcher wildcardMatcher) {
		this.wildcardMatcher = wildcardMatcher;
	}

	public void setScanSubDirs(boolean scanSubDirs) {
		this.scanSubDirs = scanSubDirs;
	}

	public void setScanCompressedFiles(boolean scanCompressedFiles) {
		this.scanCompressedFiles = scanCompressedFiles;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setDocLoggingCount(int docLoggingCount) {
		this.docLoggingCount = docLoggingCount;
	}

	public void setLinkQueue(LinkQueue linkQueue) {
		this.linkQueue = linkQueue;
	}

	public int getDocsProcessed() {
		return docsProcessed;
	}

	/**
	 * Return the elapsed execution time in millis.
	 * 
	 * @return the elapsed time
	 */
	public long getElapsed() {
		return elapsed;
	}

	public void setStageList(StageList stageList) {
		this.stageList = stageList;
	}

	/**
	 * Set the maximum file size to process, in megabytes.
	 * 
	 * @param maxFileSize
	 */
	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSizeInBytes = maxFileSize * (1024 * 1024); // convert mb to
																// bytes
	}

	public void setParentConnector(Connector connector) {
		this.connector = connector;
	}

}
