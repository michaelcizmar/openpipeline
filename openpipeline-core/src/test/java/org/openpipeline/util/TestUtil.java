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

import junit.framework.TestCase;

public class TestUtil extends TestCase {
	
	public void testTrim() {
		String [] cases =   {"aaa", "a", "", " ", "   ", " \t  a \r\n" };
		String [] correct = {"aaa", "a", "", "", "", "a"};
		
		for (int i = 0; i < cases.length; i++) {
			StringBuilder buf = new StringBuilder(cases[i]);
			Util.trimWhitespace(buf);
			String out = buf.toString();
			if (!out.equals(correct[i])) {
				fail("case #" + i + " value=" + cases[i]);
			}
		}
	}

}
