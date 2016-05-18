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
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.openpipeline.server.Server;
import org.openpipeline.util.FileUtil;
import org.slf4j.Logger;

public class TestUrlFilter extends TestCase {

	private HashMap<String, Boolean> domains1 = new HashMap<String, Boolean>();

	String[] urls = { "http://uni-heidelberg.de" };

	public void testUrlItem() {

		try {

			String dir = "/temp/TestWebCrawler";
			FileUtil.deleteDir(dir);
			System.setProperty("app.home", dir);

			Logger logger = Server.getServer().getLogger();

			domains1.put("uni-heidelberg.de", true);
			domains1.put("domain.de", false);
			domains1.put("", false);
			domains1.put(null, false);

			UrlFilter urlFilter = new UrlFilter();
			Iterator<Entry<String, Boolean>> it = domains1.entrySet()
					.iterator();

			for (String url : urls) {

				while (it.hasNext()) {

					Map.Entry<String, Boolean> pair = it.next();

					try {

						UrlItem urlItem = new UrlItem(url);

						urlFilter.setDomain(pair.getKey().toString());
						boolean result = urlFilter.checkRedirectUrl(urlItem);
						if (result != pair.getValue()) {
							logger.error("Error: result = " + result
									+ ". Correct = " + pair.getValue()
									+ " for URL " + url + "and domain = "
									+ pair.getKey());
						} else {
							logger.info("OK: result = " + result
									+ ". Correct = " + pair.getValue()
									+ " for URL " + url + "and domain = "
									+ pair.getKey());
						}
					} catch (Throwable e) {
						logger.error("Error: exception for URL " + url
								+ "and domain = " + pair.getKey());
					}
				}
			}
		} catch (Throwable e) {
			System.out.println("Exception. Message = " + e.getMessage());
		}
	}
}
