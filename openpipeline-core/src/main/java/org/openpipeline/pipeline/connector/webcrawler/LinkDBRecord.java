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
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class to hold information about a LinkQueue record. Max URL length to 2000.
 * char.
 */
public class LinkDBRecord {

	static public String ACTION_DELETE = "delete";
	static public String ACTION_UPDATE = "update";

	private String nextUrl;
	private String domain;
	private long lastCrawlTime;
	private long signature;
	private int fetchAttempts;
	private int linkDepth;
	private String action = ACTION_UPDATE;

	public LinkDBRecord(String url) throws MalformedURLException {

		UrlItem urlItem = new UrlItem(url);

		nextUrl = urlItem.getNextUrl();
		domain = urlItem.getDomain();
		lastCrawlTime = 0;
		signature = 0;

		fetchAttempts = 0;
		linkDepth = 0;
	}

	public boolean equals(Object other) {
		if (other instanceof LinkDBRecord) {
			return equals((LinkDBRecord) other);
		}
		return false;
	}

	/**
	 * Compares this record to another record. Two records are equal when their
	 * URLs are the same.
	 * 
	 * @param other
	 *            containing the record which is compared to this one
	 * @return true if the records are equal i.e. if their URLs are the same,
	 *         false otherwise
	 */
	public boolean equals(LinkDBRecord other) {

		if (!this.nextUrl.equals(other.getNextUrl())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int result = 131;
		result = 17 * result + nextUrl.hashCode();
		result = 17 * result + domain.hashCode();
		return result;
	}

	/**
	 * Sets the values for the LinkQueue record from the search ResultSet.
	 */
	public void setValues(ResultSet rs) throws SQLException {

		if (rs == null) {
			reset();
			return;
		}
		nextUrl = rs.getString("id");
		domain = rs.getString("domain");
		lastCrawlTime = rs.getLong("lastcrawl");
		signature = rs.getLong("signature");
		linkDepth = rs.getInt("linkdepth");
	}

	private void reset() {
		nextUrl = null;
		domain = null;
		lastCrawlTime = 0;
		signature = 0;
		fetchAttempts = 0;
		linkDepth = 0;
	}

	public void setNextUrl(String nextUrl) {
		this.nextUrl = nextUrl;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setLastCrawlTime(long lastCrawlTime) {
		this.lastCrawlTime = lastCrawlTime;
	}

	public void setSignature(long signature) {
		this.signature = signature;
	}

	/**
	 * Returns the URL string.
	 * 
	 * @return String containing the URL from the linkQueue record
	 */
	public String getNextUrl() {
		return nextUrl;
	}

	public String getDomain() {
		return domain;
	}

	public long getLastCrawlTime() {
		return lastCrawlTime;
	}

	public long getSignature() {
		return signature;
	}

	public void setFetchAttempts(int i) {
		this.fetchAttempts = i;
	}

	public void setLinkDepth(int i) {
		this.linkDepth = i;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getFetchAttempts() {
		return fetchAttempts;
	}

	public int getLinkDepth() {
		return this.linkDepth;
	}

	public String getAction() {
		return this.action;
	}
}
