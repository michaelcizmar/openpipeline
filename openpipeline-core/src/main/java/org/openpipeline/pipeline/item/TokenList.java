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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.openpipeline.util.FastStringBuffer;



/**
 * Contains a list of Token objects.
 */
public class TokenList implements Iterable <Token>{
	private int size;
	private Token[] array;

	public TokenList() {
		this(64);
	}

	public TokenList(int capacity) {
		array = new Token[capacity];
	}

	public int size() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Token[] getArray() {
		return array;
	}

	public void setArray(Token[] array) {
		this.array = array;
	}

	public Token get(int i) {
		return array[i];
	}

	public void append(Token token) {
		size++;
		ensureCapacity(size);
		array[size - 1] = token;
	}

	public void set(int i, Token token) {
		array[i] = token;
	}

	public Token remove(int i) {
		Token removed = array[i];
		System.arraycopy(array, i + 1, array, i, size - i - 1);
		size--;
		return removed;
	}

	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			Token[] newArray = new Token[newCapacity];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}

	private int getNewCapacity(int capacity) {
		// this increases the requested capacity to the next highest power of two
		int newCapacity = Integer.highestOneBit(capacity) << 1;
		return newCapacity;
	}

	public void insert(Token token, int position) {
		ensureCapacity(size + 1);
		System.arraycopy(array, position, array, position + 1, size - position);
		array[position] = token;
		size++;
	}

	public void clear() {
		size = 0;
	}

	/**
	 * Sort the tokens in ascending order by offset.
	 */
	public void sort() {
		Arrays.sort(array, 0, size, new CharOffsetComparator());
	}

	/**
	 * Returns a token index for a given starting character offset.
	 * If the exact char offset is not found, returns a negative
	 * number in the same format as Arrays.binarySearch().
	 * @param charOffset the offset to search for
	 * @return the index of the token in this list that has the specified char offset
	 */
	public int findToken(int charOffset) {
		// not very efficient, good enough for now
		Token probe = new Token();
		probe.setOffset(charOffset);
		TokenOffsetComparator comp = new TokenOffsetComparator();
		return Arrays.binarySearch(array, 0, size, probe, comp);
	}

	private class TokenOffsetComparator implements Comparator<Token> {
		public int compare(Token o1, Token o2) {
			return o1.getOriginalOffset() - o2.getOriginalOffset();
		}
	}
	
	
	/**
	 * Sort the tokens in ascending order alphabetically.
	 */
	public void sortAlpha() {
		Arrays.sort(array, 0, size, new AlphaComparator());
	}
	
	
	public String toString() {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append("<tokens>");
		for (int i = 0; i < size; i++) {
			Token token = get(i);
			buf.append("<token>");
			buf.append(token); // later, we might add wordoffset, etc.
			buf.append("</token>");
		}
		buf.append("</tokens>");
		return buf.toString();
	}
	
	public boolean equals(TokenList list) {
		if (size != list.size) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!get(i).equals(list.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TokenList)) {
			return false;
		}
		return equals((TokenList)o);
	}
	
	@Override
	public int hashCode() {
		// just treat the tokenlist as a long string of chars
		int result = 1;
		for (int i = 0; i < size; i++) {
			Token span = array[i];
			
			char [] arr = span.getArray();
			int start = span.getOffset();
			int end = start + span.size();
			for (int j = start; j < end; j++) {
	            result = 31 * result + arr[j];
			}
		}
		
        return result;
	}
	
	
	
	
	public Iterator<Token> iterator() {
		return new TokenIterator();
	}

	class TokenIterator implements Iterator {
		private int position;

		public boolean hasNext() {
			return position < size;
		}

		public Token next() {
			return array[position++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	

	/**
	 * Compare tokens by offset.
	 */
	class CharOffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			Token t1 = (Token) o1;
			Token t2 = (Token) o2;
			return t1.getOffset() - t2.getOffset();
		}
	}

	/**
	 * Compare tokens alphabetically.
	 */
	class AlphaComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			Token t1 = (Token) o1;
			Token t2 = (Token) o2;
			return t1.compareTo(t2);
		}
	}

	/**
	 * Make a copy of this TokenList which is exactly the right size.
	 * @return a TokenList with same contents, except the internal array is the exact size
	 */
	public TokenList getTrimmedCopy() {
		TokenList newList = new TokenList(size);
		for (int i = 0; i < size; i++) {
			newList.append(array[i]);
		}
		return newList;
	}

	/**
	 * Append the specified token list to the bottom of this list.
	 * @param tokens token list to append
	 */
	public void append(TokenList tokens) {
		ensureCapacity(size + tokens.size);
		System.arraycopy(tokens.array, 0, array, size, tokens.size);
		size += tokens.size;
	}

}
