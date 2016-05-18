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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Fetcher fetches the content of the web page.
 */
public class Fetcher {

	private int DEFAULT_MAX_NUMBER_OF_REDIRECTS = 5;
	private int DEFAULT_NUMBER_OF_FETCHATTEMPTS = 5;
	public static int DEFAULT_TIME_OUT_TIME = 6000;
	private int DEFAULT_MAX_FILE_SIZE = 5242880;

	private DefaultHttpClient client;
	private UrlFilter urlFilter;
	private Robots robotsDirectives;
	private String redirectUrl;
	private byte[] inputStream;
	private byte[] dataBuffer;
	private XMLConfig params;
	private long lastModified;
	private SimpleDateFormat format = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz");

	private int maxNumberOfRedirects = DEFAULT_MAX_NUMBER_OF_REDIRECTS;
	private int maxNumberOfFetchAttempts = DEFAULT_NUMBER_OF_FETCHATTEMPTS;
	private long lastFetchTimeThisDomain;
	private int maxFileSize = DEFAULT_MAX_FILE_SIZE;

	Logger logger;
	boolean debug;

	/**
	 * Fetches the data associated with the next URL.
	 * 
	 * @param nextUrlItem
	 *            containing the next URL to crawl
	 * 
	 * @return action based on the status code of the HttpClient, robots safety
	 *         check, max number of redirects, max number of fetch attempts
	 */
	public int fetch(LinkDBRecord nextUrlItem) {

		if (nextUrlItem == null) {
			return HttpResultMapper.ACTION_DELETE;
		}

		if (client == null) {
			throw new RuntimeException("Fetcher not initialized.");
		}

		String nextUrl = nextUrlItem.getNextUrl();
		redirectUrl = nextUrl;
		HttpResponse httpResponse = null;
		HttpGet get = null;
		lastModified = 0;

		try {
			/*
			 * Check the compliance with allow/disallow directives.
			 */
			UrlItem item = new UrlItem(nextUrlItem.getNextUrl());

			boolean robotsSafe = robotsDirectives.allowed(item);
			if (!robotsSafe) {
				if (debug) {
					logger.debug("Robots denied, next URL: " + nextUrl);
				}
				return HttpResultMapper.ACTION_DELETE;
			}
			/*
			 * Check the compliance with visit-time directives.
			 */
			boolean visitTimeSafe = robotsDirectives.visitTimeOK();
			if (!visitTimeSafe) {
				if (debug) {
					logger.debug("Robots visit time denied, next URL: "
							+ nextUrl);
				}
				return HttpResultMapper.ACTION_SKIP;
			}

			int status = -1;
			int numRedirects = 0;

			while (true) {

				get = new HttpGet();

				/* Set uri for the next execution of get */
				URI uri = new URI(nextUrl);
				get.setURI(uri);

				/*
				 * Check crawl delay. If the fetcher follows the redirect URL it
				 * will also observe the crawl delay
				 */
				long waitTime = robotsDirectives
						.crawlDelayTime(lastFetchTimeThisDomain);

				if (waitTime > 0) {
					try {
						Thread.sleep(waitTime);
					} catch (InterruptedException e) {
						logger
								.error("Exception in fetcher in thread.sleep, next URL: "
										+ nextUrl
										+ ". Message: "
										+ e.getMessage());
					}
				}

				/* Execute get method */
				DefaultRedirectHandler redirectHandler = new DefaultRedirectHandler();
				client.setRedirectHandler(redirectHandler);

				HttpContext localContext = new BasicHttpContext();

				httpResponse = client.execute(get, localContext);
				if (httpResponse == null) {
					break;
				}

				Header lastModHeader = httpResponse
						.getFirstHeader("last-modified");
				if (lastModHeader != null) {
					String lastModifiedDate = lastModHeader.getValue();
					Date date = format.parse(lastModifiedDate);
					lastModified = date.getTime();
				}

				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine == null) {
					/* Should not happen after execute */
					status = -1;
				} else {
					status = httpResponse.getStatusLine().getStatusCode();
				}

				lastFetchTimeThisDomain = System.currentTimeMillis();

				HttpEntity entity = httpResponse.getEntity();

				if (HttpResultMapper.permanentRedirect(status)
						|| HttpResultMapper.temporaryRedirect(status)) {
					/*
					 * The fetcher follows a redirect until the maximum number
					 * of redirects is reached.
					 */
					if (numRedirects == maxNumberOfRedirects) {
						break;
					}

					/* Update the URL to be fetched */
					URI redirectURI = redirectHandler.getLocationURI(
							httpResponse, localContext);

					String newUrl = redirectURI.toString();
					numRedirects++;

					/*
					 * In case of a permanent redirect, the fetcher asks the URL
					 * filter whether to follow it or not. The fetcher follows
					 * all temporary redirects.
					 */
					if (HttpResultMapper.permanentRedirect(status)) {

						boolean redirectUrlOK = urlFilter.checkCanonicalForm(
								newUrl, nextUrl);

						/*
						 * Only follows the permanent redirects which are
						 * different because of the formatting to the canonical
						 * form such as removing the trailing slash
						 */
						if (!redirectUrlOK) {
							/* Permanent redirect, keep the redirect URL */
							redirectUrl = newUrl;
							break;
						}
					}
					/*
					 * If the permanent redirect URL differs just in formatting,
					 * or if temporary redirect follow it.
					 * 
					 * The redirect URL becomes nextURL for the next iteration
					 * of the while loop.
					 */
					nextUrl = newUrl;

					if (debug) {
						logger.debug("Fetcher: had a redirect, redirect URL: "
								+ nextUrl + ", status: " + status);
					}
				} else {
					/*
					 * get's responseBody contains data if success and is null
					 * otherwise
					 */
					// TODO retry if
					// exception?

					if (entity != null) {

						long inputSize = entity.getContentLength();

						if (inputSize > 0 && inputSize >= maxFileSize) {
							throw new RuntimeException(
									"Fetcher exception: data exceeds the max file size.");
						}
						/* Often the data length is not known */
						inputStream = getData(entity);
					}
					break;
				}
				/*
				 * Need to release the current connection, otherwise client does
				 * not work
				 */
				entity.consumeContent();
			}

			/*
			 * Decide on action after the while loop is done, possibly done with
			 * redirects
			 */
			int action = HttpResultMapper.getHttpCodeResult(status);

			int fetchAttempts = nextUrlItem.getFetchAttempts();
			if (action != HttpResultMapper.ACTION_FINALIZE
					&& fetchAttempts == maxNumberOfFetchAttempts) {
				/*
				 * Remove items which have too many fetch attempts: redirects,
				 * skip etc
				 */
				action = HttpResultMapper.ACTION_DELETE;
			} else if (numRedirects == maxNumberOfRedirects) {
				/*
				 * Avoid following too many redirects
				 */
				action = HttpResultMapper.ACTION_DELETE;
			}

			return action;

		} catch (Throwable e) {
			/*
			 * Currently, no re-tries are implemented. The HttpClient
			 * automatically tries to recover from safe exceptions.
			 */

			if (e instanceof org.apache.http.conn.ConnectTimeoutException) {
				return HttpResultMapper.ACTION_SKIP;
			}
			return HttpResultMapper.ACTION_DELETE;
		} finally {
			if (get != null) {
				get.abort();
			}
		}
	}

	/**
	 * Gets data from robots.txt.
	 */
	public void loadRobotsDirectives(String domain) {

		try {

			String robotsFile = domain + "/robots.txt";
			LinkDBRecord record = new LinkDBRecord(robotsFile);

			robotsDirectives.clearDirectives();
			int action = fetch(record);

			if (action == HttpResultMapper.ACTION_FINALIZE) {

				String file = new String(getInputStream());
				robotsDirectives.readRobotsTxt(file);

			} else {
				logger.warn("No robots file for domain: " + domain);
			}
		} catch (Throwable e) {

			robotsDirectives.clearDirectives();
			logger
					.error("Error reading robots file: " + domain
							+ "/robots.txt");
		}
	}

	private byte[] getData(HttpEntity entity) throws IllegalStateException,
			IOException {

		InputStream input = entity.getContent();
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		/* Read bytes into the dataBuffer */
		int len = 0;
		int count = 0;
		int size = dataBuffer.length;

		len = input.read(dataBuffer, 0, size);
		while (len != -1) {

			if (count >= size) {
				input.close();
				throw new RuntimeException(
						"Fetcher exception: data exceeds the max file size.");
			}

			output.write(dataBuffer, 0, len);
			len = input.read(dataBuffer, 0, size);

			count += len;
		}
		input.close();

		/* Get data as a byte[] */
		return output.toByteArray();
	}

	public void setUrlFilter(UrlFilter urlFilter) {
		this.urlFilter = urlFilter;
	}

	public byte[] getInputStream() {
		return inputStream;
	}

	public void setInputStream(byte[] input) {
		inputStream = input;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public long getLastFetchTimeThisDomain() {
		return lastFetchTimeThisDomain;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public void setLastFetchTimeThisDomain(long lastFetchTimeThisDomain) {
		this.lastFetchTimeThisDomain = lastFetchTimeThisDomain;
	}

	/**
	 * Updates robotsDirectives and urlFilter for the new domain.
	 * 
	 * @param domain
	 *            containing the new domain
	 * @throws RuntimeException
	 *             if fetcher not initialized
	 */
	public void updateDomain(String domain) throws RuntimeException {

		if (robotsDirectives == null || urlFilter == null) {
			throw new RuntimeException("Fetcher not initialized.");
		}

		loadRobotsDirectives(domain);
		urlFilter.setDomain(domain);
	}

	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Initializes the httpClient, getMethod, robotsDirectives and urlFilter.
	 * 
	 * @throws RuntimeException
	 */
	public void initialize() throws RuntimeException {

		if (params == null) {
			throw new RuntimeException("Params not set for fetcher.");
		}

		client = new DefaultHttpClient();
		int timeout = params.getIntProperty("timeout", DEFAULT_TIME_OUT_TIME);
		client.getParams().setParameter("http.socket.timeout", timeout);
		/*
		 * Initialize redirects parameters. Since fetcher handles redirects
		 * manually, followRedirects for get should be "false", and client's
		 * max-redirects set to 1
		 */
		client.getParams().setParameter("http.protocol.max-redirects", 1);
		client.getParams().setBooleanParameter(
				"http.protocol.handle-redirects", false);

		robotsDirectives = new Robots();

		urlFilter = new UrlFilter();
		dataBuffer = new byte[maxFileSize];

		lastModified = 0;

		logger = Server.getServer().getLogger();
		debug = Server.getServer().getDebug();

	}

	public void setHandleRedirects(boolean handleRedirects) {
		client.getParams().setBooleanParameter(
				"http.protocol.handle-redirects", handleRedirects);
	}

	public void setMaxRedirects(int maxRedirects) {
		client.getParams().setParameter("http.protocol.max-redirects",
				maxRedirects);
	}

	/**
	 * Sets parameters.
	 */
	public void setParams(XMLConfig params) {
		this.params = params;
	}
}
