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
package org.openpipeline.pipeline.connector.filesystem;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openpipeline.pipeline.item.Item;

/**
 * A wrapper around a zip file that makes it look like a file system.
 */
public class ZipFileSystem implements FileSystem {
	private File file;
	
	public ZipFileSystem(File file) {
		this.file = file;
	}

	public String getFullName() {
		return file.getAbsolutePath();
	}

	public InputStream getInputStream() {
		throw new UnsupportedOperationException();
	}
	
	@SuppressWarnings("unused")
	public boolean getItem(Item item) {
		return false;
	}


	public FileIterator getIterator() throws FileNotFoundException {
		return new ZipFileIterator(file);
	}

	public long getLastUpdate() {
		throw new UnsupportedOperationException();
	}

	public long getSignature() {
		throw new UnsupportedOperationException();
	}

	public boolean isDirectory() {
		return true;
	}

	public boolean isFile() {
		return false;
	}
	
	public long getSize() {
		return file.length(); // this shouldn't be used
	}
	
	@SuppressWarnings("unused")
	public FileSystem fetch(String fullname) {
		// not supported
		return null;
	}
	
	
	public String toString() {
		return getFullName();
	}
	
	class ZipFileIterator implements FileIterator {
		private InputStream in;
		private ZipInputStream zipStream;
		private ZipEntry zipEntry;
		
		public ZipFileIterator(File file) throws FileNotFoundException {
			in = new FileInputStream(file);
			zipStream = new ZipInputStream(in);
		}
		
		public boolean hasNext() throws IOException {
			
			while (true) {
				zipEntry = zipStream.getNextEntry();
				if (zipEntry == null) {
					zipStream.close();
					in.close();
					return false;
				}
				
				// skip directories. zip files aren't hierarchical; all files
				// come in one big list
				if (zipEntry.isDirectory()) {
					continue;
				}
				
				// don't do nested zip files
				if (zipEntry.getName().toLowerCase().endsWith(".zip")) {
					continue;
				}
				break;
			}
			
			return true;
		}

		public FileSystem next() {
			return new ZipFile(file, zipEntry, zipStream);
		}
	}

	
	/**
	 * Wraps a ZipEntry.
	 */
	class ZipFile implements FileSystem {
		private ZipInputStream zipStream;
		private ZipEntry zipEntry;
		private String filePath;

		public ZipFile(File file, ZipEntry zipEntry, ZipInputStream zipStream) {
			this.zipStream = zipStream;
			this.zipEntry = zipEntry;
			this.filePath = file.getAbsolutePath() + "/";
		}

		/**
		 * Is returned in the form /mydir/myzipfile.zip/myfilename.txt
		 */
		public String getFullName() {
			return filePath + zipEntry.getName();
		}

		public InputStream getInputStream() {
			// see below for the reason behind NonClosableInputStream
			return new NonClosableInputStream(zipStream);
		}
		
		@SuppressWarnings("unused")
		public boolean getItem(Item item) {
			return false;
		}


		public FileIterator getIterator() {
			throw new UnsupportedOperationException();
		}

		public long getLastUpdate() {
			return zipEntry.getTime();
		}

		public long getSignature() {
			return zipEntry.getTime();
		}

		public boolean isDirectory() {
			return false;
		}

		public boolean isFile() {
			return true;
		}
		
		public long getSize() {
			return zipEntry.getSize();
		}

		@SuppressWarnings("unused")
		public FileSystem fetch(String fullname) {
			// not supported
			return null;
		}
		
		public String toString() {
			return getFullName();
		}

	}

	/**
	 * A simple InputStream that ignores calls to close(). The reason is that
	 * DocFilters often close InputStreams when they're done with them, and
	 * that causes problems for ZipInputStream. The stream needs to stay open
	 * so the next document can be retrieved from it.
	 */
	class NonClosableInputStream extends BufferedInputStream {

		public NonClosableInputStream(InputStream in) {
			super(in);
		}
		
		@Override
		public void close() {
		}
	}



}
