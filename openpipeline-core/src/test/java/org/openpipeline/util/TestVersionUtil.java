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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class TestVersionUtil extends TestCase {

	public void testReadTimestamp() throws IOException {
		// Testing reading from a non-existent file
		String nonexistingfile = "/file/does/not/exist";
		long timestamp = VersionUtil.readTimestamp(nonexistingfile);
		assertEquals(-1, timestamp);

		long expected = 1000;
		String goodPath = "/temp/stuff.txt";
		VersionUtil.writeTimestamp(goodPath, expected);
		long actual = VersionUtil.readTimestamp(goodPath);
		assertEquals(expected, actual);
		
		File f = new File(goodPath);
		f.delete();
	}

	public void testWriteTimestamp() throws IOException {
		String nonexistingfile = "/file/does/not/exist";
		try {
			VersionUtil.writeTimestamp(nonexistingfile, 1000);
			fail("An IOException should have been thrown.");
		} catch (IOException e) {
		}

		long expected = 1000;
		String goodPath = "/temp/stuff.txt";
		VersionUtil.writeTimestamp(goodPath, expected);
		long actual = VersionUtil.readTimestamp(goodPath);
		assertEquals(expected, actual);
		File f = new File(goodPath);
		f.delete();
	}

}
