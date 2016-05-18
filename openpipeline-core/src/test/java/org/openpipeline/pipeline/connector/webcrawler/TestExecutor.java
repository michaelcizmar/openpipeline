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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

public class TestExecutor extends TestCase {

	private int numberOfWorkerThreads = 5;

	public void test() {

		String dir = "/temp/executortest";
		System.setProperty("app.home", dir);
		Logger logger = Server.getServer().getLogger();

		Executor executor = Executors.newFixedThreadPool(numberOfWorkerThreads);
		CompletionService<WorkerThread> completionService = new ExecutorCompletionService<WorkerThread>(
				executor);
		List<Future<WorkerThread>> futures = new ArrayList<Future<WorkerThread>>(
				numberOfWorkerThreads);

		/* Need it to define the termination condition */
		Map<Integer, Integer> idCount = new HashMap<Integer, Integer>();

		XMLConfig params = new XMLConfig();

		/*
		 * Don't need it here, use futures for interrupt()
		 */
		for (int i = 0; i < numberOfWorkerThreads; i++) {

			WorkerThread workerThread = new WorkerThread();
			workerThread.setID(i);
			workerThread.setParams(params);
			workerThread.initialize();
			Future<WorkerThread> runningWorkerThread = completionService
					.submit(workerThread);
			futures.add(runningWorkerThread);
			idCount.put(i, 0);

		}

		int finishedThreadsCount = 0;
		boolean working = true;

		while (working) {

			try {

				/* Wait for one of the threads to finish */
				Future<WorkerThread> completedWorkerThread = completionService
						.take();

				/* Get the workerThread return value, i.e. itself */
				WorkerThread nextWorkerThread = completedWorkerThread.get();
				futures.remove(nextWorkerThread);

				int id = nextWorkerThread.getID();
				int count = idCount.get(id);

				logger.info("Completed workerThread: " + id + ", iteration: "
						+ count);

				if (count < 10) {

					/* Put it back to execution */
					idCount.put(id, count + 1);
					Future<WorkerThread> threadTask = completionService
							.submit(nextWorkerThread);
					futures.add(threadTask);

					logger.info("Put back in queue workerThread: "
							+ completedWorkerThread.get().getID());
				} else {

					logger.info("Done with workerThread: " + id);
					finishedThreadsCount++;
					if (finishedThreadsCount == numberOfWorkerThreads) {
						working = false;
					}
				}
			} catch (Throwable e) {
				logger.error("Exception");
			}
		}
	}
}
