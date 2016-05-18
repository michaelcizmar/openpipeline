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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.Util;
import org.slf4j.Logger;

/**
 * The main class of the WebCrawler. Crawls the web and processes found web
 * pages and documents.
 */
public class WebCrawler extends Connector {

	/*
	 * This is a multi threaded webcrawler with a master-slave architecture. The
	 * master thread handles two databases: one for domains and one for URLs.
	 * The master fetches the next domain to crawl and assigns it to a thread.
	 * Then it also fetches uncrawled URLs from the domain and gives them to the
	 * thread as input URLs.
	 * 
	 * The thread crawls the assigned URLs, collects the new ones and submits
	 * them to the master. It also submits the input URLs back to the master
	 * with the requested action (finalize, delete). It requests more URLs from
	 * its domain after it has crawled all input URLs.
	 * 
	 * Each thread has a list queue for the URLs from the master and a list
	 * queue of URLs that it collects during the crawl. The size of each queue
	 * is limited to 10K URLs.
	 */

	// review this:
	// http://www.searchenginecaffe.com/2009/12/bixolabs-public-terabyte-webcrawl.html

	public static int MAX_URL_QUEUE_SIZE = 10000;

	private DomainDB domainQueue;
	private LinkDB linkQueue;
	private WorkerThreadPool workerThreadPool;
	private long startOfCrawl;

	private String lastMessage;
	private int docsProcessed;
	private Logger logger;
	private boolean debug;
	private int errorCount;

	public void execute() {

		try {

			if (super.getParams() == null) {
				String msg = "Params not set for WebCrawler.";
				throw new IllegalStateException(msg);
			}

			super.setLastMessage("Initializing");

			logger = super.getLogger();
			debug = Server.getServer().getDebug();

			String jobName = super.getJobName();
			startOfCrawl = System.currentTimeMillis();

			/* Get the seed URLs */
			List<String> seedUrls = super.getParams().getValues("seed-urls");

			domainQueue = new DomainDB();
			domainQueue.setParams(super.getParams());
			domainQueue.initialize();

			linkQueue = new LinkDB();
			linkQueue.setParams(super.getParams());
			linkQueue.initialize();

			for (int i = 0; i < seedUrls.size(); i++) {

				String url = seedUrls.get(i);
				LinkDBRecord record = new LinkDBRecord(url);

				linkQueue.update(record);
				domainQueue.update(record.getDomain(), 0);
			}

			/* Initialize the thread pool */
			workerThreadPool = new WorkerThreadPool();
			workerThreadPool.setParams(super.getParams());
			workerThreadPool.initialize(this, startOfCrawl);

			logger.info("Starting " + jobName + "...");
			super.setLastMessage("Running");

			/* Do the crawl */
			crawl(startOfCrawl);

			String elapsedStr = Util.getFormattedElapsedTime(System
					.currentTimeMillis()
					- startOfCrawl);

			linkQueue.close();
			domainQueue.close();

			logger.info("WebCrawler ended. Docs processed: " + docsProcessed
					+ "  Elapsed time: " + elapsedStr);

			lastMessage = "Finished " + jobName + ". ";
			super.setLastMessage(lastMessage);

		} catch (Throwable e) {

			linkQueue.close();
			domainQueue.close();

			super.error("Error executing WebCrawler", e);
			super.setLastMessage("Failed. Message: " + e.getMessage());
		}
	}

	/**
	 * Crawls the domains in the domain Queue.
	 * 
	 * @param startOfCrawl
	 *            containing the time stamp for this crawl
	 */
	protected void crawl(long startOfCrawl) {
		String nextDomain = null;
		boolean hasLiveThreads;
		boolean isDone;

		while (true) {
			try {

				synchronized (this) {
					/* See if there are threads in the executor */
					hasLiveThreads = workerThreadPool.hasLiveThreads();

					if (!hasLiveThreads) {
						/* Finished because the crawl was interrupted. */
						if (debug) {
							logger.debug("All workerThreads are done.");
						}
						break;
					}

					if (!super.getInterrupted()) {
						/* Fetch next domain */
						nextDomain = domainQueue
								.fetchNextUncrawled(startOfCrawl);
					}

					isDone = workerThreadPool.isDone();
				}
				if ((isDone && nextDomain == null) || super.getInterrupted()) {
					/* Finished because the crawl ended. */
					if (debug) {
						logger.debug("All workerThreads are done.");
					}
					/*
					 * On finish or interrupt, call take until there are no more
					 * running workerThreads. This will reduce the count of
					 * liveThreads.
					 * 
					 * This needs to be outside the synchronized statement.
					 */
					workerThreadPool.take();
					continue;
				}

				if (nextDomain == null) {
					/*
					 * If the next domain is null but there are unfinished
					 * threads in the executor. Try again, waiting for more
					 * worker threads to finish and possibly supply new domains
					 */
					continue;
				}

				/*
				 * If we reached this point, there is a new domain to crawl.
				 * Wait for one of the threads to become available
				 */
				WorkerThread nextWorkerThread = workerThreadPool.take();

				/*
				 * Here nextWorkerThread cannot be null because there were
				 * unfinished threads in the executor.
				 */
				nextWorkerThread.setDomain(nextDomain);

				/*
				 * Get the next available workerThread and assign the next
				 * domain to it
				 */
				LinkedList<LinkDBRecord> newURLs = getURLs(nextDomain,
						startOfCrawl);
				nextWorkerThread.setInProgressURLs(newURLs);

				workerThreadPool.submit(nextWorkerThread);

				if (debug) {
					logger.debug("Put back in queue workerThread ID: "
							+ nextWorkerThread.getID() + ". Next domain: "
							+ nextDomain);
				}
			} catch (Throwable e) {
				logger.error("Exception in crawl, skipping this domain.");
			}
		}
	}

	/**
	 * Gets uncrawled URLs from this domain from the linkQueue.
	 * 
	 *@param nextDomain
	 *            containing the name of the requested domain
	 *@param startOfCrawl
	 *            containing the time stamp of the current crawl, only URLs with
	 *            the time stamp before this are fetched
	 *@throws RuntimeException
	 */
	public LinkedList<LinkDBRecord> getURLs(String nextDomain, long startOfCrawl) {

		synchronized (linkQueue) {
			return this.linkQueue.fetchUncrawledURLs(nextDomain, startOfCrawl,
					MAX_URL_QUEUE_SIZE);
		}
	}

	/**
	 * Finalize the domain in the domain queue.
	 * 
	 *@param domain
	 *            containing the name of the requested domain
	 * @param startOfCrawl
	 *            containing the time stamp of the current crawl, once the time
	 *            stamp of the domain is set to this value in the domain queue,
	 *            it will be finalized
	 */
	public void finalizeDomain(String domain, long startOfCrawl) {

		synchronized (domainQueue) {
			domainQueue.update(domain, startOfCrawl);
		}
	}

	/**
	 * Interrupts the crawl. Lets the worker threads finish, collects the
	 * processed URLs.
	 * 
	 * @throws PipelineException
	 */
	public void interrupt() throws RuntimeException {

		super.interrupt();

		/*
		 * WebCrawler is multi-threaded, need to interrupt the thread pool
		 */
		try {
			workerThreadPool.interrupt();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Collects URLs from the URL queue of the calling workerThread and updates
	 * the linkQueue and the domainQueue.
	 * 
	 * @param queue
	 *            containing the URLs
	 */
	public void collectURLS(LinkedList<LinkDBRecord> queue) {

		/* Collect the domains of the new URLs */
		Map<String, Integer> newDomains = new HashMap<String, Integer>();

		synchronized (linkQueue) {

			if (linkQueue == null || queue == null) {
				return;
			}

			Iterator<LinkDBRecord> iterator = queue.iterator();
			while (iterator.hasNext()) {
				LinkDBRecord record = iterator.next();
				if (record.getAction().equals(LinkDBRecord.ACTION_DELETE)) {
					linkQueue.remove(record.getNextUrl());
				} else {
					linkQueue.update(record);
					String nextDomain = record.getDomain();
					newDomains.put(nextDomain, 1);
				}
			}
		}

		synchronized (domainQueue) {
			Iterator<String> iter = newDomains.keySet().iterator();

			while (iter.hasNext()) {
				String nextDoman = iter.next();
				/*
				 * Here we assume it is a new domain, if not, it will be flagged
				 * as "in progress" in the domainDB and not updated
				 */
				long newTime = 0;
				domainQueue.addNew(nextDoman, newTime);
			}
		}
	}

	/**
	 * Updates the number of processed documents.
	 * 
	 * @param numProcessedDocs
	 *            containing the number of documents to be added to the number
	 *            of processed documents
	 */
	public synchronized void addDocProcessCounts(int numProcessedDocs) {
		docsProcessed += numProcessedDocs;
	}

	/**
	 * Updates the number of errors.
	 * 
	 * @param numErrors
	 *            containing the number of errors
	 */
	public synchronized void setNumberOfErrors(int numErrors) {
		this.errorCount += numErrors;
	}

	public String getLastMessage() {
		String msg = super.getLastMessage();
		if (docsProcessed > 0) {
			msg += "/ docs processed: " + docsProcessed;
		}
		return msg;
	}

	public String getDescription() {
		return "Crawls websites";
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}

	public String getPageName() {
		return "connector_web_crawler.jsp";
	}

	public String getShortName() {
		return "WebCrawler";
	}
	
	public String getDisplayName() {
		return "Web Crawler";
	}

}
