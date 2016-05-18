/*******************************************************************************
 * Copyright 2000-2010 Dieselpoint, Inc. All rights reserved.
 *   
 *  This software is licensed, not sold, and is subject to the terms
 *  of the license agreement. This software is proprietary and 
 *  may not be copied except as contractually agreed. This 
 *  software contains confidential trade secrets which may not be 
 *  disclosed or distributed. "Dieselpoint" is a trademark of 
 *  Dieselpoint, Inc.
 ******************************************************************************/
/*
 * Created on Aug 21, 2004
 *
 * Copyright 2004 Dieselpoint, Inc. All rights reserved.
 */
package org.openpipeline.util;

import java.io.Reader;

/**
 * Just like java.io.CharArrayReader, except that it has an init() method and
 * there's no unnecessary synchronization.
 */
public class FastCharArrayReader extends Reader {
	private char buf[];
	private int pos;
	private int markedPos = 0;
	private int end;

	public FastCharArrayReader() {
	}

	public void init(char buf[], int offset, int length) {
		this.buf = buf;
		this.pos = offset;
		this.end = Math.min(offset + length, buf.length);
		this.markedPos = offset;
	}

	public void close() {
		buf = null;
	}

	public int read() {
		if (pos >= end)
			return -1;
		else
			return buf[pos++];
	}

	public int read(char b[], int off, int len) {
		len = Math.min(len, end - pos);
		if (len <= 0) {
			return -1;
		}
		System.arraycopy(buf, pos, b, off, len);
		pos += len;
		return len;
	}

	public long skip(long n) {
		if (pos + n > end) {
			n = end - pos;
		}
		if (n < 0) {
			return 0;
		}
		pos += n;
		return n;
	}

	public boolean ready() {
		return (end - pos) > 0;
	}

	public boolean markSupported() {
		return true;
	}

	@SuppressWarnings("unused")
	public void mark(int readAheadLimit) {
		markedPos = pos;
	}

	public void reset() {
		pos = markedPos;
	}

}
