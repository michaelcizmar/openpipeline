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

public class TestUrlItem extends TestCase {

	String[] urls = { "http://uni-heidelberg.de", "/file.txt",
			"https://domain.de/index.html", "", "www.accenture.com" };

	public void testUrlItem() {
		for (String url : urls) {

			try {
				UrlItem nextUrlItem = new UrlItem(url);

				String nextUrl = nextUrlItem.getNextUrl();
				String domain = nextUrlItem.getDomain();

				System.out.println("\ninput = " + url);
				System.out.println("nextUrl = " + nextUrl);
				System.out.println("domain = " + domain);

			} catch (Throwable e) {
				System.out.println("Exception in parse url: " + url
						+ ". Message: " + e.getMessage());
				continue;
			}
		}
	}
}
