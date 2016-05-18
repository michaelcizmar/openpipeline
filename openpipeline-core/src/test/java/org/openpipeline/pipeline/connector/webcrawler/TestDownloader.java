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
package org.openpipeline.pipeline.connector.webcrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.server.Server;
import org.openpipeline.util.ByteArray;
import org.openpipeline.util.FileUtil;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

public class TestDownloader extends TestCase {

	private String[] testFiles = { "Test.html", "Test.doc", "Test.ppt",
			"Test.pdf" };

	private Logger logger = null;

	public void test() {

		try {
			String appdir = "/temp/downloader";
			FileUtil.deleteDir(appdir);
			System.setProperty("app.home", appdir);
			logger = Server.getServer().getLogger();

			String dir = this.getClass().getResource("TestDownloader.class")
					.toString();
			dir = dir.replaceAll("TestDownloader.class", "");
			dir = dir.replaceAll("target/test-classes/", "src/test/java/");
			dir = dir.replaceAll("file:/", "");

			XMLConfig params = new XMLConfig();
			params.setProperty("output-dir", "/temp/downloader/files");

			Downloader downloader = new Downloader();
			downloader.setParams(params);
			downloader.initialize();

			for (String nextFile : testFiles) {
				try {
					if (logger != null)
						logger.info("Downloading file: " + nextFile);

					nextFile = dir + nextFile;
					/* download data */
					Item item = getItem(nextFile);
					downloader.processItem(item);

				} catch (Throwable e) {
					if (logger != null)
						logger.error("Failed to download file: " + nextFile
								+ ". Message: " + e.getMessage());
					continue;
				}
			}
		} catch (Throwable e) {
			if (logger != null)
				logger.error("Failed. Message: " + e.getMessage());
			return;
		}
	}

	/**
	 * Creates item from file data.
	 * 
	 * @param nextFile
	 *            containing the name of the file to be read
	 * @returns item containing the binary data
	 * @throws IOException
	 */
	private Item getItem(String nextFile) throws IOException {

		Item item = new Item();

		File file = new File(nextFile);
		InputStream is = new FileInputStream(file);
		long length = file.length();

		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length) {

			numRead = is.read(bytes, offset, bytes.length - offset);

			if (numRead >= 0) {
				offset += numRead;
			} else {
				break;
			}
		}
		is.close();

		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		/* Get only the name of the file */
		if (nextFile != null) {
			int index = nextFile.lastIndexOf("/");
			nextFile = nextFile.substring(index + 1);
		}

		DocBinary docBinary = new DocBinary();
		InputStream inputStream = new ByteArray(bytes);
		docBinary.setInputStream(inputStream);
		docBinary.setName(nextFile);

		item.setDocBinary(docBinary);
		item.setItemId(docBinary.getName());

		return item;
	}
}
