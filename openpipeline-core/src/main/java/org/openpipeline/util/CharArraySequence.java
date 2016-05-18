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


/** 
 * An implementation of CharSpan and CharSequence over a char [] array.
 */
public class CharArraySequence implements CharSpan {
	private char [] arr;
	private int offset;
	private int len;
	
	public CharArraySequence() {
	}
	
	public void reset(char [] arr, int offset, int len) {
		this.arr = arr;
		this.offset = offset;
		this.len = len;
	}
	
	public char charAt(int index) {
		return arr[index + offset];
	}
	
	public int length() {
		return len;
	}
	
	public CharSequence subSequence(int start, int end) {
		CharArraySequence cs = new CharArraySequence();
		cs.reset(arr, offset + start, end - start);
		return cs;
	}
	
	public String toString() {
		return new String(arr, offset, len);
	}

	@Override
	public char[] getArray() {
		return arr;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public int size() {
		return len;
	}

	public void setArray(char[] arr) {
		this.arr = arr;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setSize(int size) {
		this.len = size;
	}
	
	public int getSize() {
		return len;
	}
}

