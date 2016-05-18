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

import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastStringBuffer;

/**
 * Defines a span of text associated with a Node in an Item.
 */
public class TextValue implements CharSpan {
	
	private FastStringBuffer buf;
	private int charOffset; 		// pointer to value in item buffer
	private int size; 				// size in chars of value 

	public TextValue(FastStringBuffer buf) {
		this.buf = buf;
	}
	
	public char[] getArray() {
		return buf.getArray();
	}

	public int getOffset() {
		return charOffset;
	}

	public int size() {
		return size;
	}

	public char charAt(int index) {
		return getArray()[charOffset + index];
	}

	public int length() {
		return size;
	}

	public CharSequence subSequence(int start, int end) {
		TextValue seq = new TextValue(buf);
		seq.charOffset = charOffset + start;
		seq.size = end - start;
		return seq;
	}
	
	public FastStringBuffer getBuffer() {
		return buf;
	}
	
	public String toString() {
		return new String(buf.getArray(), charOffset, size);
	}

	public void setOffset(int offset) {
		this.charOffset = offset;
	}

	public void setSize(int size) {
		this.size = size;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return buf.regionMatches(charOffset, obj.toString());
	}

}
