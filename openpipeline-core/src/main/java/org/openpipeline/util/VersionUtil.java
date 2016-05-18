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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class for reading and writing a small file that stores a timestamp.
 * Can be used by connectors to keep track of a timestamp for checkpointing.
 */
public class VersionUtil {

	/**
	 * Read a timestamp from a file.  If the file does not exist a timestamp of -1 is returned.
	 * @param filename the name of the file that contains the timestamp.
	 * @return the timestamp in milliseconds
	 * @throws IOException
	 */
	public static long readTimestamp(String filename) throws IOException {
		File f = new File(filename);
		long updateTime = -1;
		if (f.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			updateTime = Long.parseLong(line);
			br.close();
		}
		return updateTime;
	}

	/**
	 * Write a timestamp to a file.  If the file does not exist then an attemp will be
	 * made to create it.
	 * @param filename the name of the file that contains the timestamp.
	 * @param millis the timestamp in milliseconds
	 * @throws IOException
	 */
	public static void writeTimestamp(String filename, long millis) throws IOException {
		File f = new File(filename);
		if (!f.exists()) {
			f.createNewFile();
		}
		FileWriter writer = new FileWriter(f);
		writer.write(String.valueOf(millis));
		writer.close();
	}
}
