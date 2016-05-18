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
package org.openpipeline.util;

import java.util.Arrays;



/**
 * Resizable array of integers, with several useful utility functions.
 */
public class IntArray {
	private int[] array;
	private int size = 0;
	private int position = 0;

	public IntArray() {
		this(16);
	}

	public IntArray(int initialCapacity) {
		array = new int[initialCapacity];
	}

	public IntArray(int[] array, int size) {
		this.array = array;
		this.size = size;
	}

	public int[] getArray() {
		return array;
	}

	public void setArray(int[] array) {
		this.array = array;
		this.size = array.length;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void append(int value) {
		size++;
		ensureCapacity(size);
		array[size - 1] = value;
	}

    public void append(int[] arr, int offset, int len) {
        ensureCapacity(size + len);
        System.arraycopy(arr, offset, array, size, len);
        size += len;
    }

    public void append(IntArray intArray) {
    	int len = intArray.size;
        ensureCapacity(size + len);
        System.arraycopy(intArray.array, 0, array, size, len);
        size += len;
    }
    
    
	public void ensureCapacity(int capacity) {
		if (array.length < capacity) {
			int[] newArray = new int[capacity << 1]; // double it
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
	}

	public void ensureCapacityNoCopy(int capacity) {
		if (array.length < capacity) {
			array = new int[capacity << 1]; // double it
		}
	}

	public int get(int index) {
		return array[index];
	}

	public int get() {
		return array[position++];
	}

	public int getCurrent() {
		return array[position];
	}

	public void put(int index, int value) {
		array[index] = value;
	}

	public void put(int value) {
		array[position++] = value;
	}

	public int size() {
		return size;
	}

	public void setSize(int size) {
		//ensureCapacity(size);
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

	public int binarySearch(int key) {
		return Arrays.binarySearch(array, 0, size, key);
	}

	public boolean equals(IntArray newArr) {
		if (size != newArr.size)
			return false;
		int[] newArray = newArr.array;
		for (int i = 0; i < size; i++) {
			if (array[i] != newArray[i])
				return false;
		}
		return true;
	}

	/**
	 * Remove "count" integers from the array starting at "offset".
	 * @param offset index of the first offset to remove
	 * @param count number of integers to remove
	 */
	public void remove(int offset, int count) {
		System.arraycopy(array, offset + count, array, offset, size - (offset + count));
		size = size - count;
	}


	/*   This is sort code *************************************/
	
	public void sort() {
		java.util.Arrays.sort(array, 0, size);
	}

	/**
	 * Remove any ints that are not in itemnums. Assumes that
	 * both arrays are sorted in ascending order.
	 */
	public void intersection(IntArray itemnums) {
		int ptr = 0;
		int [] otherArr = itemnums.getArray();
		int otherSize = itemnums.size();
		int otherPtr = 0;
		
		for (int i = 0; i < size; i++) {
			int target = array[i];
			while (otherPtr < otherSize) {
				int other = otherArr[otherPtr];
				if (other < target) {
					otherPtr++;
				} else if (other > target) {
					break;
				} else { // other == target
					array[ptr] = target;
					ptr++;
					break;
				}
			}
		}
		size = ptr;
	}

	/**
	 * Add ints in itemnums, and remove dupes. Assumes arrays are sorted.
	 */
	public void union(IntArray itemnums) {
		append(itemnums);
		sort();
		
		// de-dupe
		int ptr = 0;
		int prev = -1;
		for (int i = 0; i < size; i++) {
			if (!(array[i] == prev)) {
				array[ptr] = array[i];
				ptr++;
				prev = array[i];
			}
		}
		setSize(ptr);
	}

	public void remove(IntArray itemnums) {
		
		int ptr = 0;
		int [] otherArr = itemnums.getArray();
		int otherSize = itemnums.size();
		int otherPtr = 0;
		
		for (int i = 0; i < size; i++) {
			int target = array[i];
			boolean found = false;
			while (otherPtr < otherSize) {
				int other = otherArr[otherPtr];
				if (other < target) {
					otherPtr++;
				} else if (other > target) {
					break;
				} else { // other == target
					found = true;
					break;
				}
			}
			if (!found) {
				array[ptr] = target;
				ptr++;
			}
		}
		size = ptr;
	}

	public int compareTo(IntArray other) {
		
    	int otherSize = other.size();
    	int [] otherArray = other.getArray();
        int commonLen = otherSize < size ? otherSize : size;

        for (int i = 0; i < commonLen; i++) {
        	int cmp = array[i] - otherArray[i];
            if (cmp != 0)
                return cmp;
        }
        
        return size - otherSize;
    }

	/**
	 * Removes duplicate entries from a sorted list of integers. IMPORTANT:
	 * assumes the ints are already sorted.
	 */
	public void dedupe() {
		int ptr = 0;
		int prev = Integer.MIN_VALUE;
		for (int i = 0; i < size; i++) {
			int elem = array[i];
			if (elem != prev) {
				array[ptr] = elem;
				prev = elem;
				ptr++;
			}
		}
		size = ptr;
	}

	/**
	 * Return true if the array is sorted in ascending order.
	 * @return true if the contents of the array are sorted.
	 */
	public boolean isSorted() {
		int prev = Integer.MIN_VALUE;
		for (int i = 0; i < size; i++) {
			int val = array[i];
			if (val < prev) {
				return false;
			}
			prev = val;
		}
		return true;
	}

	/**
	 * Starting at the current position, find the value and increment the
	 * position to point to it. Assumes that the
	 * array is sorted in ascending order.
	 * @param value to look for
	 * @return position where the value is found, or -1 if not found
	 */
	public int find(int value) {
		
		while (array[position] < value) {
			position++;
			if (eof()) {
				return -1;
			}
		}
		
		if (array[position] == value) {
			return position;
		} else {
			return -1;
		}
	}

}
