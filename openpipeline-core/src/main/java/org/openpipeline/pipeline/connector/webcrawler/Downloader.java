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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.stage.Stage;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.FastStringBuffer;

/**
 * Writes items to disk. Each item is written to a file.
 */
public class Downloader extends Stage {

	private static final String DEFAULT_OUTPUT_DIR = "/temp/items";
	private Stage nextStage = null;
	private String outputDir;
	private FastStringBuffer buf = new FastStringBuffer();

	/**
	 * Writes an item to a file.
	 * 
	 * @param item
	 *            containing the data
	 * @throws PipelineException
	 */
	public void processItem(Item item) throws PipelineException {

		if (item == null) {
			throw new PipelineException("Item is null.");
		}

		String filename = item.getItemId();
		try {
			// get a filename for the item. start with the url, then the
			// item_id, then
			// generate a random one

			if (filename == null) {
				filename = item.getItemId();
				if (filename == null) {
					filename = UUID.randomUUID().toString();
				}
			}

			File file = new File(outputDir, fixFilename(filename));

			if (file.exists()) {
				file.delete();
			} else {
				file.getParentFile().mkdirs();
			}

			byte[] output = getBinary(item);

			FileOutputStream outputStream = new FileOutputStream(file);
			outputStream.write(output);
			outputStream.close();

		} catch (Throwable e) {
			throw new PipelineException("Could not write itemID: "
					+ item.getItemId() + " to file: " + filename
					+ ". Message: " + e.getMessage());
		}

		try {
			if (nextStage != null) {
				nextStage.processItem(item);
			}
		} catch (PipelineException e) {
			throw new PipelineException("PipelineException for itemID: "
					+ item.getItemId() + ". Message: " + e.getMessage());
		}
	}

	/**
	 * Reads the binary data from the item into byte[].
	 * 
	 * @param item
	 *            containing the data
	 * @return data as byte[]
	 * @throws IOException
	 */
	private byte[] getBinary(Item item) throws IOException {

		DocBinary docBinary = item.getDocBinary();
		InputStream itemData = docBinary.getInputStream();
		long size = docBinary.getSize();
		if (size <= 0) {
			size = 100 * 1024;// guess
		}
		byte[] output = new byte[(int) size];
		int readBytes = itemData.read(output);
		if (readBytes <= 0) {
			return new byte[0];
		}

		return output;
	}

	/**
	 * Convert bad chars to underscores for the filename.
	 */
	private String fixFilename(String name) {
		buf.clear();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (Character.isLetterOrDigit(ch) || ch == '/' || ch == '\\'
					|| ch == '.') {
				buf.append(ch);
			} else {
				buf.append('_');
			}
		}
		return buf.toString();
	}

	public void initialize() {
		outputDir = params.getProperty("output-dir", DEFAULT_OUTPUT_DIR);
	}

	public void flush() {
	}

	public void close() {
	}

	@Override
	public String getDescription() {
		return "Writes items to files on disk.";
	}

	@Override
	public String getDisplayName() {
		return "Downloader";
	}

	public String getConfigPage() {
		return "stage_downloader.jsp";
	}
}
