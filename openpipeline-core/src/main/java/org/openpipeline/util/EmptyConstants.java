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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Contains static, final, read-only implementations of common objects. Useful when you
 * want to return an empty value from a method instead of a null. Eliminates the
 * need for constant null-checking in the code.
 */
public class EmptyConstants {
	/*
	 * Use java.util.Collections for EMPTY_MAP, EMPTY_LIST, EMPTY_SET.
	 * EMPTY_LIST will suffice for an empty Collection.
	 */
	
	public static final Iterator ITERATOR = new EmptyIterator();
	public static final CharSpan CHARSPAN = new EmptyCharSpan();
	
	public static final Object [] OBJECT_ARRAY = new Object[0];
	public static final char [] CHAR_ARRAY = new char[0];
	public static final String [] STRING_ARRAY = new String[0];
}

class EmptyIterator implements Iterator {

	public boolean hasNext() {
		return false;
	}

	public Object next() {
		throw new NoSuchElementException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}

class EmptyCharSpan implements CharSpan {

	public char[] getArray() {
		return EmptyConstants.CHAR_ARRAY;
	}

	public int getOffset() {
		return 0;
	}

	public int size() {
		return 0;
	}

	@SuppressWarnings("unused")
	public char charAt(int i) {
		return 0;
	}

	public int length() {
		return 0;
	}

	@SuppressWarnings("unused")
	public CharSequence subSequence(int start, int end) {
		return this;
	}
}

