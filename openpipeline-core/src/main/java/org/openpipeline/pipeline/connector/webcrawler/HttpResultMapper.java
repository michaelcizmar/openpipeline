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

/**
 * Maps the httpClient status codes from the fetcher to the actions to be taken
 * by workerThread.*
 */
public class HttpResultMapper {

	public static final int ACTION_SKIP = 1;
	public static final int ACTION_DELETE = 2;
	public static final int ACTION_FINALIZE = 3;
	public static final int ACTION_PERMANENT_REDIRECT = 4;
	public static final int ACTION_TEMPORARY_REDIRECT = 5;
	public static int[] actions = new int[1000];

	static {
		actions[100] = ACTION_DELETE;// 100 (Continue) The requestor should
		// continue with the request. The server
		// returns this code to indicate that it
		// has received the first part of a
		// request and is waiting for the rest.
		actions[101] = ACTION_DELETE;// 101 (Switching protocols) The requestor
		// has asked the server to switch
		// protocols and the server is
		// acknowledging that it will do so.
		actions[200] = ACTION_FINALIZE;// 200 (Successful) The server
		// successfully processed the request.
		// Generally, this means that the server
		// provided the requested page.
		actions[201] = ACTION_DELETE;// 201 (Created) The request was successful
		// and the server created a new resource.
		actions[202] = ACTION_DELETE;// 202 (Accepted) The server has accepted
		// the request, but hasn't yet processed
		// it.
		actions[203] = ACTION_FINALIZE; // 203 (Non-authoritative information)
		// The server successfully processed the
		// request, but is returning information
		// that may be from another source.
		actions[204] = ACTION_DELETE;// 204 (No content) The server successfully
		// processed the request, but isn't
		// returning any content.
		actions[205] = ACTION_DELETE;// 205 (Reset content) The server
		// successfully processed the request,m
		// but isn't returning any content.
		// Unlike a 204 response, this response
		// requires that the requestor reset the
		// document view (for instance, clear a
		// form for new input).
		actions[206] = ACTION_FINALIZE;// 206 (Partial content) The server
		// successfully processed a partial GET
		// request.
		actions[300] = ACTION_PERMANENT_REDIRECT;// 300 (Multiple choices) The
		// server has
		// several actions available based on
		// the request. The server may choose an
		// action based on the requestor (user
		// agent) or the server may present a
		// list so the requestor can choose an
		// action.
		actions[301] = ACTION_PERMANENT_REDIRECT;// 301 (Moved permanently) The
		// requested
		// page has been permanently moved to a
		// new location. When the server returns
		// this response (as a response to a GET
		// or HEAD request), it automatically
		// forwards the requestor to the new
		// location.
		actions[302] = ACTION_TEMPORARY_REDIRECT; // 302 (Moved temporarily) The
		// server is
		// currently responding to the request
		// with a page from a different
		// location, but the requestor should
		// continue to use the original location
		// for future requests. This code is
		// similar to a 301 in that for a GET or
		// HEAD request, it automatically
		// forwards the requestor to a different
		// location.
		actions[303] = ACTION_PERMANENT_REDIRECT;// 303 (See other location) The
		// server
		// returns this code when the requestor
		// should make a separate GET request to
		// a different location to retrieve the
		// response. For all requests other than
		// a HEAD request, the server
		// automatically forwards to the other
		// location.
		actions[304] = ACTION_FINALIZE;// 304 (Not modified) The requested page
		// hasn't been modified since the last
		// request. When the server returns this
		// response, it doesn't return the
		// contents of the page.
		actions[305] = ACTION_DELETE;// 305 (Use proxy) The requestor can only
		// access the requested page using a
		// proxy. When the server returns this
		// response, it also indicates the proxy
		// that the requestor should use.
		actions[307] = ACTION_TEMPORARY_REDIRECT;// 307 (Temporary redirect) The
		// server is
		// currently responding to the request
		// with a page from a different
		// location, but the requestor should
		// continue to use the original location
		// for future requests. This code is
		// similar to a 301 in that for a GET or
		// HEAD request, it automatically
		// forwards the requestor to a different
		// location.
		actions[400] = ACTION_DELETE;// 400 Bad Request, malformed syntax
		actions[401] = ACTION_DELETE;// 401 Unauthorized, provide authentication
		// information
		actions[403] = ACTION_DELETE;// 403 Forbidden, do not repeat
		actions[404] = ACTION_DELETE;// 404 Not Found
		actions[405] = ACTION_DELETE;// 405 Method not Allowed
		actions[406] = ACTION_DELETE;// 406 Not Acceptable
		actions[407] = ACTION_DELETE;// 407 Proxy Authentication Required
		actions[408] = ACTION_SKIP;// 408 Request Timeout
		actions[409] = ACTION_DELETE;// 409 Conflict, possibly resubmit
		actions[410] = ACTION_DELETE;// 410 Gone, permanent condition
		actions[411] = ACTION_DELETE;// 411 Length Required
		actions[412] = ACTION_DELETE;// 412 Precondition Failed (change request
		// headers)
		actions[413] = ACTION_DELETE;// 413 Requested Entity too Large
		actions[414] = ACTION_DELETE;// 414 Request-URI too Long
		actions[415] = ACTION_DELETE;// 415 Unsupported Media Type
		actions[416] = ACTION_DELETE;// 416 Request Range not Satisfiable
		actions[417] = ACTION_DELETE;// 417 Expectation Failed
		actions[500] = ACTION_SKIP;// Internal Server Error. A generic error
		// message, given when no more specific
		// message is suitable.
		actions[501] = ACTION_DELETE;// Not Implemented
		actions[502] = ACTION_DELETE;// 502 Bad Gateway
		actions[503] = ACTION_SKIP;// 503 Service Unavailable, The server is
		// currently unable to handle the request
		// due to a temporary overloading or
		// maintenance of the server. The
		// implication is that this is a temporary
		// condition which will be alleviated after
		// some delay. If known, the length of the
		// delay MAY be indicated in a Retry-After
		// header. If no Retry-After is given, the
		// client SHOULD handle the response as it
		// would for a 500 response.
		actions[504] = ACTION_SKIP;// 504 Gateway Timeout
		actions[505] = ACTION_DELETE;// 505 HTTP Version Not Supported
	}

	/**
	 * Maps the HttpClient status code to an action.
	 * 
	 * @param httpCode
	 *            containing the HttpClient status code
	 * 
	 * @return the action, -1 if the status code is not found in the map
	 */
	public static int getHttpCodeResult(int httpCode) {

		if (httpCode >= 0 && httpCode < actions.length) {
			int action = actions[httpCode];
			if (action == 0) {
				return -1;
			}
			return action;
		}
		return -1;
	}

	public static boolean permanentRedirect(int httpCode) {

		int action = getHttpCodeResult(httpCode);
		if (action == ACTION_PERMANENT_REDIRECT) {
			return true;
		}
		return false;
	}

	public static boolean temporaryRedirect(int httpCode) {

		int action = getHttpCodeResult(httpCode);
		if (action == ACTION_TEMPORARY_REDIRECT) {
			return true;
		}
		return false;
	}
}
