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

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openpipeline.util.XMLConfig;

/** Class responsible for the book-keeping of running a thread pool. */
public class WorkerThreadPool {

	public static int DEFAULT_NUMBER_OF_WORKER_THREADS = 1;

	private CompletionService<WorkerThread> completionService;
	private WorkerThread[] inProgressWT;
	private XMLConfig params;
	private int numberUsedThreads;
	private int runningThreadsCounter;
	private boolean interrupted;

	/**
	 * Submits a worker thread to the pool to be executed.
	 * 
	 * @param workerThread
	 */
	public void submit(WorkerThread workerThread) {

		if (interrupted) {
			return;
		}

		completionService.submit(workerThread);

		int id = workerThread.getID();
		inProgressWT[id] = workerThread;
		runningThreadsCounter++;
	}

	/**
	 * Obtains an available worker thread from the pool. Waits if needed.
	 * 
	 * @return WorkerThread or null if all worker threads are finished and the
	 *         executor is empty
	 */
	public WorkerThread take() throws InterruptedException, ExecutionException {

		WorkerThread workerThread = null;

		/*
		 * Get the future for the next completed workerThread. Null if none is
		 * completed.
		 */
		Future<WorkerThread> future = completionService.poll();

		/* All workerThreads are done */
		if (future == null && runningThreadsCounter == 0) {
			return null;
		}

		/*
		 * Available workerThreads are in the queue and there are some in the
		 * executor, need to wait for a workerThread to become available
		 */
		while (future == null) {
			try {
				Thread.sleep(1000);
			} catch (Throwable e) {
			}
			future = completionService.poll();
		}

		workerThread = future.get();
		int id = workerThread.getID();
		inProgressWT[id] = null;
		runningThreadsCounter--;

		return workerThread;

	}

	/**
	 * Indicates if there are threads in the executor, running and/or finished.
	 * 
	 * @return true if there are threads in the executor, false otherwise
	 */
	public boolean hasLiveThreads() {
		return (runningThreadsCounter > 0);
	}

	/**
	 * Initializes the worker thread pool.
	 * 
	 * @param webCrawler
	 *            containing the master webCrawler @ startOfCrawl containing the
	 *            time stamp of the start of the current crawl
	 */
	public void initialize(WebCrawler webCrawler, long startOfCrawl) {

		if (params == null) {
			throw new RuntimeException(
					"Params not set for the WorkerThreadPool");
		}

		int numberOfWorkerThreads = params.getIntProperty(
				"number-of-worker-threads", DEFAULT_NUMBER_OF_WORKER_THREADS);

		int availableProcessors = Runtime.getRuntime().availableProcessors();
		numberUsedThreads = Math.min(availableProcessors + 1,
				numberOfWorkerThreads);

		/*
		 * Used for book-keeping and accessing the workerThreads in the executor
		 * in case of an interrupt
		 */
		inProgressWT = new WorkerThread[numberUsedThreads];

		/* Initialize the executor */
		Executor executor = Executors.newFixedThreadPool(numberUsedThreads);
		completionService = new ExecutorCompletionService<WorkerThread>(
				executor);

		runningThreadsCounter = 0;

		for (int i = 0; i < numberUsedThreads; i++) {

			try {
				/* Initialize workerThread */
				WorkerThread workerThread = new WorkerThread();
				workerThread.setParams(params);
				workerThread.setWebCrawler(webCrawler);
				workerThread.setStartOfCrawl(startOfCrawl);
				workerThread
						.setMaxNewURLQueueSize(WebCrawler.MAX_URL_QUEUE_SIZE);
				workerThread.setID(i);
				workerThread.initialize();

				/* Submit it to the executor */
				this.submit(workerThread);
			} catch (Throwable e) {
			}
		}
	}

	/** Interrupts all worker threads in the pool. */
	public void interrupt() {
		/*
		 * Interrupt all running workerThreads
		 */
		for (int i = 0; i < inProgressWT.length; i++) {
			WorkerThread workerThread = inProgressWT[i];
			if (workerThread != null) {
				workerThread.interrupt();
			}
		}
		this.interrupted = true;
	}

	/**
	 * Checks if all worker threads in the pool are finished.
	 * 
	 * @return true if all worker threads are finished, false otherwise
	 */
	public boolean isDone() {

		for (int i = 0; i < inProgressWT.length; i++) {
			WorkerThread workerThread = inProgressWT[i];
			if (workerThread != null) {
				if (!workerThread.isDone()) {
					return false;
				}
			}
		}
		return true;
	}

	public void setParams(XMLConfig params) {
		this.params = params;
	}
}
