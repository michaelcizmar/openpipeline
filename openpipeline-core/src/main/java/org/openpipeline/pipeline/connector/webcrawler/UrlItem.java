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

import org.openpipeline.util.URLUtils;

/**
 * Class to hold information about a URL and its linkQueue record.
 */
public class UrlItem {

	private String nextUrl;
	private String protocol;
	private String host;
	private URL url;
	private String domain;
	private String relativePath; // used in robots

	/**
	 * Creates and initializes a URL item with java URL utils.
	 * 
	 * @param nextUrl
	 *            contains the URL to be parsed
	 * @throws MalformedURLException
	 */
	public UrlItem(String nextUrl) throws MalformedURLException {

		if (nextUrl == null)
			throw new RuntimeException("Input URL is null.");

		/* Deal with urls like "dieselpoint.com" */
		if (!nextUrl.toLowerCase().startsWith("http://")) {

			/* Ignore the special case of "mailto" */
			if (!nextUrl.contains("mailto:")) {

				/*
				 * Prepend "http://" to convert it to a well-formed url
				 */
				nextUrl = "http://" + nextUrl;
			}
		}

		String nextUrlNorm = URLUtils.normalizeURL(nextUrl);
		url = new URL(nextUrlNorm);

		protocol = url.getProtocol();
		host = url.getHost();
		domain = protocol + "://" + host;
		this.nextUrl = url.toString();

		if (domain.length() < this.nextUrl.length())
			/* Relative path is the path after the root directory */
			relativePath = this.nextUrl.substring(domain.length());
		else
			relativePath = "/";
	}

	/**
	 * Returns the URL string.
	 * 
	 * @return String containing next URL
	 */
	public String getNextUrl() {
		return nextUrl;
	}

	/**
	 * Returns domain string: protocol+://+host
	 * 
	 * @return String containing domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Returns the relative path, i.e. the path after the root directory
	 * 
	 * @return String containing the relative path
	 */
	public String getRelativePath() {
		return relativePath;
	}
}
