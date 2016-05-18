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
 * NOT YET IMPLEMENTED. See notes inside the class for more.
 * <p>
 * Provides a way of iterating over the items in an XML file
 * in multi-item format.
 * <p>
 * See the {@link FileSystem} interface for more on container files.
 */
public class MultiItemXMLFileSystem implements FileSystem {
	
	/*
	 * The purpose of this class is to move processing of multi-item
	 * xml files into the connector, instead of the XMLFilter.
	 * The benefit is that you can use the LinkQueue to remove
	 * individual deleted items from such files, instead of looking at 
	 * the file as a whole.
	 */

	private String itemId;
	
	public FileSystem fetch(String fullname) throws IOException {
		// this class is not able to do random access into a file
		return null;
	}

	public String getFullName() {
		return itemId;
	}

	public InputStream getInputStream() throws IOException {
		return null;
	}

	public boolean getItem(Item item) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public FileIterator getIterator() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public long getLastUpdate() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getSignature() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isDirectory() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isFile() {
		// TODO Auto-generated method stub
		return false;
	}

}
