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
package org.openpipeline.pipeline.item;

import org.openpipeline.util.CharArraySequence;
import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.Util;

/**
 * Represents a token, typically a word within a larger text buffer. Can
 * be used to specify any span of text within a buffer.
 */
public class Token implements CharSpan, Comparable<CharSequence> {

	public Token next; // this is used only by JavaCC, and should go away in the future
	
	private FastStringBuffer buf;
	private int charOffset; // absolute char offset in the buffer
	private int size;
	private int type;
	private int wordOffset; // wordOffset relative to the beginning of the parent node
	private float weight = 1.0f;

	private Token originalToken;

	public Token() {
	}
	
	/**
	 * Create a token on the specified string. Appends the string
	 * to the internal buffer and sets internal variables to point
	 * to it.
	 * @param tok the token to wrap
	 */
	public Token(String tok) {
		buf = new FastStringBuffer(tok);
		size = buf.size();
	}
	
	/**
	 * Create a token by making a copy of the specified buffer.
	 * @param tok the buffer to copy
	 */
	public Token(FastStringBuffer tok) {
		buf = new FastStringBuffer(tok);
		size = buf.size();
	}
	
	/**
	 * Create a token with an internal buffer of the specified capacity.
	 * @param capacity number of chars the token will contain
	 */
	public Token(int capacity) {
		buf = new FastStringBuffer(capacity);
	}
	
	/**
	 * Get the character offset within the internal character buffer
	 * where this token starts.
	 */
	public int getOffset() {
		return charOffset;
	}

	/**
	 * Get the number of characters in this token.
	 */
	public int size() {
		return size;
	}

	/**
	 * Get the type of this token. A type is any arbitrary integer. It is 
	 * typically assigned by the code that generated the token, and
	 * depends on the context.
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * When this token is part of a list of tokens over a text buffer,
	 * the wordOffset is the token number.
	 * @return the offset of this token within a list of tokens
	 */
	public int getWordOffset() {
		return wordOffset;
	}

	/**
	 * Returns the underlying char array that contains the token -- IMPORTANT: the
	 * token does not necessarily start at the beginning of the array. Use getOffset() 
	 * and size() to determine the start and length of the token within the array.
	 * ALSO IMPORTANT: don't store a reference to this array, because it can
	 * change if the underlying character buffer is resized.
	 * @return a char [] containing the token
	 */
	public char[] getArray() {
		return buf.getArray();
	}
	
	public void setOffset(int offset) {
		this.charOffset = offset;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setWordOffset(int wordOffset) {
		this.wordOffset = wordOffset;
	}

	/**
	 * Set the underlying text buffer. The token points to a span of
	 * characters in this buffer.
	 * @param buf the buffer to point to.
	 */
	public void setBuffer(FastStringBuffer buf) {
		this.buf = buf;
	}
	
	public FastStringBuffer getBuffer() {
		return buf;
	}

	/**
	 * Set an arbitrary floating-point weight to assign to the token.
	 * It's use is context-dependent.
	 * @param weight a weight
	 */
	public void setWeight(float weight) {
		this.weight = weight;
	}
	
	public float getWeight() {
		return weight;
	}
	
	/**
	 * Return a verbose description of this token, rendered in XML.
	 * @return this token converted to XML
	 */
	public String toXML() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<token offset=\"");
		buf.append(charOffset);
		buf.append("\" size=\"");
		buf.append(size);
		buf.append("\" wordoffset=\"");
		buf.append(wordOffset);
		buf.append("\" type=\"");
		buf.append(type);
		buf.append("\" text=\"");

		buf.appendWithXMLEncode(getArray(), charOffset, size);

		buf.append("\"/>");

		return buf.toString();
	}

	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		return equals(o.toString());
	}
	
	public boolean equals(CharSpan span) {
		return Util.equals(this, span);
	}
	
	public boolean equals(String str) {
		return Util.equals(this, str);
	}
	
	public int hashCode() {
		return Util.hashCodeForCharSpan(this);
	}

	@Override
	public String toString() {
		return new String(getArray(), charOffset, size);
	}
	
	public boolean startsWith(String other) {
		return buf.regionMatches(charOffset, other);
	}
	
	/**
	 * Create a shallow duplicate. Does not duplicate the underlying buffer.
	 * @return a duplicate of this Token
	 */
	public Token dupe() {
		Token dupe = new Token();
		dupe.buf = buf;
		dupe.charOffset = charOffset;
		dupe.size = size;
		dupe.type = type;
		dupe.wordOffset = wordOffset;
		dupe.weight = weight;
		return dupe;
	}

	/**
	 * Return the char at the specified character offset. This offset
	 * is relative to the beginning of the token, not the beginning
	 * of the underlying buffer.
	 */
	public char charAt(int index) {
		return getArray()[charOffset + index];
	}

	public int length() {
		return size;
	}

	public CharSequence subSequence(int start, int end) {
		CharArraySequence charSeq = new CharArraySequence();
		charSeq.reset(getArray(), charOffset + start, end - start);
		return charSeq;
	}

	/**
	 * This method allows us to compare to Strings, CharSpans, Tokens, etc.
	 * @param seq any object implementing CharSequence
	 * @return see Comparable for the return values
	 */
	public int compareTo(CharSequence seq) {
		final int len = length() < seq.length() ? length() : seq.length();
		for (int i = 0; i < len; i++) {
			int cmp = charAt(i) - seq.charAt(i);
			if (cmp != 0) {
				return cmp;
			}
		}

		// they're equal. the shorter term sorts first.
		return length() - seq.length();
	}

	/*
	public int compareTo(Object o) {
		return compareTo((CharSequence)o);
	}
	*/

	/**
	 * If this token is a modification of another token, set a reference
	 * back to the original token.
	 * @param originalToken the token from which this token was derived
	 */
	public void setOrigToken(Token originalToken) {
		this.originalToken = originalToken;
	}
	
	/**
	 * If this token is a modification of another token, return a reference
	 * to that original token.
	 * @return the original token, or null if there was none
	 */
	public Token getOriginalToken() {
		return originalToken;
	}

	/**
	 * Returns the first index of the specified char relative to
	 * the beginning of the token, or -1 if char does not exist.
	 * @param c the char to search for
	 * @return the index
	 */
	public int indexOf(char c) {
		char [] arr = getArray();
		int end = charOffset + size;
		for (int i = charOffset; i < end; i++) {
			if (arr[i] == c) {
				return i - charOffset;
			}
		}
		return -1;
	}

	/**
	 * Return the char offset of the original token for this token, if it exists,
	 * otherwise return the char offset of this token.
	 */
	public int getOriginalOffset() {
		Token token = this;
		while (true) {
			if (token.getOriginalToken() == null) {
				break;
			}
			token = getOriginalToken();
		}
		return token.getOffset();
	}
	
}
