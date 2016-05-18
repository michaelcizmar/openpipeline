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

public class ByteArray extends InputStream implements Comparable<ByteArray> {

	public static final byte TERMINATOR = 2;
	
	protected byte[] array;
	protected int size;
	protected int position;

	public ByteArray() {
		this(16);
	}

	public ByteArray(int initialCapacity) {
		array = new byte[initialCapacity];
	}

	public ByteArray(String str, boolean terminate) {
		array = new byte[str.length() * 2];
		appendModifiedUTF(str, terminate);
	}

	public ByteArray(String str) {
		this(str, true);
	}

	public ByteArray(byte[] arr) {
        this(arr, arr.length);
	}

    public ByteArray(byte[] arr, int size) {
        this.array = arr;
        this.size = size;
    }

    
	public byte[] getArray() {
		return array;
	}

	public byte[] getTrimmedArray() {
		byte[] newArray = new byte[size];
		System.arraycopy(array, 0, newArray, 0, size);
		return newArray;
	}

	public void setArray(byte[] array) {
		this.array = array;
		this.size = array.length;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void appendByte(byte value) {
		size++;
		ensureCapacity(size);
		array[size - 1] = value;
	}

	
	public void append(byte[] value) {
		append(value, 0, value.length);
	}

	public void append(byte[] value, int offset, int length) {
		int newSize = size + length;
		ensureCapacity(newSize);
		System.arraycopy(value, offset, array, size, length);
		size = newSize;
	}

	public void appendLong(long value) {
		int newSize = size + 8;
		ensureCapacity(newSize);
		Util.writeLong(array, value, size);
		size = newSize;
	}

	public void appendInt(int value) {
		int newSize = size + 4;
		ensureCapacity(newSize);
		Util.writeInt(array, value, size);
		size = newSize;
	}

	public void appendChar(char value) {
		int newSize = size + 2;
		ensureCapacity(newSize);
		array[size++] = (byte) (value >>> 8);
		array[size++] = (byte) (value & 0xFF);
	}

	public void append(ByteArray inputArray) {
		int newSize = size + inputArray.size;
		ensureCapacity(newSize);
		System.arraycopy(inputArray.array, 0, array, size, inputArray.size);
		size = newSize;
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

	public void append(InputStream in, int len) throws IOException {
		ensureCapacity(size + len);
		while (true) {
			int count = in.read(array, size, len);
			if (count == -1)
				break;
			size += count;
			len = len - count;
			if (len <= 0)
				break;
		}
	}

	/**
	 * Append an integer as a group of compressed bytes. Requires
	 * between 1 and 5 byte of space.
	 * @param i the integer to write
	 */
	public void appendvInt(int i) {
		ensureCapacity(size + 5);
		while ((i & ~0x7F) != 0) {
			array[size++] = (byte) ((i & 0x7f) | 0x80);
			i >>>= 7;
		}
		array[size++] = (byte) i;
	}

	public void appendvLong(long lng) {
		ensureCapacity(size + 9);
		while ((lng & ~0x7F) != 0) {
			array[size++] = (byte) ((lng & 0x7f) | 0x80);
			lng >>>= 7;
		}
		array[size++] = (byte) lng;
	}	
	
	
	/**
	 * Get an integer encoded as a vInt starting at the current position.
	 */
	public int getvInt() {
		byte b = array[position++];
		int num = b & 0x7F;
		for (int shift = 7; b < 0; shift += 7) {
			b = array[position++];
			num |= (b & 0x7F) << shift;
		}
		return num;
	}

	/**
	 * Get a long encoded as a vLong starting at the current position.
	 */
	public long getvLong() {
		byte b = array[position++];
		long num = b & 0x7F;
		for (int shift = 7; b < 0; shift += 7) {
			b = array[position++];
			num |= (b & 0x7FL) << shift;
		}
		return num;
	}
	
	
	
	public long getLong() {
		long ret = Util.readLong(array, position);
		position += 8;
		return ret;
	}

	public int getInt() {
		int ret = Util.readInt(array, position);
		position += 4;
		return ret;
	}
	
	public char getChar() {
	    char ch = (char)(array[position++] << 8);
	    ch |= array[position++] & 0xFF;
	    return ch;
	}

	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			byte[] newArray = new byte[newCapacity];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}

	public void ensureCapacityNoCopy(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			array = new byte[newCapacity];
		}
	}

	protected int getNewCapacity(int oldCapacity) {
		// make 10% bigger than required
		int newCapacity = Math.max(16, (int) ((float) oldCapacity * 1.1f));
		return newCapacity;
	}

	public byte get(int index) {
		return array[index];
	}

	public byte get() {
		return array[position++];
	}

	public byte peek() {
		return array[position];
	}

	public void put(int index, byte value) {
		array[index] = value;
	}

	public void put(int index, char value) {
		array[index++] = (byte) (value >>> 8);
		array[index] = (byte) (value & 0xFF);
	}

	public void put(int index, int value) {
		Util.writeInt(array, value, index);
	}

	public void put(int index, long value) {
		Util.writeLong(array, value, index);
	}
	
	public void put(byte value) {
		array[position++] = value;
	}

	public void put(int value) {
		if (size < position + 4) {
			int newSize = size + 4;
			ensureCapacity(newSize);
			size = newSize;
		}
		Util.writeInt(array, value, position);
		position += 4;
	}

	public void put(byte[] value, int offset, int count) {
		System.arraycopy(value, offset, array, position, count);
		position += count;
	}

	public int size() {
		return size;
	}

	public void setSize(int size) {
		//ensureCapacityNoCopy(size);
		this.size = size;
	}

	public void clear() {
		size = 0;
		position = 0;
	}

	public boolean eof() {
		return (position >= size);
	}

	public String toString() {
		FastStringBuffer strBuf = new FastStringBuffer();
		for (int i = 0; i < size; i++) {
			strBuf.append(Integer.toString(array[i]) + " ");
		}
		return strBuf.toString();
	}

	public String toASCII() {
		FastStringBuffer strBuf = new FastStringBuffer();
		for (int i = 0; i < size; i++) {
			strBuf.append((char) array[i]);
		}
		return strBuf.toString();
	}

	public boolean equals(ByteArray newArr) {
		if (size != newArr.size)
			return false;
		byte[] newArray = newArr.array;
		for (int i = 0; i < size; i++) {
			if (array[i] != newArray[i])
				return false;
		}
		return true;
	}

	public boolean equals(Object o) {
		return equals((ByteArray) o);
	}

	/**
	 * Returns a hash code for this object. Based on the hash code
	 * for String.
	 *
	 * @return  a hash code value for this object.
	 */
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < size; i++) {
			hash = 31 * hash + array[i];
		}
		return hash;
	}

	/**
	 * Appends a CharSequence using a modified UTF-8 format. See java.io.DataInputStream
	 * for an explanation of the format. The format is further modified by
	 * omitting the two-byte length indicator in the beginning. Strings are
	 * optionally terminated by ByteArray.TERMINATOR. Can be used to append
	 * String, StringBuffer, StringBuilder, and FastStringBuffer, all of
	 * which implement CharSequence.
	 * @param seq the sequence to append
	 * @param terminate set to true to add a terminator byte at the end.
	 * @see java.io.DataInputStream
	 */
	public void appendModifiedUTF(CharSequence seq, boolean terminate) {
		final int strlen = seq.length();
		int c;

		ensureCapacity(size + (strlen * 3) + 1);

		for (int i = 0; i < strlen; i++) {
			c = seq.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				array[size++] = (byte) c;
			} else if (c > 0x07FF) {
				array[size++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				array[size++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				array[size++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
		if (terminate)
			array[size++] = TERMINATOR;
	}

	/*
	 * Appends a FastStringBuffer using a modified UTF-8 format. See java.io.DataInputStream
	 * for an explanation of the format. The format is further modified by
	 * omitting the two-byte length indicator in the beginning. Strings are
	 * optionally terminated by ByteArray.TERMINATOR.
	 * @param strBuf the buffer to append
	 * @param terminate set to true to add a terminator byte at the end.
	 * @see java.io.DataInputStream
	 * /
	public void appendModifiedUTF(FastStringBuffer strBuf, boolean terminate) {
		appendModifiedUTF(strBuf.getArray(), 0, strBuf.size(), terminate);
	}
	*/

	public void appendModifiedUTF(char[] arr, int offset, int len, boolean terminate) {
		int c;

		ensureCapacity(size + (len * 3) + 1);
		final int end = offset + len;

		for (int i = offset; i < end; i++) {
			c = arr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				array[size++] = (byte) c;
			} else if (c > 0x07FF) {
				array[size++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				array[size++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				array[size++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
		if (terminate)
			array[size++] = TERMINATOR;
	}

	/**
	 * Appends a StringBuffer using a modified UTF-8 format. See java.io.DataInputStream
	 * for an explanation of the format. The format is further modified by
	 * omitting the two-byte length indicator in the beginning. Strings are
	 * optionally terminated by ByteArray.TERMINATOR.
	 * @param strBuf the buffer to append
	 * @param terminate set to true to add a terminator byte at the end.
	 * @see java.io.DataInputStream
	 * /
	public void appendModifiedUTF(StringBuffer strBuf, boolean terminate) {
		final int strlen = strBuf.length();
		int c;

		ensureCapacity(size + (strlen * 3) + 1);

		for (int i = 0; i < strlen; i++) {
			c = strBuf.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				array[size++] = (byte) c;
			} else if (c > 0x07FF) {
				array[size++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				array[size++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				array[size++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				array[size++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
		if (terminate)
			array[size++] = TERMINATOR;
	}
	*/

	/**
	 * Skip past the UTF-encoded string at the current position. Assumes
	 * that the string is terminated.
	 */
	public void skipUTF() {
		while (array[position] != TERMINATOR)
			position++;
		position++; // get past terminator
	}

	/**
	 * Reads a String written with appendModifiedUTF(). Reads characters
	 * up to TERMINATOR or the end of the array.
	 * @return a FastStringBuffer
	 */
	public FastStringBuffer getModifiedUTFBuffer() throws IOException {
		FastStringBuffer buf = new FastStringBuffer();
		getModifiedUTFBuffer(buf);
		return buf;
	}

	/**
	 * Reads the UTF-encoded string starting at the current position and places
	 * it in the buffer. If the position is past the end, the returned buffer
	 * will have a length() of 0.
	 * @param buf the buffer to contain the results
	 * @throws IOException
	 */
	public void getModifiedUTFBuffer(FastStringBuffer buf) throws IOException {
		buf.clear();
		getModifiedUTFBufferAppended(buf);
	}

	/**
	 * Reads the UTF-encoded string starting at the current position and appends
	 * it to data already in the specified buffer. If the position is past
	 * the end, the returned buffer will have a length() of 0.
	 * @param buf the buffer to contain the results
	 * @throws IOException
	 */
	public void getModifiedUTFBufferAppended(FastStringBuffer buf) throws IOException {
		while (true) {
			char ch = getNextUTFChar();
			if (ch == TERMINATOR || ch == '\uFFFF')
				break;
			buf.append(ch);
		}

		/*
		OBSOLETE, but keep it until the replacement code is stable
		int c, char2, char3;
		while (true) {
		    if (position >= size)
		        break;
		
		    c = (int)array[position] & 0xff;
		    if (c == TERMINATOR) {
		        position++;
		        break;
		    }
		
		    switch (c >> 4) {
		    case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
		        /* 0xxxxxxx* /
		        position++;
		        buf.append((char)c);
		        break;
		
		    case 12: case 13:
		        /* 110x xxxx   10xx xxxx* /
		        position += 2;
		        char2 = (int)array[position - 1];
		        buf.append((char)(((c & 0x1F) << 6) | (char2 & 0x3F)));
		        break;
		    case 14:
		        /* 1110 xxxx  10xx xxxx  10xx xxxx * /
		        position += 3;
		        char2 = (int)array[position - 2];
		        char3 = (int)array[position - 1];
		        buf.append((char)(((c     & 0x0F) << 12) |
		                          ((char2 & 0x3F) << 6)  |
		                          ((char3 & 0x3F) << 0)));
		        break;
		    default:
		        /* 10xx xxxx,  1111 xxxx * /
		        throw new IOException("bad UTF String format");
		    }
		}
		*/
	}

	public char getNextUTFChar() throws IOException {
		int c, char2, char3;

		if (position >= size)
			return '\uFFFF'; // not a valid unicode char

		c = (int) array[position] & 0xff;

		switch (c >> 4) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
				/* 0xxxxxxx*/
				position++;
				return (char) c;

			case 12 :
			case 13 :
				/* 110x xxxx   10xx xxxx*/
				position += 2;
				char2 = (int) array[position - 1];
				return (char) (((c & 0x1F) << 6) | (char2 & 0x3F));

			case 14 :
				/* 1110 xxxx  10xx xxxx  10xx xxxx */
				position += 3;
				char2 = (int) array[position - 2];
				char3 = (int) array[position - 1];
				return (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));

			default :
				/* 10xx xxxx,  1111 xxxx */
				throw new IOException("bad UTF String format:" + Integer.toHexString(c));
		}
	}

	/**
	 * Reads a String written with appendModifiedUTF(). Reads characters
	 * up to TERMINATOR or the end of the array.
	 * @return a String
	 */
	public String getModifiedUTF() throws IOException {
		return getModifiedUTFBuffer().toString();
	}

	public void appendBoolean(boolean value) {
		if (value)
			appendByte((byte) 1);
		else
			appendByte((byte) 0);
	}

	public boolean getBoolean() {
		byte b = array[position++];
		// if the byte is 1, return true, else false (0)
		return (b == 1);
	}

	// inputstream methods
	public void reset() {
		setPosition(0);
	}

	public int read() {
		if (position >= size)
			return -1;
		return get();
	}

	public int available() {
		return size - position;
	}

	public boolean markSupported() {
		return true;
	}

	public int read(byte[] b) {
		if (position >= size)
			return -1;
		int bytesToCopy = Math.min(b.length, size - position);
		System.arraycopy(array, position, b, 0, bytesToCopy);
		position += bytesToCopy;
		return bytesToCopy;
	}

	public int read(byte[] b, int off, int len) {
		if (position >= size)
			return -1;
		int bytesToCopy = Math.min(len, size - position);
		System.arraycopy(array, position, b, off, bytesToCopy);
		position += bytesToCopy;
		return bytesToCopy;
	}

	public long skip(long n) {
		position += n;
		if (position > size) {
			long skipped = n - position - size;
			position = size;
			return skipped;
		} else {
			return n;
		}
	}
    
    
    /**
     * Compare buffers byte for byte up to the length of the specified buf.
     * Important: bytes must be treated as *unsigned* values for UTF-8 to sort correctly
     */
    public int comparePartial(ByteArray buf) {

    	int bufSize = buf.size();
    	byte [] bufArray = buf.getArray();
        int commonLen = bufSize < size ? bufSize : size;

        for (int i = 0; i < commonLen; i++) {
        	// we must cast to ints to do an unsigned comparison
            int cmp = ((int)array[i] & 0xFF) - ((int)bufArray[i] & 0xFF);
            if (cmp != 0)
                return cmp;
        }
        
        if (bufSize <= size) {
        	return 0; 
        } else {
        	// bufSize is larger, so it's greater than this object
        	return -1; 
        }
    }

    /**
     * Compare buffers byte for byte up to the length of the smaller buffer. If
     * the bytes are equal, then the shorter buffer is first.
     */
    public int compareTo(ByteArray buf) {
    	int cmp = comparePartial(buf);
    	if (cmp == 0) {
    		return size - buf.size();
    	}
    	return cmp;
    }
	

    /**
     * Appends each character in the buffer as two bytes.
     * @param buf the buffer to ingest
     */
	public void append(FastStringBuffer buf) {
		char [] arr = buf.getArray();
		int arrSize = buf.size();
		int newSize = size + (arrSize * 2);
		ensureCapacity(newSize);
		int ptr = size;
		for (int i = 0; i < arrSize; i++) {
			char value = arr[i];
			array[ptr++] = (byte) (value >>> 8);
			array[ptr++] = (byte) (value & 0xFF);
		}
		size = newSize;
	}

	public long[] getLongArray(int count) {
		long [] out = new long [count];
		for (int i = 0; i < count; i++) {
			out[i] = Util.readLong(array, position);
			position += 8;
 		}
		return out;
	}

	public int[] getIntArray(int count) {
		int [] out = new int [count];
		for (int i = 0; i < count; i++) {
			out[i] = Util.readInt(array, position);
			position += 4;
 		}
		return out;
	}

	public char[] getCharArray(int count) {
		char [] out = new char [count];
		for (int i = 0; i < count; i++) {
			out[i] = Util.readChar(array, position);
			position += 2;
 		}
		return out;
	}

	/**
	 * Append a long value that fits into the specified number of bytes. For example,
	 * if the long is know to be less than 65536, then append only the two least
	 * significant bytes.
	 * @param value contains the long to append
	 * @param size the number of bytes to appeand
	 */
	public void appendSizedLong(long value, int size) {
		for (int i = size - 1; i >= 0; i--) {
			byte b = (byte)((value >>> (i * 8)) & 0xFF);
			appendByte(b);
		}
	}

	/**
	 * Return a long value which is stored in the specified number of bytes.
	 * @param size the number of bytes
	 * @return the long
	 */
	public long getSizedLong(int size) {
		long out = 0;
		for (int i = 0; i < size; i++) {
			out <<= 8;
			out |= array[position++] & 0xFF;
		}
		return out;
	}

	public void appendFloat(float value) {
		int bits = Float.floatToIntBits(value);
		appendInt(bits);
	}
	
	public float getFloat() {
		int bits = getInt();
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Insert the specified buffer at the specified byte position.
	 * @param position starting offset in this ByteArray where the buffer should be inserted
	 * @param buf the buffer to insert
	 */
	public void insert(int position, ByteArray buf) {
		
		int bufSize = buf.size();;
		ensureCapacity(size + bufSize);
		
		// shift current contents up
		System.arraycopy(array, position, array, position + bufSize, size - position);
		
		// copy new contents into the space we created above
		System.arraycopy(buf.getArray(), 0, array, position, bufSize);
		
		size += bufSize;
	}

	/**
	 * The reverse of appendvInt() -- pops a vInt off the end,
	 * reduces the size of the array. This method assumes that the 
	 * previous entry is also a vInt. If not, this method is unreliable.
	 * @return the popped vInt
	 */
	public int popVInt() {

		// back up until we hit a positive byte, then select the next byte as the start of the vInt
		int start = size - 2;
		while (start > 0 && array[start] < 0) {
			start--;
		}
		start++;
		
		position = start;
		int out = getvInt();
		size = start;
		return out;
	}


    
}
