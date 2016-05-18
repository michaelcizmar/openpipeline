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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.openpipeline.server.Server;
import org.openpipeline.util.FileUtil;
import org.slf4j.Logger;

/**
 * Test for the Robots class
 */
@SuppressWarnings("serial")
public class TestRobots extends TestCase {

	private Logger logger;
	private Robots robots = new Robots();

	// robots_1.txt
	//
	// # Test user agent
	//
	// User-agent: googlebot
	// Allow: /
	// Disallow: /.*pdf
	//
	// User-agent: openpipebot
	// Allow: /images/2003/
	// Disallow: /files/
	//
	// User-agent: *
	// Allow: /.*html
	// Disallow: /images/

	private HashMap<String, Boolean> robots_1_AllAgents = new HashMap<String, Boolean>() {

		private static final long serialVersionUID = 1L;

		{
			put("http://www.mysite.com/file.html", true);
			put("http://www.mysite.com/images/2003/file.txt", false);
			put("http://www.mysite.com/archive/images/2003/file.txt", true);
			put("http://www.mysite.com/images/2003/file.html", true);
			put("http://www.mysite.com/file.pdf", true);
			put("http://www.mysite.com/files/file.doc", true);
		}
	};

	private HashMap<String, Boolean> robots_1_Googlebot = new HashMap<String, Boolean>() {
		{
			put("http://www.mysite.com/file.html", true);
			put("http://www.mysite.com/images/2003/file.txt", true);
			put("http://www.mysite.com/images/2003/file.pdf", false);
			put("http://www.mysite.com/file.pdf", false);
			put("http://www.mysite.com/files/file.doc", true);
		}
	};

	private HashMap<String, Boolean> robots_1_Openpipebot = new HashMap<String, Boolean>() {
		{
			put("http://www.mysite.com/file.html", true);
			put("http://www.mysite.com/images/file.jpg", true);
			put("http://www.mysite.com/images/2003/file.txt", true);
			put("http://www.mysite.com/images/2003/file.html", true);
			put("http://www.mysite.com/file.pdf", true);
			put("http://www.mysite.com/files/file.doc", false);
		}
	};

	// robots_2.txt
	//
	// User-agent: WeirdBot
	// Disallow: /links/listing.html
	// Disallow: /tmp/
	// Disallow: /private/
	// Disallow: /*/private/*
	//
	// User-agent: openpipebot
	// Disallow:
	//
	// User-agent: *
	// Allow: /images/2003/
	// Disallow: /images/
	// Disallow: /*pdf
	// Allow: /
	// Disallow: /temp*
	// Allow: *temperature*
	// Disallow: /private/
	// Disallow: /*/private/*

	private HashMap<String, Boolean> robots_2_AllAgents = new HashMap<String, Boolean>() {
		{
			put("http://mysite/toolkit/file.txt", true);
			put("http://www.mysite.com/toolkit/file.pdf", false);
			put("http://www.mysite.com/images/file.jpg", false);
			put("http://www.mysite.com/archive/images/file.jpg", true);
			put("http://www.mysite.com/images/2003/file.jpg", true);
			put("http://www.mysite.com/images/2003/itinerary.pdf", false);
			put("http://www.mysite.com/temp/2003/itinerary.pdf", false);
			put("http://www.mysite.com/temporary.pdf", false);
			put("http://www.mysite.com/images/2003/temperature.doc", true);
			put("http://www.mysite.com/temp/temperature2.doc", true);
			put("http://www.mysite.com/weather/temperature", true);
			put("http://www.mysite.com/private/file.pdf", false);
			put("http://www.mysite.com/images/2003/private/file.txt", false);
			put("http://www.mysite.com/2002/private.txt", true);
		}
	};

	private HashMap<String, Boolean> robots_2_Openpipebot = new HashMap<String, Boolean>() {
		{
			put("http://mysite/toolkit/file.txt", true);
			put("http://www.mysite.com/toolkit/file.pdf", true);
			put("http://www.mysite.com/images/file.jpg", true);
			put("http://www.mysite.com/archive/images/file.jpg", true);
			put("http://www.mysite.com/images/2003/file.jpg", true);
			put("http://www.mysite.com/images/2003/itinerary.pdf", true);
			put("http://www.mysite.com/temp/2003/itinerary.pdf", true);
			put("http://www.mysite.com/temporary.pdf", true);
			put("http://www.mysite.com/images/2003/temperature.doc", true);
			put("http://www.mysite.com/temp/temperature2.doc", true);
			put("http://www.mysite.com/weather/temperature", true);
			put("http://www.mysite.com/private/file.pdf", true);
			put("http://www.mysite.com/images/2003/private/file.txt", true);
			put("http://www.mysite.com/2002/private.txt", true);
		}
	};

	private HashMap<String, Boolean> robots_2_Weirdbot = new HashMap<String, Boolean>() {
		{
			put("http://mysite/toolkit/file.txt", true);
			put("http://mysite/links/listing.html", false);
			put("http://www.mysite.com/toolkit/file.pdf", true);
			put("http://www.mysite.com/tmp/file.jpg", false);
			put("http://www.mysite.com/archive/tmp/file.jpg", true);
			put("http://www.mysite.com/archive/images/file.jpg", true);
			put("http://www.mysite.com/images/2003/itinerary.pdf", true);
			put("http://www.mysite.com/temp/2003/itinerary.pdf", true);
			put("http://www.mysite.com/temporary.pdf", true);
			put("http://www.mysite.com/images/2003/temperature.doc", true);
			put("http://www.mysite.com/temp/temperature2.doc", true);
			put("http://www.mysite.com/weather/temperature", true);
			put("http://www.mysite.com/private/file.pdf", false);
			put("http://www.mysite.com/images/2003/private/file.txt", false);
			put("http://www.mysite.com/2002/private.txt", true);
		}
	};

	// robots_3.txt
	//	
	// # Test Allow/Disallow
	//
	// User-agent: *
	// Allow: /
	// Disallow: /cgi-bin/
	// Disallow: /foren/
	// Disallow: /bib/lhb/
	private HashMap<String, Boolean> robots_3 = new HashMap<String, Boolean>() {
		{
			put("http://mysite/cgi-bin/file.txt", false);
			put("http://mysite/toolkit/file.txt", true);
			put("http://mysite/bib/file.txt", true);
			put("http://mysite/bib/lhb/file.txt", false);
		}
	};

	public void testRobots() {

		String dir = "/temp/robotstest";
		System.setProperty("app.home", dir);
		System.setProperty("derby.system.home", dir);

		logger = Server.getServer().getLogger();

		try {

			testRobotsSafe(robots_1_AllAgents, "all", "robots_1.txt");
			testRobotsSafe(robots_1_Googlebot, "googlebot", "robots_1.txt");
			testRobotsSafe(robots_1_Openpipebot, "openpipebot", "robots_1.txt");
			testRobotsSafe(robots_2_AllAgents, "all", "robots_2.txt");
			testRobotsSafe(robots_2_Openpipebot, "openpipebot", "robots_2.txt");
			testRobotsSafe(robots_2_Weirdbot, "weirdbot", "robots_2.txt");
			testRobotsSafe(robots_3, "all", "robots_3.txt");

			testVisitTime("all", "robots_1.txt", true);
			testVisitTime("googlebot", "robots_1.txt", true);
			testVisitTime("openpipebot", "robots_1.txt", false);
			testVisitTime("all", "robots_2.txt", true);
			testVisitTime("openpipebot", "robots_2.txt", false);
			testVisitTime("weirdbot", "robots_2.txt", true);

		} catch (Throwable e) {
			logger.error("Exception. Message = " + e.getMessage());
		}
	}

	private void testRobotsSafe(HashMap<String, Boolean> directives,
			String userAgent, String file) throws IOException {

		String name = this.getClass().getResource("TestRobots.class")
				.toString();
		name = name.replaceAll("TestRobots.class", "");
		name = name.replaceAll("target/test-classes/", "src/test/java/");
		name = name.replaceAll("file:/", "");
		file = name + file;

		logger.info("Test for file = " + file + " userAgent = " + userAgent);

		String content = FileUtil.getFileAsString(file);
		robots.readRobotsTxt(content);

		boolean allow = true;
		Iterator<Entry<String, Boolean>> it = directives.entrySet().iterator();
		String nextUrl = null;

		while (it.hasNext()) {

			Map.Entry<String, Boolean> pair = it.next();

			try {

				nextUrl = pair.getKey();
				boolean correctAnswer = pair.getValue();

				UrlItem nextTestUrl = new UrlItem(nextUrl);

				allow = robots.allowed(nextTestUrl);

				assertEquals(correctAnswer, allow);
				logger.info("OK allow test for " + nextUrl
						+ " correctAnswer = " + correctAnswer);

			} catch (MalformedURLException e) {
				logger.error("Malformed url " + nextUrl + " " + e.getMessage());
			} catch (Throwable e) {
				logger.error("FAILED allow test for " + nextUrl
						+ ". Message = " + e.getMessage());
			}
		}
	}

	private void testVisitTime(String userAgent, String robotsFile,
			boolean correctAnswer) {

		String name = this.getClass().getResource("TestRobots.class")
				.toString();
		name = name.replaceAll("TestRobots.class", "");
		name = name.replaceAll("target/test-classes/", "src/test/java/");
		name = name.replaceAll("file:/", "");
		robotsFile = name + robotsFile;

		logger.info("Test visit time for file = " + robotsFile
				+ " userAgent = " + userAgent);
		try {
			String content = FileUtil.getFileAsString(robotsFile);
			robots.readRobotsTxt(content);

			boolean visitTimeOk = robots.visitTimeOK();
			assertEquals(correctAnswer, visitTimeOk);
			logger.info("OK visit time test for " + userAgent
					+ " correctAnswer = " + correctAnswer);

		} catch (Throwable e) {
			logger.error("FAILED visit time test for " + userAgent
					+ ". Message = " + e.getMessage());
		}
	}

}
