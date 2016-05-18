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

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.openpipeline.util.ByteArray;
import org.openpipeline.util.Util;

/**
 * A data structure that carries the binary content of a file plus some file
 * metadata. A DocBinary object could carry a PDF file, a MS Word document, or
 * any other content. The purpose of this class is to allow Connector classes to
 * attach binary content to an Item and pass it down the pipeline. A stage,
 * possibly a DocFilter stage, can process the binary and insert the extracted
 * text into the Item.
 */
public class DocBinary {
	private InputStream stream;
	private ByteArray binary = new ByteArray();
	private long timestamp;
	private String mimeType;
	private String name;
	private String encoding;
	private long size = -1;

	/**
	 * Sets the binary content. An internal buffer will be cleared, and then
	 * content of the InputStream be read into it.
	 * 
	 * @param in
	 *            an InputStream with the content
	 * @throws IOException
	 */
	public static ByteArray  getBinary(InputStream stream) throws IOException {
		ByteArray binary = new ByteArray();
		binary.append(stream);
		return binary;
	}

	/**
	 * Return the date/time the document was last updated, in milliseconds.
	 * 
	 * @return a timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the date/time the document was last updated, in milliseconds.
	 * 
	 * @param timestamp
	 *            the new lastupdate value
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Return the mime type of the document. See <a
	 * href="http://en.wikipedia.org/wiki/MIME_type">mime type</a>
	 * 
	 * @return a standard mime type
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Set the mime type of the document. See <a
	 * href="http://en.wikipedia.org/wiki/MIME_type">mime type</a>
	 * 
	 * @param mimeType
	 *            a standard mime type
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Get the name of the document. The name should be a URL, if available, or
	 * a full filepath, or a filename.
	 * 
	 * @return the name assigned to the document
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the document. The name should be a URL, if available, or
	 * a full filepath, or a filename.
	 * 
	 * @param name
	 *            name of the document
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Clear the internal variables.
	 */
	public void clear() {
		binary = null;
		timestamp = 0;
		mimeType = null;
		name = null;
	}

	/**
	 * Set the encoding of the binary data. Optional; may apply in cases where
	 * the input is plain text or HTML, but will not apply in cases where the
	 * document specifies its own encoding.
	 * 
	 * @param encoding
	 *            an encoding string, for example, "UTF-8" or "ISO-8859-1". Must
	 *            be one supported by the JVM.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get the encoding of the binary data. Optional; may apply in cases where
	 * the input is plain text or HTML, but will not apply in cases where the
	 * document specifies its own encoding.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Parse the URL to try to get an extension for this file.
	 * 
	 * @return an extension, or null if one cannot be detected
	 */
	public String getExtension() {
		String ext = Util.getExtension(name);
		return ext;
	}

	/**
	 * Set the size of the document in bytes.
	 * 
	 * @param size
	 *            size of doc in bytes
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Get the size of the document in bytes if known. If the size is not known,
	 * return -1
	 * 
	 * @return the size in bytes or -1
	 */
	public long getSize() {
		return size;
	}

	public InputStream getInputStream() {
		return stream;
	}

	public void setInputStream(InputStream stream) {
		this.binary.clear();
		this.stream = stream;
	}

}
