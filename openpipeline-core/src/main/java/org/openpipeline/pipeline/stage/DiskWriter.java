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
package org.openpipeline.pipeline.stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.StandardAttributeNames;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.ByteArray;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.FileUtil;

/**
 * Writes items to disk in XML format. Each item is written to a file. The
 * filename is the itemId with ".xml" appended.
 */
public class DiskWriter extends Stage {

	public static final String DEFAULT_OUTPUT_DIR = "/temp/items";

	private String outputDir;
	private boolean includeAnnotations;
	private boolean includeBinary;

	private FastStringBuffer xmlBuf = new FastStringBuffer();

	@Override
	public void processItem(Item item) throws PipelineException {

		try {
			File file = null;

			// write the binary
			if (includeBinary) {
				DocBinary docBinary = item.getDocBinary();
				if (docBinary != null) {
					String name = docBinary.getName();
					if (name != null) {
						file = new File(outputDir, FileUtil.fixFilename(name));
						clearFile(file);
						try (ByteArray buf = DocBinary.getBinary(docBinary
								.getInputStream())) {
							buf.setPosition(0);
							FileUtil.writeStreamToFile(buf, file);
						}
					}
				}
			}

			// if a filename was created above, use it with an .xml extension,
			// else create one. Start with the url, then the item_id, then
			// generate a random one. The url preserves the directory structure
			// of the
			// files, if any

			if (file == null) {
				String url = item.getRootNode().getChildValue(
						StandardAttributeNames.URL);
				String itemId = item.getItemId();

				String filename = (url == null ? "" : url)
						+ (itemId == null ? "" : itemId);
				if (filename.length() == 0) {
					filename = UUID.randomUUID().toString();
				}

				file = new File(outputDir, FileUtil.fixFilename(filename)
						+ ".xml");
			} else {
				file = new File(file.toString() + ".xml");
			}

			clearFile(file);

			xmlBuf.clear();
			xmlBuf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			item.appendXMLtoBuffer(xmlBuf, includeAnnotations);

			FileOutputStream fos = new FileOutputStream(file);
			Writer writer = new BufferedWriter(new OutputStreamWriter(fos,
					"UTF-8"));
			writer.write(xmlBuf.getArray(), 0, xmlBuf.size());
			writer.close();

			super.pushItemDownPipeline(item);

		} catch (Throwable e) {
			throw new PipelineException(e);
		}
	}

	/**
	 * Clear out the old file, set up parent directories
	 */
	private void clearFile(File file) {
		if (file.exists()) {
			file.delete();
		} else {
			file.getParentFile().mkdirs();
		}
	}

	@Override
	public void initialize() {
		outputDir = params.getProperty("output-dir", DEFAULT_OUTPUT_DIR);
		includeAnnotations = params.getBooleanProperty("include-annotations",
				false);
		includeBinary = params.getBooleanProperty("include-binary", true);
	}

	@Override
	public String getDescription() {
		return "Writes items to files on disk in XML format";
	}

	@Override
	public String getDisplayName() {
		return "Disk Writer";
	}

	public String getConfigPage() {
		return "stage_disk_writer.jsp";
	}

}
