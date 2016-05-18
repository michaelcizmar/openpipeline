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
package org.openpipeline.server.pages;

import java.util.Iterator;

import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueueFactory;
import org.openpipeline.util.FastStringBuffer;

public class LinkQueuePage {
	
	private GenericConnectorPage currPage;
	
	public void processPage(GenericConnectorPage currPage) {
		this.currPage = currPage;
	}
	
	public String getLinkQueueDropdown() {
		
		FastStringBuffer options = new FastStringBuffer();
		FastStringBuffer values = new FastStringBuffer();
		
		options.append("None");
		// don't add anything to values
		
		Iterator it = LinkQueueFactory.getLinkQueues();
		while (it.hasNext()) {
			LinkQueue lq = (LinkQueue) it.next();
			String name = lq.getName();
			//String desc = lq.getDescription(); // commas in the description cause problems

			options.append(",");
			options.append(name);
			values.append(",");
			values.append(name);
		}
		
		return currPage.selectField("linkqueue-name", options.toString(), values.toString());
	}
	

}
