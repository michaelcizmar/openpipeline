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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpipeline.server.Server;
import org.slf4j.Logger;

/**
 * Handles the directives from robots.txt.
 * <p>
 * Implements the following crawler directives: user-agent, allow, disallow,
 * crawl-delay, crawl-rate, visit-time.
 */
public class Robots {

	private int DEFAULT_MIN_CRAWL_DELAY = 10;
	private String DEFAULT_USER_AGENT = "openpipebot";

	private Pattern[] allowedPatterns;
	private Pattern[] disallowedPatterns;

	private String userAgent = DEFAULT_USER_AGENT;
	private double crawlDelay = 0; // set in robots.txt
	private double minCrawlDelay = DEFAULT_MIN_CRAWL_DELAY; // user defined
	private int minVisitTime = 0;
	private double maxVisitTime = Double.POSITIVE_INFINITY;

	private Logger logger;

	/**
	 * Reads robots.txt file and populates allowed/disallowed directives and
	 * crawl delay.
	 * 
	 * @param file
	 *            as a String containing the contents of the robots.txt file.
	 */
	protected void readRobotsTxt(String file) {

		try {

			if (logger == null) {
				logger = Server.getServer().getLogger();
			}

			clearDirectives();
			if (file == null || file.length() == 0)
				return;

			if (userAgent != null)
				userAgent = userAgent.toLowerCase();

			boolean ignore = true; // Handle multiple user-agents
			List<Pattern> allowed = new ArrayList<Pattern>();
			List<Pattern> disallowed = new ArrayList<Pattern>();

			/*
			 * Robots.txt directives have the format "key: value" , eg
			 * "User-agent: xxx", "Allow: xxx", "Disallow: xxx"
			 */
			Pattern pattern = null;
			String[] lines = file.split("\n");
			for (String line : lines) {
				int commentIndex = line.indexOf("#");
				if (commentIndex > -1) {
					/* Strip trailing comment */
					line = line.substring(0, commentIndex);
				}
				line = line.trim().toLowerCase();
				/*
				 * Skip comments & blanks. Lines that start with a comment will
				 * be empty now
				 */
				if (line.length() == 0) {
					continue;
				}
				String[] parts = line.split(":");
				if (parts == null || parts.length != 2) {
					continue;
				}
				String name = parts[0].trim().toLowerCase();
				String value = parts[1].trim();
				if (name.length() == 0 || value.length() == 0) {
					continue;
				}
				/*
				 * Stops after reading the directives which apply to its user
				 * Agent.
				 */
				if (name.equals("user-agent") && ignore == false) {
					/*
					 * Start of directives for other user Agents, done.
					 */
					break;
				} else if (name.equals("user-agent")) {
					/*
					 * In robots.txt the directives for userAgent: * should come
					 * after all directives for specific user Agents
					 */
					if (value.equals("*") || value.equals(userAgent)) {
						ignore = false;
					} else {
						ignore = true;
					}
				} else if (ignore) {
					/* Skip directives which are not applicable */
					continue;
				} else if (name.equals("disallow") || name.equals("allow")) {
					if (value.contains("://")) {
						try {
							/* Strip the protocol and host */
							UrlItem urlItem = new UrlItem(value);
							value = urlItem.getRelativePath();
						} catch (MalformedURLException e) {
							/*
							 * Assume that protocol and host already stripped,
							 * do nothing
							 */
						}
					}
					try {
						if (value.contains("*")) {
							value = value.replaceAll("\\*", "\\.\\*");
						}
					} catch (Throwable e) {
						logger.error("Error formatting value with * = " + value
								+ ". Message = " + e.getMessage());
						continue;
					}
					try {
						pattern = Pattern.compile("^(" + value + ")");
					} catch (Throwable e) {
						logger.error("Error compiling pattern = " + value
								+ ". Message = " + e.getMessage());
						continue;
					}
					try {
						if (name.equals("allow"))
							allowed.add(pattern);
						else
							disallowed.add(pattern);
					} catch (Throwable e) {
						logger.error("Error adding pattern = "
								+ pattern.toString() + ". Directives = " + name
								+ ". Message = " + e.getMessage());
						continue;
					}
				} else if (name.equals("crawl-delay")) {
					try {
						crawlDelay = Double.parseDouble(value);
					} catch (Throwable e) {
						logger.error("Error parsing crawl delay directive = "
								+ line + ". Message = " + e.getMessage());
						crawlDelay = 0;
						continue;
					}
				} else if (name.equals("request-rate")) {
					/* The syntax is 1/5 i.e. one request per 5 seconds */
					try {
						String[] requestRate = value.split("/");
						if (requestRate != null && requestRate.length == 2) {
							String numberRequests = requestRate[0];
							String numberSeconds = requestRate[1];
							if (numberRequests.length() != 0
									& numberSeconds.length() != 0) {

								double numRequests = Double
										.parseDouble(numberRequests);
								double numSeconds = Double
										.parseDouble(numberSeconds);

								if (numRequests == 0)
									crawlDelay = 0;
								else
									crawlDelay = numSeconds / numRequests;
							}
						}
					} catch (Throwable e) {
						logger.error("Error parsing request rate directive = "
								+ line + ". Message = " + e.getMessage());
						crawlDelay = 0;
						continue;
					}
				} else if (name.equals("visit-time")) {
					/* The time is specified in UT (or GMT) time */
					String[] visitTime = value.split("-");
					if (visitTime != null && visitTime.length == 2) {
						try {
							String temp = visitTime[0].trim();
							if (temp.length() > 0) {
								minVisitTime = Integer.parseInt(temp);
							}
							temp = visitTime[1].trim();
							if (temp.length() > 0) {
								maxVisitTime = Integer.parseInt(temp);
							}
						} catch (Throwable e) {
							/* Keep the default values */
							logger
									.error("Error parsing visit time directive = "
											+ line
											+ ". Message = "
											+ e.getMessage());
							continue;
						}
					}
				}
			}
			allowedPatterns = allowed.toArray(new Pattern[allowed.size()]);
			disallowedPatterns = disallowed.toArray(new Pattern[disallowed
					.size()]);
		} catch (Throwable e) {
			if (logger != null) {
				logger.error("Error processing robots.txt. Message = "
						+ e.getMessage());
			}
			clearDirectives();
		}
	}

	/**
	 * Decides whether a URL is safe to crawl wrt allowed and disallowed
	 * directives.
	 * 
	 * @param nextUrlItem
	 *            containing the URL to be fetched
	 * @return true if the URL is allowed or null, false if not allowed or there
	 *         is an exception
	 */
	public boolean allowed(UrlItem nextUrlItem) {

		if (nextUrlItem == null) {
			return true;
		}

		/*
		 * There is a question of which directive is more specific. For example,
		 * if the includes are {"/files/include.txt"} and the excludes are
		 * {"/files/"}, the isIncluded() method will return true for
		 * "/files/include.txt", but exclude other files in the /files/
		 * directory. It can also be the case that Disallow directives are more
		 * specific than Allow. E.g., "Allow: /pictures/",
		 * "Disallow: /pictures/2008/". To resolve this, we need to look at the
		 * longest matching substring.
		 * 
		 * Wildcards do not lengthen a path -- if there's a wildcard directive
		 * path that's shorter, as written, than one without a wildcard, the one
		 * with the path spelled out will generally override the one with the
		 * wildcard. For example, "Allow: /" "Disallow: /tmp/". The spelled out
		 * path is longer for "/tmp/file.doc" wrt to "Disallow: /tmp/" then wrt
		 * to "Allow: /".
		 */

		try {
			int allowedPrefix = checkUrl(nextUrlItem, allowedPatterns);
			int disallowedPrefix = checkUrl(nextUrlItem, disallowedPatterns);

			if (disallowedPrefix > allowedPrefix) {
				return false;
			}
			return true;
		} catch (Throwable e) {

			if (logger == null) {
				logger = Server.getServer().getLogger();
			}

			logger
					.error("Exception in robots.allowed, assume not allowed for URL: "
							+ nextUrlItem.getNextUrl());
			return false;
		}
	}

	/**
	 * Matches a URL against an array of Patterns.
	 * 
	 * @param nextUrlItem
	 *            containing the URL to be matched
	 * @param directives
	 *            containing the array of patterns against which nextUrl is
	 *            matched
	 * @return the length of the longest matching substring
	 */
	private int checkUrl(UrlItem nextUrlItem, Pattern[] directives) {

		if (directives == null) {
			return 0;
		}

		String nextUrl = nextUrlItem.getRelativePath();
		if (nextUrl == null || nextUrl.length() == 0) {
			/* nextUrl is the root directory, will match "Disallow: /" */
			nextUrl = "/";
		}
		/* Need regexp for case like "Allow: /toolkit/*.html" */
		int matchingSubstringLength = 0;
		for (Pattern prefix : directives) {
			int newMatchLength = match(nextUrl, prefix);
			matchingSubstringLength = Math.max(newMatchLength,
					matchingSubstringLength);
		}
		return matchingSubstringLength;
	}

	/**
	 * Matches a URL against a Pattern using regular expressions.
	 * 
	 * @param nextUrl
	 *            containing the URL string to be matched
	 * 
	 * @param prefix
	 *            containing the pattern
	 * 
	 * @return the length of the longest matching substring
	 */
	private int match(String nextUrl, Pattern prefix) {

		if (prefix == null)
			return 0;
		Matcher matcher = prefix.matcher(nextUrl);
		if (matcher == null) {
			return 0;
		}
		if (matcher.find()) {
			/*
			 * group(1) contains the substring in the nextUrl matching the
			 * pattern
			 */
			if (matcher.groupCount() >= 1) {
				if (matcher.group(1) == null)
					return 0;
				int matchingSubstringLength = matcher.group(1).length();
				return matchingSubstringLength;
			}
		}
		return 0;
	}

	/**
	 * Checks if it is ok to crawl its domain at the current time.
	 * 
	 * @return true if it is ok and false otherwise
	 */
	public boolean visitTimeOK() {

		if (minVisitTime == 0 || maxVisitTime == Double.POSITIVE_INFINITY)
			return true;

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		int min = calendar.get(Calendar.MINUTE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		/* Concatenate hours and min to get the format hhmm */
		int currentTime = hour * 100 + min;

		if (currentTime > minVisitTime && currentTime < maxVisitTime) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether the minimum crawl delay is observed.
	 * 
	 * @param lastFetchTimeThisDomain
	 *            containing the last time a request was submitted to the
	 *            current domain
	 * 
	 * @return the time in milliseconds till the next request can be safely
	 *         issued
	 */
	public long crawlDelayTime(long lastFetchTimeThisDomain) {

		if (lastFetchTimeThisDomain == 0)
			return 0;
		long currentTime = System.currentTimeMillis();
		long diff = (currentTime - lastFetchTimeThisDomain);

		if (diff < crawlDelay) {
			return diff;
		}
		return 0;
	}

	/**
	 * Clears the allowed and disallowed Patterns and resets other parameters.
	 */
	public void clearDirectives() {

		allowedPatterns = null;
		disallowedPatterns = null;
		minVisitTime = 0;
		maxVisitTime = Double.POSITIVE_INFINITY;
		crawlDelay = minCrawlDelay;
	}

}
