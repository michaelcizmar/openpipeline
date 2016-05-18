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
package org.openpipeline.pipeline.docfilter;

import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openpipeline.html.HTMLLexer;
import org.openpipeline.html.HTMLUtils;
import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.StandardAttributeNames;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.ByteArray;
import org.openpipeline.util.FastStringBuffer;

/**
 * Implementation of the {@link DocFilter} class for HTML files
 */
public class HTMLFilter extends DocFilter {

	static private final byte[] CHARSET_BYTES = "charset=".getBytes();

	private FastStringBuffer generalText = new FastStringBuffer();
	private FastStringBuffer titleText = new FastStringBuffer();
	// Metatags can be multivalued
	private HashMap<String, List<String>> metaNameHash = new HashMap<String, List<String>>();
	private HashMap metaHttpEquivHash = new HashMap();
	private ArrayList hrefLink = new ArrayList();
	private ArrayList frameLink = new ArrayList();
	private HashMap tagHash = new HashMap();

	private String baseURLStr;
	private URL baseURL;

	@Override
	public void processItem(Item item) throws PipelineException {

		try {

			clearBuffers();

			try {
				this.baseURL = new URL(baseURLStr);
			} catch (MalformedURLException e) {
				this.baseURL = null;
			}

			DocBinary docBinary = item.getDocBinary();
			InputStream inputStream = docBinary.getInputStream();
			
				int size = (int) docBinary.getSize();
				if (size <= 0) {
					size = 100 * 1024;// guess
				}
				ByteArray arr = new ByteArray(size);
				arr.append(inputStream);
			

			InputStreamReader isr;
			String cs = extractCharset(arr);
			if (cs == null) {
				isr = new InputStreamReader(arr);
			} else {
				isr = new InputStreamReader(arr, cs);
			}

			populateBuffers(isr);

			copyBuffersToItem(item, docBinary);

		} catch (Throwable t) {
			throw new PipelineException(t);
		}

		if (nextStage != null) {
			nextStage.processItem(item);
		}
	}

	/**
	 * Copied the data in the buffers above into the item. Creates tags as
	 * needed.
	 * 
	 * @param item
	 * @param docBinary
	 */
	private void copyBuffersToItem(Item item, DocBinary docBinary) {

		Node rootNode = item.getRootNode();

		if (titleText.size() > 0) {
			rootNode.addNode(StandardAttributeNames.TITLE,
					titleText.getArray(), 0, titleText.size());
		}

		if (generalText.size() > 0) {
			rootNode.addNode(StandardAttributeNames.TEXT,
					generalText.getArray(), 0, generalText.size());
		}

		String encoding = docBinary.getEncoding();
		if (encoding != null) {
			rootNode.addNode(StandardAttributeNames.ENCODING, encoding);
		}

		Iterator<Entry<String, List<String>>> it = getMetaNameTags().entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<String>> entry = it.next();
			String attributeId = (String) entry.getKey();
			List<String> values = entry.getValue();
			// Insert all values
			for (String value : values) {
				rootNode.addNode(attributeId, value);
			}
		}

		// if (baseURLStr != null) {
		// rootNode.addNode(StandardAttributeNames.URL, baseURLStr);
		// }
	}

	/**
	 * Using the input stream, populate all the data structured defined at the
	 * top of this class.
	 * 
	 * @throws IOException
	 */
	private void populateBuffers(Reader inReader) throws IOException {

		HTMLLexer lexer = new HTMLLexer(inReader);

		FastStringBuffer matchedText = new FastStringBuffer();
		FastStringBuffer currentBuffer = generalText;

		boolean noIndex = false;

		DONE: while (true) {
			int lexResult = lexer.lex(matchedText);

			/**
			 * implements <NOINDEX>, </NOINDEX> directives
			 */
			switch (lexResult) {
			case HTMLLexer.EOF:
				break DONE;
			case HTMLLexer.STARTNOINDEX:
				noIndex = true;
				break;
			case HTMLLexer.ENDNOINDEX:
				noIndex = false;
				break;
			default:
				// do nothing
			}
			if (noIndex)
				continue;

			switch (lexResult) {
			case HTMLLexer.TEXT:
				if (currentBuffer.size() > 0) {
					char ch = currentBuffer.charAt(currentBuffer.size() - 1);
					if (!Character.isWhitespace(ch)) {
						currentBuffer.append(' ');
					}
				}
				currentBuffer.append(matchedText);
				break;
			case HTMLLexer.WHITESPACE:
				// compress all whitespace down to a single space
				if ((currentBuffer.size() > 0)
						&& (currentBuffer.charAt(currentBuffer.size() - 1) != ' ')) {
					currentBuffer.append(' ');
				}
				break;
			case HTMLLexer.STARTTITLE:
				currentBuffer = titleText;
				break;
			case HTMLLexer.ENDTITLE:
				currentBuffer = generalText;
				break;
			case HTMLLexer.META:
				parseTags(matchedText, tagHash);
				String content = (String) tagHash.get("content");
				if (content == null)
					break;
				String name = (String) tagHash.get("name");
				if (name != null) {
					addToMetaNameHash(name.toLowerCase(), content);
					break;
				}
				String httpequiv = (String) tagHash.get("http-equiv");
				if (httpequiv != null) {
					httpequiv = httpequiv.toLowerCase();
					metaHttpEquivHash.put(httpequiv, content);

					// the http-equiv may contain a meta tag in the following
					// form:
					// Content-Type: text/html;charset=euc-jp
					// if (httpequiv.equals("content-type"))
					// setEncodingFromMetaTag(content);
				}
				break;
			case HTMLLexer.IMG:
				parseTags(matchedText, tagHash);
				String altText = (String) tagHash.get("alt");
				if (altText != null) {
					currentBuffer.append(' ');
					currentBuffer.append(altText);
				}
				break;
			case HTMLLexer.STARTANCHOR:
				parseTags(matchedText, tagHash);
				String href = (String) tagHash.get("href");
				href = putLinkInContext(href, baseURL);
				if (href != null)
					hrefLink.add(href);
				break;
			case HTMLLexer.FRAME:
				parseTags(matchedText, tagHash);
				String framesrc = (String) tagHash.get("src");
				if (framesrc != null)
					frameLink.add(framesrc);
				break;
			case HTMLLexer.NBSP: // do nothing
				break;
			case HTMLLexer.AMP:
				currentBuffer.append('&');
				break;
			case HTMLLexer.GT:
				currentBuffer.append('>');
				break;
			case HTMLLexer.LT:
				currentBuffer.append('<');
				break;
			case HTMLLexer.QUOT:
				currentBuffer.append('"');
				break;
			case HTMLLexer.APOS:
				currentBuffer.append('\'');
				break;
			case HTMLLexer.SYMBOL_DECIMAL:
				currentBuffer.append(HTMLUtils
						.convertSymbolDecimal(matchedText));
				break;
			case HTMLLexer.SYMBOL_HEX:
				currentBuffer.append(HTMLUtils.convertSymbolHex(matchedText));
				break;
			// case HTMLLexer.SYMBOL_CHAR:
			// currentBuffer.append(HTMLUtils.convertSymbolChar(matchedText));
			// break;
			case HTMLLexer.CHAR:
				currentBuffer.append(matchedText.charAt(0));
				break;
			case HTMLLexer.BASE:
				parseTags(matchedText, tagHash);
				try {
					baseURL = new URL((String) tagHash.get("href"));
				} catch (MalformedURLException e) {
				} // ignore errors
				break;
			default:
				// character entities are returned as 100000 + the entity value
				if (lexResult > 100000) {
					char entity = (char) (lexResult - 100000);
					currentBuffer.append(entity);
				}
				// everything else ignored, including unrecognized tags
			}
		}

	}

	private void addToMetaNameHash(String name, String content) {

		List<String> values = metaNameHash.get(name);

		if (values == null) {
			values = new ArrayList<String>();
			metaNameHash.put(name, values);
		}

		values.add(content);
	}

	private void clearBuffers() {
		generalText.clear();
		titleText.clear();
		metaNameHash.clear();
		metaHttpEquivHash.clear();
		hrefLink.clear();
		frameLink.clear();
		tagHash.clear();
		// baseURLStr = null;//TODO
		// baseURL = null;//TODO
	}

	private void parseTags(FastStringBuffer buf, HashMap hashMap)
			throws IOException {
		String tag = buf.toString();
		TagLexer tagLexer = new TagLexer(new StringReader(tag));
		hashMap.clear();
		tagLexer.parse(hashMap);
	}

	/**
	 * Converts a relative link to an absolute URL. Uses the mainURL as the
	 * context. If a <base href="http://mydomain.com"> tag exists, uses that for
	 * context instead.
	 * 
	 * @param link
	 *            the link to convert
	 * @return an absolute URL, or the unchanged link if the base URL is bad.
	 */
	private String putLinkInContext(String link, URL baseURL) {
		if (link == null || link.length() == 0)
			return null;
		URL url = null;
		try {
			url = new URL(baseURL, link);
		} catch (MalformedURLException e) {
			return link; // baseURL probably bad; just return the link
		}

		return url.toString();
	}

	private String extractCharset(ByteArray binary) {
		byte[] buf = binary.getArray();
		int size = binary.size();

		int start = findBytes(CHARSET_BYTES, buf, size);
		if (start == -1)
			return null;
		start += CHARSET_BYTES.length;
		int end = start;
		while (end < size) {
			byte b = buf[end];
			// look for the first space, ;, >, or "
			if (b == 32 || b == 59 || b == 62 || b == 34) {
				break;
			}
			end++;
		}

		String encoding = null;
		try {
			encoding = new String(buf, start, end - start, "iso-8859-1");
			// this is just a test to see if the encoding is legitimate
			new String(CHARSET_BYTES, encoding);
		} catch (UnsupportedEncodingException e) {
			encoding = null;
		}
		return encoding;
	}

	/**
	 * Return the starting offset of sub in buf.
	 */
	private int findBytes(byte[] sub, byte[] buf, int bufSize) {
		if (sub.length > bufSize)
			return -1;

		for (int i = 0; i < (bufSize - sub.length); i++) {
			boolean found = true;
			for (int j = 0; j < sub.length; j++) {
				if (sub[j] != buf[i + j]) {
					found = false;
					break;
				}
			}
			if (found)
				return i;
		}
		return -1;
	}

	/**
	 * Returns a Map containing the name/value pairs found in meta tags in the
	 * form &lt;meta name="name" content="value"&gt;. Not part of the standard
	 * DocFilter interfacet
	 * 
	 * @return a Map with the values, if any.
	 */
	public Map<String, List<String>> getMetaNameTags() {
		return metaNameHash;
	}

	/**
	 * Used to resolve relative links in the document. Not part of the standard
	 * DocFilter interface. IMPORTANT: call this method *after* you call
	 * setDocBinary();
	 * 
	 * @param url
	 *            the URL of the page we're parsing
	 */
	public void setBaseURL(String url) {
		this.baseURLStr = url;
	}

	@Override
	public String[] getDefaultExtensions() {
		String[] exts = { "htm", "html", "asp", "jsp", "phtml", "shtml", "stm",
				"do" };
		return exts;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		String[] mimeTypes = { "text/html" };
		return mimeTypes;
	}

	@Override
	public String getDescription() {
		return "Parses html documents";
	}

	@Override
	public String getDisplayName() {
		return "HTMLFilter";
	}

	/**
	 * Returns links found in &lt;a href="link"&gt; tags. Links will be
	 * converted from relative to absolute: if a page contains a relative link
	 * like "anotherpage.htm", the full URL path will be added so it is returned
	 * as "http://mysite.com/anotherpage.htm". Only correctly-formatted URLs
	 * will be returned.
	 * 
	 * @return an ArrayList of links, where each link is a String. An empty
	 *         ArrayList is returned if there are no links.
	 */
	public ArrayList getLinks() {
		return hrefLink;
	}

	@Override
	public String getDocType() {
		return "html";
	}

}
