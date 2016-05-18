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
 * Describes a span of characters in a char [] array.
 */
public interface CharSpan extends CharSequence {
	
	/**
	 * Get the underlying character array. The span covers some portion of
	 * this array.
	 * @return underlying char array
	 */
	public char[] getArray();

	/**
	 * Get the offset within the char [] array where this span starts.
	 * @return a starting offset in the array
	 */
	public int getOffset();
	
	/**
	 * Return the number of characters in this span.
	 * @return number of chars covered 
	 */
	public int size();
	
	
	/**
	 * Create a new String that contains the chars covered by this span.
	 */
	public String toString();

}
