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
package org.openpipeline.pipeline.connector.linkqueue;

import org.openpipeline.util.XMLConfig;

/**
 * A LinkQueue maintains a list of the ids of items that have been crawled, and a 
 * crawl timestamp. It's useful when re-crawling data sources so you can 
 * locate items that haven't been crawled and may have been deleted. 
 */
public interface LinkQueue {

	/**
	 * Get the signature of the item with the given id, or -1 if the id is not found.
	 * A signature is any arbitrary value that can identify if a document has changed.
	 * It can be a timestamp, or a checksum, or a version number, or any other value 
	 * that will change when the document changes. This value should only be used 
	 * to detect changed documents. The exact value is depends on the implementation
	 * of the connector.
	 * @param id the id of the item
	 * @return a signature
	 */
	public long getSignature(String id);

	/**
	 * Update the item with the given id and set the signature and lastCrawl timestamp.
	 * If the id does not exist in the queue, it will be added. 
	 * @param id the id of the item
 	 * @param signature see the definition of signature in getSignature()
	 * @param lastCrawl timestamp of the last crawl of the item
	 */
	public void update(String id, long signature, long lastCrawl);

	/**
	 * Fetch the id of an uncrawled item. An uncrawled item is any item with
	 * a lastCrawl timestamp before the specified beforeTimestamp. 
	 * @param beforeTimestamp a timestamp in millis, typically matching the start
	 * time of the current crawl
	 * @return an id, or null if there are no more uncrawled items 
	 */
	public String fetchNextUncrawled(long beforeTimestamp);

	/**
	 * Remove the specified id from the queue. Normally called if the item
	 * cannot be found in the repository.
	 * @param id an id of a item.
	 */
	public void remove(String id);

	/**
	 * Close this queue.
	 */
	public void close();
	
	/** 
	 * Return the name of this LinkQueue implementation, for example,
	 * "MyLinkQueue"
	 */
	public String getName();
	
	/**
	 * Return a description of the LinkQueue for display in the Admin UI, for example,
	 * "LinkQueue based on XYZ database" 
	 */
	public String getDescription();
	
	/**
	 * Set parameters to configure this LinkQueue. 
	 * @param params the params to use
	 */
	public void setParams(XMLConfig params);
	
	
}
