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
package org.openpipeline.pipeline.connector.webcrawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.Adler32;

import org.openpipeline.pipeline.docfilter.DocFilter;
import org.openpipeline.pipeline.docfilter.DocFilterFactory;
import org.openpipeline.pipeline.docfilter.HTMLFilter;
import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.stage.StageList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.ByteArray;
import org.openpipeline.util.URLUtils;
import org.openpipeline.util.WildcardMatcher;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * The main class to crawl and process the URLs assigned to it by the WebCrawler
 * and collects new URLs.
 */
public class WorkerThread implements Callable<WorkerThread> {

	public static int DEFAULT_MAX_LINK_DEPTH = 5;
	public static int DEFAULT_DOC_LOGGING_COUNT = 1;
	public static boolean DEFAULT_IGNORE_DYNAMIC_PAGES = true;

	private int ID;

	// Make these class members to initialize or set only once
	private WebCrawler webCrawler;
	private WildcardMatcher wildcardMatcher;
	private StageList stageList;
	private Fetcher fetcher;
	private XMLConfig params;
	private Adler32 adler32 = new Adler32();
	private Item item = new Item();
	private Item tempItem = new Item();
	private DocFilterFactory docFilterFactory;

	// Provided by the webCrawler at the start of domain crawl
	private LinkedList<LinkDBRecord> inProgressURLs;

	// Will be returned to the webCrawler at the end of domain crawl
	private LinkedList<LinkDBRecord> doneURLs;
	private LinkedList<LinkDBRecord> newURLs;

	// Params
	private String domain;
	private int docLoggingCount;
	private boolean ignoreDynamicPages;
	private int maxLinkDepth;
	private int maxNewURLQueueSize;

	// Book keeping
	private long startOfCrawl;
	private int docsProcessed;
	private boolean interrupted;
	private Logger logger;
	private boolean debug;
	private boolean isDone;

	// For test
	protected byte[] inputStream;
	private boolean inputProvided;

	/**
	 * Crawls and processes the URLs assigned to it by the WebCrawler and
	 * collects new URLs.
	 * 
	 * @return itself
	 */
	public WorkerThread call() throws Exception {

		interrupted = false;
		isDone = false;

		if (params == null || fetcher == null) {
			throw new IllegalStateException(
					"Parameters not set, or initialization not done for worker thread ID: "
							+ ID);
		}

		if (inProgressURLs == null) {
			return this;
		}

		try {
			logger.info("Starting worker thread ID: " + ID + ", for domain: "
					+ domain);

			/* Reset for the new domain */
			fetcher.updateDomain(domain);
			doneURLs = new LinkedList<LinkDBRecord>();
			newURLs = new LinkedList<LinkDBRecord>();
			docsProcessed = 0;

			/*
			 * The main loop of the crawling process: get the next URL from the
			 * linkQueue, fetch the data, extract new links, push the data into
			 * the pipeline
			 */
			while (!interrupted) {

				/*
				 * Get the next URL to crawl from the linkQueue
				 */
				if (inProgressURLs.isEmpty()) {

					if (debug) {
						logger.debug("No more URLs in inProgressURLs. Submitting the queues to master. Trying to get more URLs.");
					}

					webCrawler.collectURLS(this.newURLs);
					webCrawler.collectURLS(this.doneURLs);
					newURLs.clear();
					doneURLs.clear();

					/* Request more URLs from this domain from the master */
					inProgressURLs = webCrawler.getURLs(domain, startOfCrawl);
					if (inProgressURLs.isEmpty()) {

						if (debug) {
							logger.debug("No more URLs in the link Queue.");
						}
						break;
					}

					if (debug) {
						logger.debug("Got more URLs from the master.");
					}
				}

				/* Get the fist url from the fifo queue and dequeue it. */
				LinkDBRecord linkDBRecord = inProgressURLs.removeFirst();

				if (debug) {
					logger.debug("Next URL from linkQueue: "
							+ linkDBRecord.getNextUrl());
				}

				/* Increment the number of fetch attempts */
				linkDBRecord
						.setFetchAttempts(linkDBRecord.getFetchAttempts() + 1);

				int action;

				/* unit test for linkqueue only */
				if (inputProvided) {
					action = HttpResultMapper.ACTION_FINALIZE;
					fetcher.setInputStream(inputStream);
					fetcher.setRedirectUrl(linkDBRecord.getNextUrl());
					fetcher.setLastFetchTimeThisDomain(System
							.currentTimeMillis());
					processUrl(linkDBRecord, action, fetcher);
				} else {

					try {
						/* Fetch the data from the nextUrl */
						action = fetcher.fetch(linkDBRecord);

						if (debug) {
							logger.debug("Fetched data with the action: "
									+ action);
						}

					} catch (Throwable e) {

						/* Something went wrong, set the status to delete */
						action = HttpResultMapper.ACTION_DELETE;

						webCrawler.error(
								"Exception fetching data from next URL: "
										+ linkDBRecord.getNextUrl()
										+ ". Removing it. Message: ", e);
					}
				}
				/*
				 * Update nextUrl according to the action. If appropriate,
				 * process the fetched data: extract the new links, put them
				 * into linkQueue; push the data into pipeline
				 */
				processUrl(linkDBRecord, action, fetcher);

				if (newURLs.size() >= maxNewURLQueueSize) {
					webCrawler.collectURLS(newURLs);
					newURLs.clear();
				}
			}
		} catch (Throwable e) {
			webCrawler.error("Nonrecoverable exception in WorkerThread: " + ID
					+ ". Message: " + e.getMessage(), e);
		}

		try {
			/*
			 * If interrupted or non-recoverable exception, submit the processed
			 * URLs to the master.
			 */
			if (newURLs != null && newURLs.size() > 0) {
				webCrawler.collectURLS(newURLs);
				newURLs.clear();
			}

			if (doneURLs != null && doneURLs.size() > 0) {
				webCrawler.collectURLS(doneURLs);
				doneURLs.clear();
			}

			if (domain != null) {
				webCrawler.finalizeDomain(domain, startOfCrawl);
			}

			stageList.flush();
		} catch (Throwable e) {
			logger.error("Error finishing up the worker thread id = " + ID
					+ ". " + e.getMessage(), e);
		}
		isDone = true;
		return this;
	}

	/**
	 * Updates the fetched URL depending on the result defined by the fetcher
	 * status, adds to doneURLs. If required, processes the fetched data.
	 * 
	 * @param linkDBRecord
	 *            containing the information about URL
	 * @param action
	 *            containing the action which should follow the fetch
	 * @param fetcher
	 *            which has the fetched data
	 */
	protected void processUrl(LinkDBRecord linkDBRecord, int action,
			Fetcher fetcher) {

		String nextUrl = linkDBRecord.getNextUrl();
		String redirectUrl = fetcher.getRedirectUrl();
		/*
		 * Possible results for URL update in linkQueue after fetch:
		 * skip/delete/permanent redirect/finalize
		 * 
		 * Need to set the lastCrawlTime after fetch appropriately to reflect
		 * the URL's status in the linkQueue.
		 */
		switch (action) {

		case HttpResultMapper.ACTION_DELETE:

			/* Set the status to delete */
			linkDBRecord.setAction(LinkDBRecord.ACTION_DELETE);
			doneURLs.add(linkDBRecord);
			return;

		case HttpResultMapper.ACTION_SKIP:

			/*
			 * Put it back into the fifo queue as uncrawled during the current
			 * crawl
			 */
			linkDBRecord.setLastCrawlTime(0);
			inProgressURLs.add(linkDBRecord);
			return;

		case HttpResultMapper.ACTION_PERMANENT_REDIRECT:
			/*
			 * Redirect to a different domain
			 */
			try {
				int i = insertLinkIntoLinkQueue(redirectUrl,
						linkDBRecord.getLinkDepth());
				if (debug && i > 0) {
					logger.info("Inserted the redirect URL into newURLs = "
							+ redirectUrl);
				}
			} catch (Throwable e) {
				/*
				 * Ignore bad redirect URL, should not happen, because fetcher
				 * already checked it.
				 */
			}

			/* Set the status to delete for the original URL */
			linkDBRecord.setAction(LinkDBRecord.ACTION_DELETE);
			doneURLs.add(linkDBRecord);
			return;

		case HttpResultMapper.ACTION_FINALIZE:

			/*
			 * At this point we may have two urls: the original url and
			 * (possibly) a redirect url
			 */
			try {

				if (!nextUrl.equals(redirectUrl)) {
					/* Should not happen */
					logger.warn("nextUrl not equal redirectUrl for case HttpResultMapper.ACTION_FINALIZE");
					logger.warn("nextUrl = " + nextUrl + " redirectUrl = "
							+ redirectUrl);
				}

				byte[] inputStream = fetcher.getInputStream();

				/* Compute checksum for the fetched data */
				long previousSignature = linkDBRecord.getSignature();
				long newSignature = updateChecksum(inputStream);

				/*
				 * Finalize next URL in the link Queue.
				 * 
				 * Overrides the existing record, updates the signature if it
				 * changed, and sets last crawl time to > startOfCrawl which
				 * finalizes this item in the link queue
				 */
				if (newSignature != -1) {
					linkDBRecord.setSignature(newSignature);
				}

				/* Process data from modified pages */
				if (newSignature != -1 && previousSignature != newSignature) {
					processData(linkDBRecord, inputStream);
				}

				/*
				 * If the fetched data was processed successfully, finalize the
				 * url
				 */
				linkDBRecord.setLastCrawlTime(fetcher
						.getLastFetchTimeThisDomain());
				linkDBRecord.setAction(LinkDBRecord.ACTION_UPDATE);
				doneURLs.add(linkDBRecord);

			} catch (Throwable e) {

				webCrawler
						.error("Error processing data and finalizing nextURL="
								+ nextUrl + " or redirect URL=" + redirectUrl
								+ ". Message = " + e.getMessage());

				/*
				 * If error processing fetched data, remove both, the original
				 * and the redirect urls
				 */
				try {
					LinkDBRecord record = new LinkDBRecord(nextUrl);
					record.setAction(LinkDBRecord.ACTION_DELETE);
					doneURLs.add(record);

					if (!nextUrl.equals(redirectUrl)) {
						// linkQueue.remove(redirectUrl);
						LinkDBRecord recordRedir = new LinkDBRecord(redirectUrl);
						recordRedir.setAction(LinkDBRecord.ACTION_DELETE);
						doneURLs.add(recordRedir);

						/*
						 * It is possible that the redirect url is in the
						 * inProgress queue, avoid processing it twice
						 */
						inProgressURLs.remove(recordRedir);
					}
				} catch (Throwable t) {
					/*
					 * This error would come from the initialize method which
					 * includes formatting url string as URL. Should not happen
					 * because these urls have been checked already and
					 * formatted successfully.
					 */
					logger.error("Error removing nextURL=" + nextUrl
							+ " or redirect URL=" + redirectUrl);
				}
			}
			return;
		default:
			if (debug) {
				logger.debug("No fetcher entry for action: " + action
						+ ", deleting next URL: " + nextUrl);
			}
			// linkQueue.remove(nextUrl);
			linkDBRecord.setAction(LinkDBRecord.ACTION_DELETE);
			doneURLs.add(linkDBRecord);

			try {
				if (!nextUrl.equals(redirectUrl)) {

					LinkDBRecord recordRedir = new LinkDBRecord(redirectUrl);
					recordRedir.setAction(LinkDBRecord.ACTION_DELETE);
					doneURLs.add(recordRedir);

					/*
					 * It is possible that the redirect url is in the inProgress
					 * queue, avoid processing it twice
					 */
					inProgressURLs.remove(recordRedir);
				}
			} catch (Throwable t) {
				/*
				 * This error would come from the initialize method which
				 * includes formatting url string as URL. Should not happen
				 * because these urls have been checked already and formatted
				 * successfully.
				 */
				logger.error("Error removing nextURL=" + nextUrl
						+ " or redirect URL=" + redirectUrl);
			}
		}
	}

	/**
	 * Processes the fetched data. Extracts new links and pushes the data into
	 * the pipeline.
	 * 
	 * @param linkDBRecord
	 *            containing the URL which was fetched
	 * @param inputStream
	 *            containing the fetched data
	 * @throws PipelineException
	 */
	private void processData(LinkDBRecord linkDBRecord, byte[] inputStream)
			throws PipelineException {

		String nextUrl = linkDBRecord.getNextUrl();

		/* Get the docBinary data */
		DocBinary docBinary = new DocBinary();
		ByteArray input = new ByteArray(inputStream);
		docBinary.setInputStream(input);
		int size = inputStream.length;
		docBinary.setSize(size);
		docBinary.setName(nextUrl);

		/* Extract the new links */
		int currentLinkDepth = linkDBRecord.getLinkDepth();
		if (currentLinkDepth < maxLinkDepth) {

			try {

				/* Use the tempItem to extract the new links */
				tempItem.clear();
				tempItem.setDocBinary(docBinary);
				tempItem.setItemId(docBinary.getName());

				/* Extract new links */
				List<String> newLinks = getNewLinks(tempItem);
				int numInserted = insertNewLinksIntoLinkQueue(newLinks,
						currentLinkDepth);

				tempItem.clear();

				if (debug) {
					logger.debug("Inserted new links into linkQueue, number of new Links: "
							+ numInserted);
				}
			} catch (Throwable e) {
				throw new PipelineException(
						"Error extracting new URLs. Message = "
								+ e.getMessage());
			}
		}

		/*
		 * Push the URL data into the pipeline if the URL matches
		 * include/exclude patterns
		 */
		boolean isIncluded = wildcardMatcher.isIncluded(linkDBRecord
				.getNextUrl());

		long lastModified = fetcher.getLastModified();
		if (lastModified > 0 && lastModified <= linkDBRecord.getLastCrawlTime()) {
			/*
			 * No need to re-index this page, not modified since the last crawl
			 */
			isIncluded = false;
		}

		if (isIncluded) {

			try {

				item.clear();

				/* Reset the input stream */
				input = new ByteArray(inputStream);
				docBinary.setInputStream(input);
				int size2 = inputStream.length;
				docBinary.setSize(size2);
				docBinary.setName(nextUrl);

				item.setDocBinary(docBinary);
				item.setItemId(docBinary.getName());

				stageList.processItem(item);
				
			} catch (Throwable e) {
				throw new PipelineException(
						"Error processing the URL in the pipeline, URL = "
								+ nextUrl + ". Message = " + e.getMessage());
			}
			docsProcessed++;

			if (docLoggingCount == 1) {
				logger.info("WorkerThread: " + ID + ". Processed document: "
						+ item.getDocBinary().getName());

			} else if (docLoggingCount > 0
					&& docsProcessed % docLoggingCount == 0) {
				logger.info("WorkerThread: " + ID
						+ ". Number of processed documents: " + docsProcessed);
			}
			if (webCrawler != null) {
				webCrawler.addDocProcessCounts(1);
			}
		}
	}

	/**
	 * Extracts new links from the document
	 * 
	 * @param item
	 *            containing the data to be processed by the filter
	 * @return new links as a List, an empty list if there are no new links
	 * @throws PipelineException
	 */
	private List<String> getNewLinks(Item item) throws PipelineException {

		List<String> newLinks = Collections.emptyList();

		if (item.getItemId() == null || item.getDocBinary() == null)
			return null;

		String docName = item.getItemId();

		/*
		 * Make the html filter the default filter on the docfilter
		 * configuration page since many pages don't have the html extension
		 */
		DocFilter filter = docFilterFactory.getFilterByFilename(docName);

		if (filter == null) {
			filter = docFilterFactory.getFilterByFilename(docName + ".html");
		}

		if (filter != null) {

			if (filter.getDocType().equals("html")) {
				((HTMLFilter) filter).setBaseURL(docName);
			}

			if (debug) {
				logger.debug("Document: " + docName + ", filtered by: "
						+ filter.getDisplayName());
			}

			filter.processItem(item);

			newLinks = filter.getLinks();

		}
		return newLinks;
	}

	/**
	 * Inserts new links into the newURLs queue. New links are pre-processed to
	 * make sure that only well-formed URLs which match include/exclude patterns
	 * go into the newURLs queue.
	 * 
	 * @param newLinks
	 *            containing the links to be put into the newURLs queue
	 * @param currentLinkDepth
	 *            containing the linkDepth of the parent URL
	 * @return number of inserted links
	 */
	protected int insertNewLinksIntoLinkQueue(List<String> newLinks,
			int currentLinkDepth) {

		int numInserted = 0;

		if (newLinks == null || newLinks.isEmpty())
			return 0;

		for (String nextLink : newLinks) {
			try {
				numInserted += insertLinkIntoLinkQueue(nextLink,
						currentLinkDepth);

			} catch (Throwable e) {
				if (debug) {
					logger.debug("Warning in insertNewLinksIntoLinkQueue, skipping URL: "
							+ nextLink + ". Message: " + e.getMessage());
				}
			}
		}
		return numInserted;
	}

	/**
	 * Inserts a new link into the newURLs link queue. The new link is
	 * pre-processed to make sure that only well-formed URLs which match
	 * include/exclude patterns go into the newURLs.
	 * 
	 * @param nextLink
	 *            containing the link to be put into the newURLs queue
	 * @param currentLinkDepth
	 *            containing the linkDepth of the parent URL
	 * @return number of inserted links
	 */
	private int insertLinkIntoLinkQueue(String nextLink, int currentLinkDepth)
			throws Throwable {

		int numInserted = 0;

		boolean isDynamic = isDynamicPage(nextLink);
		/* Dynamic pages filter */
		if (ignoreDynamicPages && isDynamic) {
			return 0;
		}

		/* Match against include/exclude filters */
		boolean isIncluded = wildcardMatcher.isIncluded(nextLink);
		if (!isIncluded) {
			return 0;
		}

		LinkDBRecord record = new LinkDBRecord(nextLink);
		record.setLinkDepth(currentLinkDepth + 1);

		newURLs.add(record);

		/*
		 * If the size of the list reaches max, submit the URLs to the master
		 * queue
		 */

		numInserted++;

		if (newURLs.size() >= maxNewURLQueueSize) {
			webCrawler.collectURLS(newURLs);
			newURLs.clear();
		}

		return numInserted;
	}

	/**
	 * Calculates a checksum of the fetched data and updates the Adler32 value.
	 * 
	 * @return the computed check sum
	 */
	private long updateChecksum(byte[] inputStream) {

		if (inputStream == null) {
			/* No data was fetched, redirect, etc, do nothing */
			return -1;
		}
		String input = new String(inputStream);// TODO look into charset
		adler32.reset();

		if (input.length() == 0)
			adler32.update(0);
		else {
			int len = input.length();
			for (int i = 0; i < len; i++) {
				char ch = input.charAt(i);
				adler32.update((ch >> 8) & 0xff);
				adler32.update(ch & 0xff);
			}
		}
		long signature = adler32.getValue();
		return signature;
	}

	public void initialize() throws RuntimeException {

		if (params == null) {
			throw new RuntimeException("Params not set for workerThread.");
		}

		try {

			logger = Server.getServer().getLogger();
			debug = Server.getServer().getDebug();

			/* Set parameters */
			maxLinkDepth = params.getIntProperty("max-link-depth",
					DEFAULT_MAX_LINK_DEPTH);
			docLoggingCount = params.getIntProperty("doc-logging-count",
					DEFAULT_DOC_LOGGING_COUNT);
			ignoreDynamicPages = params.getBooleanProperty(
					"ignore-dynamic-pages", DEFAULT_IGNORE_DYNAMIC_PAGES);

			/*
			 * Initialize Fetcher
			 */
			fetcher = new Fetcher();
			fetcher.setParams(params);
			fetcher.initialize();

			/*
			 * Initialize the document processing pipeline
			 */
			stageList = new StageList();
			stageList.createPipeline(params);
			stageList.initialize();

			/* Get the include/exclude patterns */
			List<String> includes = params.getValues("include-patterns");
			List<String> excludes = params.getValues("exclude-patterns");

			/* Initialize the wildcard Matcher */
			wildcardMatcher = new WildcardMatcher();
			wildcardMatcher.setIncludePatterns(includes);
			wildcardMatcher.setExcludePatterns(excludes);

			/*
			 * Initialize the DocFilterFactory, docfilters are used to extract
			 * new links
			 */
			XMLConfig stages = params.getChild("stages");
			if (stages != null) {
				docFilterFactory = new DocFilterFactory(params.getChild(
						"stages").getChild("stage"));
			} else {
				docFilterFactory = new DocFilterFactory();
			}

		} catch (Throwable e) {
			if (logger != null) {
				logger.error("Exception in WorkerThread: " + ID + ". Message: "
						+ e.getMessage(), e);
			}
			throw new RuntimeException(
					"Exception initializing the workerThread: " + ID);
		}
	}

	public void setParams(XMLConfig params) {
		this.params = params;
	}

	public void setInProgressURLs(LinkedList<LinkDBRecord> inProgressURLs) {
		this.inProgressURLs = inProgressURLs;
	}

	public void setStartOfCrawl(long startOfCrawl) {
		this.startOfCrawl = startOfCrawl;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public int getID() {
		return ID;
	}

	public void setWildcardMatcher(WildcardMatcher wildcardMatcher) {
		this.wildcardMatcher = wildcardMatcher;
	}

	public void setWebCrawler(WebCrawler webCrawler) {
		this.webCrawler = webCrawler;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDomain() {
		return domain;
	}

	private boolean isDynamicPage(String nextUrl) throws MalformedURLException {

		nextUrl = URLUtils.normalizeURL(nextUrl);

		URL url = new URL(nextUrl);
		String query = url.getQuery();

		if (query == null) {
			return false;
		}

		return true;
	}

	public void interrupt() {
		synchronized (this) {
			interrupted = true;
			logger.info("Interrupted. Worker thread ID: " + ID);
		}
	}

	public boolean isDone() {
		return this.isDone;
	}

	protected void setInputProvided(boolean inputProvided) {
		this.inputProvided = inputProvided;
	}

	protected void setInputStream(byte[] input) {
		this.inputStream = input;
	}

	public void printInProgress() {
		printQueue(inProgressURLs, "inProgress");
	}

	public void printDoneURLs() {
		printQueue(doneURLs, "doneURLs");
	}

	public void setMaxNewURLQueueSize(int size) {
		this.maxNewURLQueueSize = size;
	}

	private void printQueue(LinkedList<LinkDBRecord> queue, String name) {

		if (queue == null) {
			return;
		}

		logger.info("Writing queue " + name);

		Iterator<LinkDBRecord> iter = queue.iterator();

		while (iter.hasNext()) {
			LinkDBRecord record = iter.next();
			logger.info("Next record = " + record.getNextUrl()
					+ " signature = " + record.getSignature()
					+ " last crawl time = " + record.getLastCrawlTime());
		}
	}
}
