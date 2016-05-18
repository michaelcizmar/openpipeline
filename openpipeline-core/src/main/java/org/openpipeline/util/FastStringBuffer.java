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

import java.io.IOException;
import java.io.Reader;

/**
 * This FastStringBuffer class replaces StringBuffer in those 
 * instances when we know a buffer is not shared.  It gets away 
 * from some of the complications of synchronization and sharing. 
 * It also contains some handy methods for xml encoding. 
 */
public class FastStringBuffer implements Comparable, CharSequence, CharSpan {

	private char[] array;
	private int size = 0;

	public FastStringBuffer() {
		this(16);
	}

	public FastStringBuffer(String str) {
		array = new char[str.length()];
		str.getChars(0, str.length(), array, 0);
		size = str.length();
	}

	public FastStringBuffer(int initialCapacity) {
		array = new char[initialCapacity];
	}

	public FastStringBuffer(char[] arr, int offset, int length) {
		array = new char[length];
		System.arraycopy(arr, offset, array, 0, length);
		size = length;
	}

    public FastStringBuffer(FastStringBuffer fsb) {
        int len = fsb.size();
        array = new char[len];
        System.arraycopy(fsb.getArray(), 0, array, 0, len);
        size = len;
    }

    public FastStringBuffer(CharSpan span) {
    	this(span.getArray(), span.getOffset(), span.size());
    }
    
	public FastStringBuffer(CharSequence seq) {
		int len = seq.length();
		array = new char[len];
		for (int i = 0; i < len; i++) {
			array[i] = seq.charAt(i);
		}
		size = len;
	}

	public int size() {
		return size;
	}


	public void setSize(int size) {
		ensureCapacityNoCopy(size);
		this.size = size;
	}

	public void clear() {
		size = 0;
		//		position = 0;
	}

	public void append(char[] arr, int offset, int length) {
		int newSize = size + length;
		ensureCapacity(newSize);
		System.arraycopy(arr, offset, array, size, length);
		size = newSize;
	}
	
	/**
	 * Append an integer as a decimal string
	 */
	public void append(int i) {
		append(Integer.toString(i));
	}

	public void append(char ch) {
		size++;
		ensureCapacity(size);
		array[size - 1] = ch;
	}

	public void append(String str) {
	    append(str, 0, str.length());
	}

	public void append(String str, int offset, int length) {
		if (str == null)
			return;
		int newSize = size + length;
		ensureCapacity(newSize);
		str.getChars(offset, offset + length, array, size);
		size = newSize;
	}
	
    /**
     * Append an integer into two chars, bigendian.
     */
    public void appendIntAsChars(int i) {
        char ch = (char)((i >>> 16) & 0xFFFF);
        append(ch);
        ch = (char)(i & 0xFFFF);
        append(ch);
    }
    
	public char charAt(int offset) {
		return array[offset];
	}

	public void setCharAt(int offset, char ch) {
		array[offset] = ch;
	}

	public void append(FastStringBuffer fsb) {
		append(fsb.getArray(), 0, fsb.size());
	}
	
	public void append(CharSpan span) {
		append(span.getArray(), span.getOffset(), span.size());
	}

	public void append(StringBuffer sb) {
		int newSize = size + sb.length();
		ensureCapacity(newSize);
		sb.getChars(0, sb.length(), array, size);
		size = newSize;
	}

	public void append(Reader in) throws IOException {
		while (true) {
			// check the number of chars avail in the buffer
			int avail = array.length - size;
			if (avail < 1024) {
				avail = 1024; // always try to read at least 1024 chars
			}
			ensureCapacity(size + avail);
			int count = in.read(array, size, avail);
			if (count < 0)
				break;
			size += count;
		}
	}

	public void insert(int offset, char ch) {
		ensureCapacity(size + 1);
		System.arraycopy(array, offset, array, offset + 1, size - offset);
		array[offset] = ch;
		size++;
	}

	public void insert(int offset, String str) {
		int strLen = str.length();
		ensureCapacity(size + strLen);
		System.arraycopy(array, offset, array, offset + strLen, size - offset);
		str.getChars(0, strLen, array, offset);
		size += strLen;
	}

	public void insert(int offset, FastStringBuffer buf) {
		int bufLen = buf.size();
		ensureCapacity(size + bufLen);
		System.arraycopy(array, offset, array, offset + bufLen, size - offset);
		System.arraycopy(buf.getArray(), 0, array, 0, bufLen);
		size += bufLen;
	}
	
	public void remove(int offset, int length) {
		// count of chars to move
		int count = size - (offset + length);
		if (count < 0) {
			throw new IndexOutOfBoundsException("size=" + size + " offset=" + offset + " length=" + length);
		}
		System.arraycopy(array, offset + length, array, offset, count);
		size = size - length;
	}

	
	/**
	 * Works the same as String.trim(), except that it trims whitespace
	 * in-place.
	 */
	public void trim() {
		int st = 0;
		while ((st < size) && (array[st] <= ' ')) {
			st++;
		}
		while ((st < size) && (array[size - 1] <= ' ')) {
			size--;
		}
		if (st > 0) {
			System.arraycopy(array, st, array, 0, size - st);
		}
		size -= st;
	}
	
	/**
	 * Trims whitespace from the beginning and end of the
	 * character sequence, starting at offset and ending
	 * at offset + len. For example, if the buffer contains
	 * the characters "abc def ", a call to trim(3, 5) will
	 * leave "abcdef" in the buffer. The buffer size will be adjusted
	 * accordingly.
	 */
	public void trim(int offset, int len) {
	    int start = offset;
		int end = offset + len - 1;
		
		// strip whitespace from the beginning
		while (start <= end && Character.isWhitespace(array[start]))
			start++;
		// strip whitespace from the end
		while (end > start && Character.isWhitespace(array[end]))
			end--;
			
		int charsToCopy = end - start + 1;
		if (start != offset) {
			System.arraycopy(array, start, array, offset, charsToCopy);
		}
		size = offset + charsToCopy;
	}
	

	/**
	 * Reduce the size of the internal char array to the exact size needed.
	 */
	public void trimToSize() {
		char[] newArray = new char[size];
		System.arraycopy(array, 0, newArray, 0, size);
		array = newArray;
	}

	public boolean equals(Object obj) {
		if (obj instanceof FastStringBuffer) {
			return equals((FastStringBuffer) obj);
		} else {
			return false;
		}
	}

	public boolean equals(FastStringBuffer buf) {
		if (buf == null)
			return false;
		if (size != buf.size) // if lengths differ
			return false;

		char[] bufArr = buf.array;
		for (int i = 0; i < size; i++) {
			if (array[i] != bufArr[i])
				return false;
		}
		
		return true;
	}

	public boolean equals(StringBuffer strBuf) {
		if (strBuf == null)
			return false;
		if (size != strBuf.length()) // if lengths differ
			return false;

		for (int i = 0; i < size; i++) {
			if (array[i] != strBuf.charAt(i))
				return false;
		}
		return true;
	}

	public boolean equals(String str) {
		if (str == null)
			return false;
		if (size != str.length()) // if lengths differ
			return false;

		for (int i = 0; i < size; i++) {
			if (array[i] != str.charAt(i))
				return false;
		}
		return true;
	}

	/**
	 * Returns a hash code for this object. Based on the hash code
	 * for String.
	 * @return  a hash code value for this object.
	 */
	public int hashCode() {
		return Util.hashCodeForCharSpan(this);
	}

	/**
	 * Replace the contents of the buffer with XML-encoded contents.
	 * Encodes the following chars: > < & "
	 * <b>Important: does not encode apostrophes (') as &apos; as per
	 * the XML spec.</b> The reason is that XML encoding is often used
	 * to encode HTML text as well, and some browsers (notably IE) do
	 * not properly handle apostrophes. It's ok to have unencoded apostrophes
	 * anywhere in XML except in one case: when an attribute value starts
	 * with a single quote instead of a double quotes. In that case, apostrophes
	 * within the value need to be encoded. The workaround is to enclose XML
	 * attributes with the standard double quotes.
	 * @param offset the char where encoding should start
	 * @param length the total number of chars to encode
	 */
	public void xmlEncode(int offset, int length) {
        /* @todo encode some upper ranges as well
         * { from the XML spec, 2.2 "Characters" }
         * [2] Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD]
         * | [#x10000-#x10FFFF]
         *  / any Unicode character, excluding the surrogate blocks, FFFE, and
         *  FFFF. /
         */
        
	    /* @todo Jim Nicholson suggests encoding
	     * case 7: result.append("&#x07");
	     * case '\f': result.append("&#x0C");
	     * case '\r': result.append("&#x0D;");
	     * case 14 - 31: just ignore, supposedly impossible in XML?
	     */
	    
		// this is a two-pass algorithm, but it minimizes the garbage generated
		// and the total amount of copying required.
		int extraSpace = calcExtraSpaceForXML(array, offset, length);
		if (extraSpace == 0) {
			return; // no encoding necessary
		} else if (extraSpace == -1) {
			extraSpace = 0;
		}
		ensureCapacity(size + extraSpace);
		expandXML(offset, length, extraSpace);
	}


	/**
	 * Append and XML-encode the string. 
	 * See {@link #xmlEncode(int offset, int length)} for details on the encoding rules.
	 * @param str string containing chars to append
	 */
	public void appendWithXMLEncode(String str) {
		// TODO redo all this with CharSequences
		if (str == null)
			return;
		char [] arr = str.toCharArray();
		appendWithXMLEncode(arr, 0, arr.length);
	}
	
	/**
	 * Append and XML-encode the contents of the FastStringBuffer.
	 * See {@link #xmlEncode(int offset, int length)} for details on the encoding rules.
 	 * @param buf the buffer to append
	 */
	public void appendWithXMLEncode(FastStringBuffer buf) {
		appendWithXMLEncode(buf.getArray(), 0, buf.size());
	}
	
	/**
	 * Append the char array, starting with the char at "offset" and including
	 * "length" chars. XML-encode the appended chars. 
	 * See {@link #xmlEncode(int offset, int length)} for details on the encoding rules.
	 * @param arr array containing chars to append
	 * @param offset the first char in the array to add
	 * @param length the number of chars in the array to add
	 */
	public void appendWithXMLEncode(char[] arr, int offset, int length) {
		int extraSpace = calcExtraSpaceForXML(arr, offset, length);
		ensureCapacity(size + length + extraSpace);
		int start = size;
		append(arr, offset, length); // increases size
		if (extraSpace > 0) {
			expandXML(start, length, extraSpace);
		} else if (extraSpace == -1) {
			expandXML(start, length, 0);
		}
	}

	/**
	 * Return the number of extra characters it would require to
	 * xml-encode the array.
	 */
	private int calcExtraSpaceForXML(char[] arr, int offset, int length) {
		// calc extra space req'd for expanded codes
		final int end = offset + length;
		int extraSpace = 0;
		boolean hasBadChar = false;
		
		for (int i = offset; i < end; i++) {
            char ch = arr[i];
          
            // tab, cr, lf are ok. all other ctrl chars bad
            if (ch < 0x20 && ch != 0x9 && ch != 0xA && ch != 0xD) {
            	hasBadChar = true;
            	
            } else {
                switch (ch) {
				case '<' :
					extraSpace += 3;
					break;
				case '>' :
					extraSpace += 3;
					break;
				case '&' :
					extraSpace += 4;
					break;
				/*
				case '\'' :
					extraSpace += 5;
					break;
				*/
				case '"' :
					extraSpace += 5;
					break;
				default : // do nothing
				}
			}
		}
		
		if (extraSpace == 0 && hasBadChar) {
			extraSpace = -1; // force an expansion
		}
		
		return extraSpace;
	}
	
    //private static final char[] hexchars = {
    //      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    //    };

	private void expandXML(int offset, int length, int extraSpace) {

		// copy any chars past the end of the portion we're encoding
		if (offset + length < size) {
			int start = offset + length;
			System.arraycopy(array, start, array, start + extraSpace, size - start);
		}

		size += extraSpace;

		// Replace the chars with the encoded versions
		// Do it backwards to minimize the copying necessary
		int outPtr = offset + length + extraSpace - 1; // pointer to current output char
		for (int i = offset + length - 1; i >= offset; i--) {
            char ch = array[i];

            if (!isAllowedXMLChar(ch)) {
            	
                // replace bad char with space
                array[outPtr] = ' ';
                outPtr--;
                
            } else {
                switch (ch) {
				case '<' :
					array[outPtr--] = ';';
					array[outPtr--] = 't';
					array[outPtr--] = 'l';
					array[outPtr--] = '&';
					break;
				case '>' :
					array[outPtr--] = ';';
					array[outPtr--] = 't';
					array[outPtr--] = 'g';
					array[outPtr--] = '&';
					break;
				case '&' :
					array[outPtr--] = ';';
					array[outPtr--] = 'p';
					array[outPtr--] = 'm';
					array[outPtr--] = 'a';
					array[outPtr--] = '&';
					break;
				/*
				case '\'' :
					array[outPtr--] = ';';
					array[outPtr--] = 's';
					array[outPtr--] = 'o';
					array[outPtr--] = 'p';
					array[outPtr--] = 'a';
					array[outPtr--] = '&';
					break;
				*/
				case '"' :
					array[outPtr--] = ';';
					array[outPtr--] = 't';
					array[outPtr--] = 'o';
					array[outPtr--] = 'u';
					array[outPtr--] = 'q';
					array[outPtr--] = '&';
					break;
				default :
					array[outPtr] = ch;
					outPtr--;
			}
            }
			if (i == outPtr) // no more codes to translate
				break;
		}
	}

	/**
	 * Return true if the character is allowed in XML 1.0. See
	 * http://en.wikipedia.org/wiki/Valid_characters_in_XML
	 */
	private boolean isAllowedXMLChar(char ch) {
		if (ch == 0x9 || 
			ch == 0xA || 
			ch == 0xD || 
			(ch >= 0x20 && ch <= 0xD7FF) ||
			(ch >= 0xE000 && ch <= 0xFFFD)) {
			return true;
		}
		return false;
		
	}
	
	
	/**
	 * Replace "length" chars, starting at "offset", with the contents
	 * of newChars.
	 * @param offset position of first char to replace
	 * @param length number of chars to replace
	 * @param newChars the replacement chars
	 */
	public void replaceChars(int offset, int length, String newChars) {
		final int newLen = newChars.length();
		if (newLen != length) {
			if (newLen > length) // if we need to expand
				ensureCapacity(size + newLen - length);
			System.arraycopy(
				array,
				offset + length,
				array,
				offset + newLen,
				size - (offset + length));
			size += newLen - length;
		}
		newChars.getChars(0, newChars.length(), array, offset);
	}

	/**
	 * Replace all instances of oldStr with newStr.
	 */
	public void replace(String oldStr, String newStr) {
		int from = 0;
		while (true) {
			int i = indexOf(oldStr, from);
			if (i == -1)
				break;
			replaceChars(i, oldStr.length(), newStr);
			from = i + newStr.length();
		}

	}

	/**
	 * Replace all instances of oldStr with newStr, starting at offset and
	 * going up to end.
	 */
	public void replace(String oldStr, String newStr, int offset, int end) {
		while (true) {
			int i = indexOf(oldStr, offset);
			if (i == -1 || i >= end)
				break;
			replaceChars(i, oldStr.length(), newStr);
			offset = i + newStr.length();
		}
	}

	/**
	 * Replace the chars from start to end with the contents of str.
	 * @param start start of span to replace
	 * @param end end of span to replace
	 * @param str new chars to insert
	 */
	public void replace(int start, int end, String str) {
		int oldLen = end - start;
		int netChange = str.length() - oldLen;
		ensureCapacity(size + netChange);
		System.arraycopy(array, end, array, end + netChange, size - end);
		str.getChars(0, str.length(), array, start);
		size = size + netChange;
	}
	
	public int indexOf(char ch) {
	    return indexOf(ch, 0);
	}
	
	public int indexOf(char ch, int fromIndex) {
	    for (int i = fromIndex; i < size; i++) {
	        if (array[i] == ch) {
	            return i;
	        }
	    }
	    return -1;
	}

	public int indexOf(String str) {
		return indexOf(str, 0);
	}
	
	public int indexOf(String str, int fromIndex) {
		if (fromIndex >= size || str.length() > size) {
			return -1;
		}
		if (fromIndex < 0) fromIndex = 0;

		final char firstChar = str.charAt(0);
		final int strLen = str.length();
		for (int i = fromIndex; i < size; i++) {
			
			TEST_FIRST_CHAR:
			if (firstChar == array[i]) {

				// now see if rest of string matches
				int arrPtr = i + 1;
				for (int j = 1; j < strLen; j++) {
					if (str.charAt(j) != array[arrPtr++])
						break TEST_FIRST_CHAR;
				}
				// match found
				return i;
				
			}
		}
		return -1;
	}

	/**
	 * Return true if this buffer starts with the specified string.
	 */
	public boolean startsWith(String str) {
		return regionMatches(0, str);
	}
	
	/**
	 * Return true if this buffer ends with the specified string.
	 */
	public boolean endsWith(String str) {
		int offset = size - str.length();
		return regionMatches(offset, str);
	}
	
	/**
	 * Compare a region of this buffer with a string.
	 * @param offset starting offset in this buffer to do the comparison
	 * @param other the string to compare
	 * @return true if the region exactly matches the string
	 */
    public boolean regionMatches(int offset, String other) {
    	if (offset < 0) {
    		return false;
    	}
    	int otherLen = other.length();
    	int end = offset + otherLen;
    	if (end > size) {
    		return false;
    	}
    	int otherPtr = 0;
    	for (int i = offset; i < end; i++) {
    		if (other.charAt(otherPtr) != array[i]) {
    			return false;
    		}
    		otherPtr++;
    	}
    	return true;
    }
	
	
	/**
	 * Compare buffers character for character up to maxLen. Assumes
	 * that buf has a size of at least maxLen chars.
	 */
	public int compareTo(FastStringBuffer buf, int maxLen) {
		maxLen = size < maxLen ? size : maxLen;
		char[] bufArray = buf.array;
		int cmp = 0;
		for (int i = 0; i < maxLen; i++) {
			cmp = array[i] - bufArray[i];
			if (cmp != 0)
				return cmp;
		}
		return 0; // equal
	}
	
	public int compareTo(FastStringBuffer buf) {
		final int len = size < buf.size ? size : buf.size;
		int cmp = compareTo(buf, len);
		if (cmp == 0) {
			// they're equal. the shorter word sorts first
			return size - buf.size;
		} else {
			return cmp;
		}
	}
    
    public int compareTo(Object o) {
        return compareTo((FastStringBuffer)o);
    }

	/**
	 * Return true if the buffer consists entirely of letters. Handles
	 * non-Western characters correctly.
	 * @return true if the source is all letters, or false if the buffer
	 * contains a digit, punctuation, a space, etc.
	 */
	public boolean isAllLetters() {
		for (int i = 0; i < size; i++) {
			if (!Character.isLetter(array[i]))
				return false;
		}
		return true;
	}

	public char[] getArray() {
		return array;
	}

	public void setArray(char [] array) {
		this.array = array;
	}

	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			char[] newArray = new char[newCapacity];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}

	public void ensureCapacityNoCopy(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			array = new char[newCapacity << 1];
		}
	}

	private final int getNewCapacity(int oldCapacity) {
		// make 10% bigger than required
		int newCapacity = Math.max(16, (int) ((float) oldCapacity * 1.1f));
		return newCapacity;
	}

	public String toString() {
		return new String(array, 0, size);
	}

	public int length() {
		return size;
	}

	public CharSequence subSequence(int start, int end) {
		return new String(array, start, end - start);
	}

	public String substring(int start, int end) {
		return new String(array, start, end - start);
	}

	/**
	 * Implementation of getOffset() for the CharSpan interface -- always returns 0.
	 */
	public int getOffset() {
		return 0;
	}

	/** 
	 * Encode the string for use in a URL and append it.
	 */
	public void appendWithURLEncode(CharSequence input) {
		URLUtils.urlEncode(input, this);
	}

	/**
	 * Convert the specified chars to lower case.
	 * @param offset starting char to convert
	 * @param size number of chars to convert
	 */
	public void lowerCase(int offset, int size) {
		for (int i = offset; i < offset + size; i++) {
			array[i] = Character.toLowerCase(array[i]);
		}
	}

	/**
	 * Append a string and escape appropriate characters
	 * for use in a JSON string. See
	 * http://www.ietf.org/rfc/rfc4627.txt?number=4627 under 
	 * section 2.5, Strings.
	 * @param str the string to encode and then append
	 */
	public void appendWithJSONEncode(CharSequence str) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char ch = str.charAt(i);
			if (ch == '"') {
				append("\\u0022");
			} else if (ch == '\\') {
				append("\\u005C");
			} else if (ch > 0 && ch <= 0x1F) {
				// control chars
				String hex = Integer.toHexString(ch);
				hex = "000".substring(hex.length() - 1) + hex; // left pad to four digits
				append("\\u");
				append(hex);
			} else {
				append(ch);
			}
		}
	}

	public void appendWithEscapedQuotes(CharSequence value) {
		int len = value.length();
		for (int i = 0; i < len; i++) {
			char ch = value.charAt(i);
			if (ch == '"') {
				append('\\');
			}
			append(ch);
		}
	}

	public boolean isWhitespace() {
		for (int i = 0; i < size; i++) {
			if (!Character.isWhitespace(array[i]))
				return false;
		}
		return true;
	}
}
