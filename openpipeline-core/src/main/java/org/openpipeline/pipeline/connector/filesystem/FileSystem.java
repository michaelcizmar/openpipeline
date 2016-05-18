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

import java.io.IOException;
import java.io.InputStream;

import org.openpipeline.pipeline.item.Item;

/**
 * An interface over a data structure which is similar to a hierarchical disk file system. Makes
 * it possible to iterate over files on disk, files in a zip file, and files in specialized
 * repositories in a uniform manner.
 * <p>
 * This interface is also useful for iterating over the files contained in a larger
 * "container" file, like a Microsoft .pst file or a multi-item XML file.
 * <p>
 * It is possible that isFile() and isDirectory() can both be true for a given instance
 * of this class. This would not happen for a disk file system, but there are some
 * repositories where a node can have content and also have subnodes.
 */
public interface FileSystem {
	
	/**
	 * If this file is a directory, this iterator iterates over all files
	 * and subdirectories in it. Performs a similar function to java.io.File.listFiles(),
	 * except that the iterator allows the implementation to fetch sub-files in
	 * a lazy way.
	 * @return an iterator. If there are no files in the directory, the iterator will
	 * be empty. Implementations will generally throw an exception (possibly unchecked) 
	 * if this file is not a directory, so always check isDirectory() first.
	 * @throws IOException
	 */
	public FileIterator getIterator() throws IOException;

	/**
	 * Get the full name, including an absolute path, of the file. The format of
	 * the name will be dependent on the particular implementation.
	 * @return a name
	 */
	public String getFullName();
	
	/**
	 * Get an InputStream that contains the data in the file. Only returns an
	 * InputStream if isFile() is true. For some implementations, may 
	 * return null, even for files; these implementations may return
	 * an Item from getItem() instead.
	 * @return an InputStream or null
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException;
	
	/**
	 * Populates an Item with the data contained in this file. Will
	 * only populate the Item if 1) isFile() is true, 2) there is actually
	 * data available in this file, and 3) this particular
	 * implementation returns items. The implementation may return data
	 * via getInputStream() instead.
	 * @return true if the item was populated, else false.
	 * @throws IOException
	 */
	public boolean getItem(Item item) throws IOException;
	
	
	/**
	 * Returns a timestamp, in millis, of the time the file was last updated. 
	 * @return a timestamp, or -1 if the timestamp is not available
	 */
	public long getLastUpdate();
	
	/**
	 * Returns a long value that can help identify if a document has changed. The exact
	 * value is implementation dependent. It can be a timestamp, or a checksum, or a version
	 * number, or any other value that will change when the document changes. This
	 * value should only be used to detect changed documents.
	 * @return a long value
	 */
	public long getSignature();
	
	/**
	 * Returns true if this entry is a directory, and it's possible to call
	 * getFileIterator() to iterate over files and subdirectories in it.
	 * @return true if this file is a directory
	 */
	public boolean isDirectory();
	
	/**
	 * Returns true if this entry is a file and there is an InputStream available for it.
	 */
	public boolean isFile();
	
	/**
	 * Return the length, in bytes, of the file. Undefined for directories.
	 * @return a length
	 */
	public long getSize();
	
	/**
	 * Returns the file or directory that has the specified name. The format of
	 * the name is implementation-dependent, but it should be the same as is
	 * returned by getFullName(), and should include full path information.
	 * @param fullname the name of the file
	 * @return a FileSystem object, or null if the name was not found
	 */
	public FileSystem fetch(String fullname) throws IOException;
	
}
