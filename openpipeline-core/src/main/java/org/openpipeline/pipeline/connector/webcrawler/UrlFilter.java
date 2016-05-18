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

import org.openpipeline.util.URLUtils;

/**
 * Class to filter redirect URLs.
 */
public class UrlFilter {

	private String domain = null;

	/**
	 * Checks whether the URL has the same domain as the filter.
	 * 
	 * @param nextUrl
	 *            containing the URL
	 * @return true if the URL's domain matches the filter's domain, false
	 *         otherwise. If filter's domain is null returns true.
	 */
	public boolean checkRedirectUrl(UrlItem nextUrl) {

		if (domain == null) {
			return true;
		}

		if (nextUrl == null) {
			return false;
		}

		if (!domain.equals(nextUrl.getDomain())) {
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the redirect URL has the same canonical form as the
	 * original URL.
	 * 
	 * @param nextUrl
	 *            containing the URL
	 * @param originalURL
	 *            containing the original URL
	 * @return true if the redirect URL has the same canonical form as the
	 *         original URL, false otherwise
	 */
	public boolean checkCanonicalForm(String nextUrl, String originalURL) {

		if (nextUrl == null || originalURL == null) {
			return false;
		}

		String redirectURL = URLUtils.normalizeURL(nextUrl);
		String origURL = URLUtils.normalizeURL(originalURL);

		if (redirectURL == null || origURL == null) {
			return false;
		}

		if (!redirectURL.equals(origURL)) {
			return false;
		}
		return true;

	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}
