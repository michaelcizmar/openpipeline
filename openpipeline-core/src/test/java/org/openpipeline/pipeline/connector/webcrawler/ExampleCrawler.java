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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;


/**
 * 
 */
public class ExampleCrawler {
	
	private int liveThreadCount;
	

	public void run() {
		
		ExecutorService threadPool = createThreadPool();
		
		while (true) {
			
			String nextDomain;
			boolean hasLiveThreads;
			
			synchronized (this) {
				// this is the key section. must check both together in sync'd block
				nextDomain = getNextDomainFromDB();
				hasLiveThreads = liveThreadCount > 0;
			}

			if (nextDomain != null) {
			
				
				//threadPool.execute(command)
				
				continue;
			}
			
			// if we get here, there are no more domains
			if (hasLiveThreads) {
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
				
				// or do wait/notify
				
				continue;
			}
			
			// no more domains and no more live threads. quit.
			break;
		}
		
	}

	private ExecutorService createThreadPool() {
		
		/*
		 * could return worker threads?
		ConfigurableThreadFactory factory = new ConfigurableThreadFactory();
		factory.setDaemon(true);
		return Executors.newCachedThreadPool(factory);
		*/
		
		
		// TODO Auto-generated method stub
		return null;
	}

	private String getNextDomainFromDB() {
		return null;
	}

	public synchronized void incrLiveThreads() {
		liveThreadCount++;
	}

	public synchronized void decrLiveThreads() {
		liveThreadCount--;
	}

	public synchronized void insertDomainIntoDB(String domain) {
	}


}

class WorkerThreadFactory implements ThreadFactory {

	public Thread newThread(Runnable r) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

class Worker {

	private ExampleCrawler crawler;

	public Worker(ExampleCrawler crawler) {
		this.crawler = crawler;
	}
	
	public void crawlDomain(String domain) {
		crawler.incrLiveThreads();

		// crawl the domain here
		
		// if new domain found...
		crawler.insertDomainIntoDB("foo.com");

		
		// must be last method
		crawler.decrLiveThreads();
	}

}
