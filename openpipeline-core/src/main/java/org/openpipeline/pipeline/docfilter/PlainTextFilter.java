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
package org.openpipeline.pipeline.docfilter;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.StandardAttributeNames;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.FastStringBuffer;

/**
 * A filter for plain text files. The entire file is loaded into an attribute
 * with an attributeId of "text".
 */
public class PlainTextFilter extends DocFilter {

	private FastStringBuffer buf = new FastStringBuffer(16 * 1024);

	@Override
	public void processItem(Item item) throws PipelineException {

		try {

			DocBinary docBinary = item.getDocBinary();
			InputStream binary = docBinary.getInputStream();

			InputStreamReader reader;
			if (docBinary.getEncoding() == null) {
				reader = new InputStreamReader(binary);
			} else {
				reader = new InputStreamReader(binary, docBinary.getEncoding());
			}

			buf.clear();
			if (docBinary.getSize() > 0) {
				buf.ensureCapacity((int)docBinary.getSize());
			}
			
			buf.append(reader);

			if (buf.size() > 0) {
				item.getRootNode().addNode(StandardAttributeNames.TEXT,
						buf.getArray(), 0, buf.size());
			}

		} catch (Throwable t) {
			throw new PipelineException(t);
		}

		super.pushItemDownPipeline(item);
	}

	@Override
	public String[] getDefaultExtensions() {
		String[] exts = { "txt" };
		return exts;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		String[] mimetypes = { "text/plain" };
		return mimetypes;
	}

	@Override
	public String getDescription() {
		return "Parses plain text files";
	}

	@Override
	public String getDisplayName() {
		return "PlainTextFilter";
	}

	@Override
	public String getDocType() {
		return "txt";
	}

}
