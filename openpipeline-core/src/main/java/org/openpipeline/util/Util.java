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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;


/**
 * A class that contains general utility methods. All methods in Util are static.
 */
public class Util {

	// prevent instantiation
	private Util() {
	}

	/**
	 * Return a string which converts the milliseconds value in the elapsed
	 * parameter to day/hours/minutes/seconds.
	 */
	public static String getFormattedElapsedTime(float elapsed) {
		FastStringBuffer buf = new FastStringBuffer();
		elapsed = elapsed / (24 * 60 * 60 * 1000);
		int days = (int) elapsed;
		elapsed -= days;
		elapsed *= 24;
		int hours = (int) elapsed;
		elapsed -= hours;
		elapsed *= 60;
		int minutes = (int) elapsed;
		elapsed -= minutes;
		elapsed *= 60;
		int seconds = (int) elapsed;
		elapsed -= seconds;
		elapsed *= 1000;
		int milliseconds = (int) elapsed;

		if (days > 0)
			buf.append(days + " days ");
		if (hours > 0)
			buf.append(hours + " hours ");
		if (minutes > 0)
			buf.append(minutes + " minutes ");
		if (seconds > 0)
			buf.append(seconds + " seconds ");
		if (milliseconds > 0)
			buf.append(milliseconds + " ms ");
		if (buf.size() == 0)
			buf.append("0 ms");
		return buf.toString();
	}

	/**
	 * Format a number of bytes into kilobytes, megabytes, etc., 
	 * to one decimal place.
	 */
	public static String getFormattedDataSize(long num) {
		if (num < 1024) {
			return num + " bytes";
		}

		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(1);

		if (num < 1024 * 1024) {
			float f = (float) num / 1024;
			return nf.format(f) + " KB";
		}

		if (num < 1024 * 1024 * 1024) {
			float f = (float) num / (1024 * 1024);
			return nf.format(f) + " MB";
		}

		float f = (float) num / (1024 * 1024 * 1024);
		return nf.format(f) + " GB";
	}

	/**
	 * Extract error information from a request and format it into a
	 * human-readable string. Useful for formatting JSP error pages.
	 * @param request the request object that gets passed to a JSP error page.
	 * @param includeStackTrace if true, the stack trace will be included in the output
	 * @return an html-formatted string with the error information
	 */
	static public String formatError(HttpServletRequest request, boolean includeStackTrace) {

		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<table>");

		Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
		buf.append("<tr valign=top><td>Status code:</td><td>" + statusCode + "</td></tr>");

		String pageRequested = (String) request.getAttribute("javax.servlet.error.request_uri");
		String query = request.getQueryString();
		if (query != null) {
			pageRequested += "?" + query;
		}
		buf.append("<tr valign=top><td>Page:&nbsp;&nbsp;&nbsp;</td><td>" + pageRequested + "</td></tr>");

		Throwable t = (Throwable) request.getAttribute("javax.servlet.error.exception");

		buf.append("<tr valign=top><td>Exception:</td><td>" + t + "</td></tr>");

		if (includeStackTrace && t != null) {
			buf.append("<tr valign=top><td>Stack Trace:</td><td>");
			CharArrayWriter cw = new CharArrayWriter();
			PrintWriter pw = new PrintWriter(cw);
			t.printStackTrace(pw);
			char[] arr = cw.toCharArray();
			FastStringBuffer tmpbuf = new FastStringBuffer(arr, 0, arr.length);
			tmpbuf.replace("\r", "<br>");

			buf.append(tmpbuf);

			StackTraceElement[] elements = t.getStackTrace();
			for (int i = 0; i < elements.length; i++) {
				buf.append(elements[i].toString());
				buf.append("<br>");
			}

			buf.append("</td></tr>");
		}
		buf.append("</table>");
		return buf.toString();
	}

	/**
	 * Trim leading and trailing whitespace from the contents of this StringBuilder object.
	 * @param buf the buffer containing the text to trim.
	 */
	static public void trimWhitespace(StringBuilder buf) {

		int len = buf.length();
		int start = 0;
		// find 1st non-whitespace char
		for (int i = 0; i < len; i++) {
			if (!Character.isWhitespace(buf.charAt(i))) {
				start = i;
				break;
			}
		}
		if (start > 0) {
			buf.delete(0, start);
		}

		int end = buf.length() - 1;
		while (end >= 0) {
			if (Character.isWhitespace(buf.charAt(end))) {
				buf.setLength(end);
				end--;
			} else {
				break;
			}
		}
	}

	/**
	 * Test to see if the specified section of the char array is
	 * entirely whitespace
	 * @param ch the array to test
	 * @param start the starting offset in the array
	 * @param length the number of characters to test
	 * @return true if it's all whitespace
	 */
	public static boolean isWhitespace(char[] ch, int start, int length) {
		final int end = start + length;
		for (int i = start; i < end; i++) {
			if (!Character.isWhitespace(ch[i])) {
				return false;
			}
		}
		return true;
	}

	
	public static boolean isWhitespace(CharSpan span) {
		return isWhitespace(span.getArray(), span.getOffset(), span.size());
	}

	/**
	 * Test to see if the specified String is
	 * entirely whitespace
	 * @param str the string to test
	 * @return true if it's all whitespace
	 */
	public static boolean isWhitespace(String str) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	
	
	
	
	
	
	
	/**
	 * Converts wildcard expression to regular expression. In wildcard-format,
	 * '*' = 0-N characters and ? = any one character.
	 * @param wildcard wildcard expression string
	 * @return given wildcard expression as regular expression
	 */
	static public String wildcardToRegexp(String wildcard) {
		final int len = wildcard.length();
		FastStringBuffer buf = new FastStringBuffer(len + 10);
		FastStringBuffer wildcardExp = new FastStringBuffer(wildcard);
		wildcardToRegexp(wildcardExp, buf);
		return buf.toString();
	}

	/**
	 * Converts wildcard expression to regular expression. In wildcard-format,
	 * '*' = 0-N characters and ? = any one character.
	 * @param wildcardExp wildcard expression string
	 * @param buf buffer which receives the regular expression
	 */
	static public void wildcardToRegexp(FastStringBuffer wildcardExp, FastStringBuffer buf) {
		final int len = wildcardExp.size();
		buf.clear();
		for (int i = 0; i < len; i++) {
			char c = wildcardExp.charAt(i);
			switch (c) {
			case '*':
				buf.append('.');
				buf.append('*');
				break;
			case '?':
				buf.append('.');
				break;
			// escape special regexp-characters

			case '(':
			case ')':
			case '[':
			case ']':
			case '$':
			case '^':
			case '.':
			case '{':
			case '}':
			case '|':
			case '\\':
			case '+':
				buf.append('\\');
				buf.append(c);
				break;
			default:
				buf.append(c);
				break;
			}
		}
	}

	/**
	 * Converts milliseconds to an ISO8601 format for string comparisons.
	 * @param milliseconds
	 * @return ISO date format in a String
	 */
	static public String convertMillisecondsToISO8601(long milliseconds) {
		final SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		final Date d = new Date(milliseconds);
		return date.format(d);
	}

	/**
	 * Write an integer to a location in a byte array. Serializes the
	 * integer out as four bytes, big-endian.
	 *
	 * @param array the array to write to
	 * @param i the integer to write
	 * @param offset the offset into the array to start writing
	 */
	public static void writeInt(byte[] array, int i, int offset) {
		array[offset++] = (byte) ((i >>> 24) & 0xFF);
		array[offset++] = (byte) ((i >>> 16) & 0xFF);
		array[offset++] = (byte) ((i >>> 8) & 0xFF);
		array[offset] = (byte) (i & 0xFF);
	}

	/**
	 * Write a long to a location in a byte array. Serializes the
	 * long out as eight bytes, big endian.
	 *
	 * @param array the array to write to
	 * @param i the long to write
	 * @param offset the offset into the array to start writing
	 */
	public static void writeLong(byte[] array, long i, int offset) {
		array[offset++] = (byte) ((i >>> 56) & 0xFF);
		array[offset++] = (byte) ((i >>> 48) & 0xFF);
		array[offset++] = (byte) ((i >>> 40) & 0xFF);
		array[offset++] = (byte) ((i >>> 32) & 0xFF);
		array[offset++] = (byte) ((i >>> 24) & 0xFF);
		array[offset++] = (byte) ((i >>> 16) & 0xFF);
		array[offset++] = (byte) ((i >>> 8) & 0xFF);
		array[offset] = (byte) ((i & 0xFF));
	}

	/**
	 * Read an integer from a location in a byte array. Assumes the int was
	 * serialized out using writeInt().
	 *
	 * @param array the array to read from
	 * @param offset the offset into the array to start reading
	 * @return the integer that was read
	 */
	public static int readInt(byte[] array, int offset) {
		int i = 0;
		i |= ((int) array[offset++] & 0xFF) << 24;
		i |= ((int) array[offset++] & 0xFF) << 16;
		i |= ((int) array[offset++] & 0xFF) << 8;
		i |= array[offset] & 0xFF;
		return i;
	}

	/**
	 * Read a long from a location in a byte array. Assumes the long was
	 * serialized out using writeLong().
	 *
	 * @param array the array to read from
	 * @param offset the offset into the array to start reading
	 * @return the long that was read
	 */
	public static long readLong(byte[] array, int offset) {
		long i = 0;
		i |= ((long) array[offset++] & 0xFF) << 56;
		i |= ((long) array[offset++] & 0xFF) << 48;
		i |= ((long) array[offset++] & 0xFF) << 40;
		i |= ((long) array[offset++] & 0xFF) << 32;
		i |= ((long) array[offset++] & 0xFF) << 24;
		i |= ((long) array[offset++] & 0xFF) << 16;
		i |= ((long) array[offset++] & 0xFF) << 8;
		i |= array[offset] & 0xFF;
		return i;
	}

	/**
	 * Reads a char from a location in a byte array.
	 *
	 * @param array the array to read from
	 * @param offset the offset into the array to start reading
	 * @return the char that was read
	 */
	public static char readChar(byte[] array, int offset) {
		char i = 0;
		i |= (array[offset++] & 0xFF) << 8;
		i |= array[offset] & 0xFF;
		return i;
	}

	/**
	 * Return a hashCode for this CharSpan.
	 * @param span the span to hash
	 * @return a hashCode
	 */
	public static int hashCodeForCharSpan(CharSpan span) {
		char [] arr = span.getArray();
		int start = span.getOffset();
		int end = start + span.size();
        int result = 1;
		for (int i = start; i < end; i++) {
            result = 31 * result + arr[i];
		}
        return result;
	}
	
	/**
	 * Return a hashCode for this CharSequence. The Javadoc for
	 * CharSequence says there is no way, in general, to create
	 * a hashCode or equals for a class that implement CharSequence,
	 * but this works and is good enough.
	 * @param seq the seq to hash
	 * @return a hashCode
	 */
	public static int hashCodeForCharSequence(CharSequence seq) {
		int end = seq.length();
		int result = 1;
		for (int i = 0; i < end; i++) {
            result = 31 * result + seq.charAt(i);
		}
        return result;
	}

	/**
	 * Return true if the contents of the two CharSpans are equal.
	 * @param span0 first CharSpan to compare
	 * @param span1 second CharSpan to compare
	 * @return true if equal
	 */
	public static boolean equals(CharSpan span0, CharSpan span1) {
		if (span0.size() != span1.size()) {
			return false;
		}
		int size = span0.size();

		char [] arr0 = span0.getArray();
		char [] arr1 = span1.getArray();
		
		int offset0 = span0.getOffset();
		int offset1 = span1.getOffset();
		
		for (int i = 0; i < size; i++) {
			if (arr0[offset0++] != arr1[offset1++]) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Return true if the contents of the CharSpan and the String are equal.
	 * @param span CharSpan to compare
	 * @param str String to compare
	 * @return true if equal
	 */
	public static boolean equals(CharSpan span, String str) {
		if (span.size() != str.length()) {
			return false;
		}
		int size = span.size();

		char [] arr = span.getArray();
		int offset = span.getOffset();
		
		for (int i = 0; i < size; i++) {
			if (!(arr[offset + i] == str.charAt(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * Converts a String to a boolean. Recognizes "t", "true", "y", "yes" without 
	 * regard to case. 
	 * @param value the boolean in string form
	 * @param defaultValue the value to return if the value is null
	 * @return the value as a boolean, or false if the string does not contain
	 * one of the forms of "true" above
	 */
	public static boolean toBoolean(String value, boolean defaultValue) {
		if (value == null)
			return defaultValue;
		if (value.equalsIgnoreCase("Y")
				|| value.equalsIgnoreCase("T")
				|| value.equalsIgnoreCase("yes")
				|| value.equalsIgnoreCase("true"))
				return true;
			else
				return false;
	}
	
	/**
	 * Return a filename's extension.
	 * @return the extension, or null if one cannot be detected
	 */
	public static String getExtension(String filename) {
		if (filename == null) {
			return null;
		}
		// get extension
		int pos = filename.lastIndexOf('.');
		if (pos == -1 || pos == filename.length() - 1)
			return null;
		String ext = filename.substring(pos + 1, filename.length());
		ext = ext.toLowerCase();
		return ext;
	}
	
	
}
