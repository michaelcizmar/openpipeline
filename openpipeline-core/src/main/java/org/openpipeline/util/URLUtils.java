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
package org.openpipeline.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;



/**
 * URL Utilities
 */
public class URLUtils {
	
	private static String [] urlChars = new String [128];
	
	static {
		/*
		 * Create an array of URL encoded strings for chars under 128.
		 * If no encoding necessary, then the entry is null.
		 * All this overhead is incurred only once on startup
		 */
		for (char ch = 0; ch < urlChars.length; ch++) {
			String encoded = null;
			try {
				encoded = URLEncoder.encode(Character.toString(ch), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e); // will never happen
			}
			
			if (encoded.length() > 1 || encoded.charAt(0) != ch) {
				urlChars[ch] = encoded;
			}
		}
		// we prefer %20 to the default '+' because it's more portable
		urlChars[' '] = "%20"; 
	}
	
	
	/**
	 * URLEncode the input and append it to the output. This method is far more efficient
	 * than java.net.URLEncoder.
	 */
	public static void urlEncode(CharSequence input, FastStringBuffer output) {

		/* java.net.URLEncoder forces the creation of strings at runtime, which is 
		 * exceptionally bad design.
		 * This method avoids that overhead for ASCII characters.
		 */
		int len = input.length();
		for (int i = 0; i < len; i++) {
			char ch = input.charAt(i);
			if (ch >= 128) {
				// getting here will be very rare
				try {
					output.append(URLEncoder.encode(Character.toString(ch), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e); // will never happen
				}
				
			} else {
				String encodedChar = urlChars[ch];
				if (urlChars[ch] == null) {
					output.append(ch);
				} else {
					output.append(encodedChar);
				}
			}
		}
	}
	
	
    /**
     * Put the URL into a standard form
     */
    public static String normalizeURL(String urlStr) {
		try {
			URL url = new URL(urlStr);
			String protocol = url.getProtocol();
			if (protocol != null)
			    protocol = protocol.toLowerCase();
			String host = url.getHost();
			if (host != null)
			    host = host.toLowerCase();
			int port = url.getPort();
			
			String path = url.getPath();
			FastStringBuffer pathBuf = new FastStringBuffer();
			if (path != null && path.length() > 0) {
			    pathBuf.append(path);
			    // strip trailing slash
			    if (pathBuf.charAt(pathBuf.size() - 1) == '/') {
			        pathBuf.setSize(pathBuf.size() - 1);
			    }
			    pathBuf.replace(" ", "%20");
			    // path = URLUtils.encode(pathBuf).toString(); messes up
			}
			
			String query = url.getQuery();
			if (query != null && query.length() > 0) {
			    FastStringBuffer queryBuf = new FastStringBuffer(query.length() + 10);
			    queryBuf.append(query);
			    queryBuf.replace(" ", "%20");
			    //query = URLUtils.encode(queryBuf).toString(); messes up
			    pathBuf.append('?');
			    pathBuf.append(queryBuf);
			}

			URL newURL = new URL(protocol, host, port, pathBuf.toString());
			
			urlStr = newURL.toString();
			
		} catch (MalformedURLException e) {
			return null; // don't add bad urls
		}
        
        return urlStr;
    }
}
