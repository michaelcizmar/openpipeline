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

import java.util.Iterator;

import org.openpipeline.util.FastStringBuffer;

public class NodeList implements Iterable<Node> {
	private int size;
	private Node [] array;
	
	public NodeList() {
		this(4);
	}
	
	public NodeList(int capacity) {
		array = new Node[capacity];
	}
	
	public int size() {
		return size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public Node get(int i) {
		checkNodeIndex(i);
		return array[i];
	}
	
	public void put(int i, Node node) {
		checkNodeIndex(i);
		array[i] = node;
	}
	
	public void append(Node node) {
		size++;
		ensureCapacity(size);
		array[size - 1] = node;
	}
	
	
	public Node remove(int i) {
		checkNodeIndex(i);
		Node removed = array[i];
		System.arraycopy(array, i + 1, array, i, size - i - 1);
		size--;
		return removed;
	}

	private void checkNodeIndex(int i) {
		if (i < 0 || i >= size)
			throw new IllegalArgumentException("Node index is out of bounds: " + i);
	}
	
	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			int newCapacity = getNewCapacity(capacity);
			Node [] newArray = new Node[newCapacity];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}

	private int getNewCapacity(int capacity) {
		// this increases the requested capacity to the next highest power of two
		int newCapacity = Integer.highestOneBit(capacity) << 1;
		return newCapacity;
	}

	public void insert(Node node, int position) {
		ensureCapacity(size + 1);
		System.arraycopy(array, position, array, position + 1, size - position);
		array[position] = node;
		size++;
	}

	
	public Iterator<Node> iterator() {
		return new NodeIterator();
	}

	class NodeIterator implements Iterator {
		private int position;

		public boolean hasNext() {
			return position < size;
		}

		public Node next() {
			return array[position++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public String toString() {
		FastStringBuffer buf = new FastStringBuffer();
		for (int i = 0; i < size; i++) {
			if (buf.size() > 0) {
				buf.append(' ');
			}
			buf.append(array[i].toString());
		}
		return buf.toString();
	}
	
}
