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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.openpipeline.util.FileUtil;
import org.openpipeline.util.XMLConfig;

/**
 * Initializes the parameters for the crawl
 * 
 * @throws Exception
 */
public class TestWebCrawler extends TestCase {

	private List<String> inputURLs = new ArrayList<String>(Arrays.asList(
			"http://www.cnn.com/WORLD/", "http://www.cnn.com/POLITICS/",
			"http://www.cnn.com/TECH/", "http://www.cnn.com/SCIENCE/",
			"http://www.cnn.com/MUSIC/", "http://www.cnn.com/ART/",
			"http://www.msnbc.com/WORLD/", "http://www.msnbc.com/POLITICS/",
			"http://www.msnbc.com/TECH/", "http://www.msnbc.com/SCIENCE/",
			"http://www.msnbc.com/MUSIC/", "http://www.msnbc.com/ART/"));

	private byte[] testInput;

	/**
	 * 
	 */
	public void test() {

		try {

			String home = "C:/dev/openpipeline/trunk/openpipeline/";

			System.setProperty("app.home", home);
			System.setProperty("derby.system.home", home);

		//	FileUtil.deleteDir(home + "derby");
			FileUtil.deleteDir(home + "logs");

			// File jobFile = new File(home, "config/jobs/WebCrawler.xml");
			File jobFile = new File(home, "config/jobs/WebCrawler0.xml");

			XMLConfig params = new XMLConfig();
			params.load(jobFile);

			params.setProperty("number-of-worker-threads", "20");
			// params.addProperty("include-patterns", "*enron*pdf");
			// params.addProperty("include-patterns", "*archdrl*");
			// params.addProperty("seed-urls","http://news.findlaw.com/legalnews/lit/enron/#documents");
			// params.addProperty("seed-urls", "http://www.archdrl.com/");

			boolean runQueueTest = false;
			for (int i = 0; i < inputURLs.size(); i++) {
				// params.addProperty("seed-urls", inputURLs.get(i));
			}

			params.addProperty("seed-urls", "http://www.uni-heidelberg.de/");// "http://www.cnn.com");//
			// "http://www.dieselpoint.com"); //"http://www.archdrl.com/"

			params.setProperty("user-agent", "openpipebot");
			params.setProperty("follow-redirects", "true");
			params.setProperty("ignore-dynamic-pages", "true");
			params.setProperty("max-link-depth", "5");
			params.setProperty("max-number-of-redirects", "3");
			params.setProperty("min-crawl-delay", "6"); // 6 seconds
			params.setProperty("doc-logging-count", "1");
			params.setProperty("jobname", "WebCrawler");
			params.setProperty("max-file-size", "5242880");

			try {
				WebCrawler webCrawler = new WebCrawler();
				webCrawler.setParams(params);
				webCrawler.execute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} catch (Throwable t) {
			System.err.println("Error: " + t.toString());
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private void createTestInput() {

		String testFile = "";
		for (int i = 0; i < inputURLs.size(); i++) {

			testFile += "<a href=" + inputURLs.get(i) + ">test</a>"
					+ System.getProperty("line.separator");

			testFile += "<a href=" + inputURLs.get(i) + "zzz" + ">test</a>"
					+ System.getProperty("line.separator");
		}
		testInput = testFile.getBytes();
	}
}
