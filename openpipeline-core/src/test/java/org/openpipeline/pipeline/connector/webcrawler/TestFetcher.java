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

import junit.framework.TestCase;

import org.openpipeline.server.Server;
import org.openpipeline.util.FileUtil;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

public class TestFetcher extends TestCase {

	String[] urls = { "http://uni-heidelberg.de", "www.accenture.com",
			"accenture.com", "http://www.accenture.com/index.html", "",
			"http://www.uni-heidelberg.de", "http://www.uni-darmstadt.de/",
			"http://www.dieselpoint.com" };

	String[] urlsOne = { "http://www.cnn.com" };

	public void testUrlItem() {

		try {

			String dir = "/temp/fetcher";
			FileUtil.deleteDir(dir);
			System.setProperty("app.home", dir);
			Logger logger = Server.getServer().getLogger();

			XMLConfig params = new XMLConfig();

			Fetcher fetcher = new Fetcher();
			fetcher.setParams(params);
			fetcher.initialize();

			for (String url : urlsOne) {
				try {

					LinkDBRecord record = new LinkDBRecord(url);

					UrlFilter urlFilter = new UrlFilter();
					urlFilter.setDomain(record.getDomain());
					fetcher.setUrlFilter(urlFilter);

					logger.info("Result for URL " + url);
					logger.info("Normalized URL " + record.getNextUrl());
					int action = fetcher.fetch(record);
					logger.info("\tAction = " + action + ".");

				} catch (Throwable e) {
					logger.error("Error: exception for URL " + url
							+ ". Message = " + e.getMessage());
				}
			}
		} catch (Throwable e) {
			System.out.println("Exception. Message = " + e.getMessage());
		}
	}
}
