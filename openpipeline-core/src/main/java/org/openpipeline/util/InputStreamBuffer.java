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
import java.io.InputStream;

/**
 * Converts an InputStream into an array of byte [] without excessive copying.
 * (ByteArrayOutputStream forces an unnecessary copy.) Also extends InputStream
 * so the data can be read out in a stream.
 */
public class InputStreamBuffer extends InputStream {
	
	private byte [] array;
	private int size;
	private int position;
	private int mark;
	
	public InputStreamBuffer(int capacity) {
		array = new byte [capacity];
	}
	
	public InputStreamBuffer() {
		this(1024);
	}
	
	public void append(InputStream in) throws IOException {
		while (true) {
			int avail = in.available();
			if (avail < 0)
				break;

			// when avail is 0, sometimes it means that no bytes are
			// available temporarily (as in an http connection), and
			// sometimes it means we are at the end of file. The only way
			// to distinguish is to test with a read(). Unfortunately, a
			// read(array, size, 0) will always return 0, so we have
			// to try to read at least one byte.
			if (avail == 0)
				avail = 1;
			ensureCapacity(size + avail);
			int count = in.read(array, size, avail);
			if (count < 0)
				break;
			size += count;
		}
	}
	
	public byte [] getArray() {
		return array;
	}
	
	public int size() {
		return size;
	}
	
	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			// get double the required capacity
			int newCapacity = capacity * 2;
			byte[] newArray = new byte[newCapacity];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}
	
	public void clear() {
		size = 0;
	}

	@Override
	public int read() {
		byte [] b = new byte [1];
		int count = read(b, 0, b.length);
		if (count == -1) {
			return -1;
		} else {
			return b[0] & 0xFF;
		}
	}

	@Override
	public int read(byte [] b) {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) {
		if (position >= size) {
			return -1;
		}
		if (b.length < off + len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		
		int count = Math.min(len, size - position);
		System.arraycopy(array, position, b, off, count);
		position += count;
		return count;
	}
	
	@Override
	public int available() {
		return size - position;
	}

	@Override
	@SuppressWarnings("unused")
	public void mark(int readlimit) {
		mark = position;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void reset() {
		position = mark;
	}


	
}
