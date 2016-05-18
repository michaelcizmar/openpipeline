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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openpipeline.pipeline.item.Item;


/**
 * File system backed by files on disk.
 */
public class DiskFileSystem implements FileSystem {
	private File file;
	private String name;
	
	public DiskFileSystem(File file) {
		this.file = file;
		this.name = file.getAbsolutePath(); // cache it because it might get called more than once
	}

	public String getFullName() {
		return name;
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}
	
	@SuppressWarnings("unused")
	public boolean getItem(Item item) {
		return false;
	}

	public FileIterator getIterator() {
		return new DiskFileIterator(file.listFiles());
	}

	public long getLastUpdate() {
		return file.lastModified();
	}

	public long getSignature() {
		return file.lastModified();
	}

	public boolean isDirectory() {
		return file.isDirectory();
	}

	public boolean isFile() {
		return file.isFile();
	}

	public long getSize() {
		return file.length();
	}
	
	public FileSystem fetch(String fullname) {
		File newFile = new File(fullname);
		if (!newFile.exists()) {
			return null;
		}
		
		DiskFileSystem newFS = new DiskFileSystem(newFile);
		newFS.name = fullname;
		return newFS;
	}
	
	public String toString() {
		return getFullName();
	}
	
	class DiskFileIterator implements FileIterator {
		private File[] files;
		private int next;

		public DiskFileIterator(File [] files) {
			this.files = files;
		}

		public boolean hasNext() {
			return next < files.length; 
		}

		public FileSystem next() {
			FileSystem nextFile = new DiskFileSystem(files[next]);
			next++;
			return nextFile;
		}
	}




}
