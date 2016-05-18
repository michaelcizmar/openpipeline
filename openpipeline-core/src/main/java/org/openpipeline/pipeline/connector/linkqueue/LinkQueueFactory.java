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

import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;


/**
 * Returns a LinkQueue. Connectors can decide internally which type of link queue they need,
 * and optionally supply parameters to configure it. 
 */
public class LinkQueueFactory {
	
	// See ConnectorFactory for notes on how this factory works.
	
	/**
	 * Returns an iterator over all the LinkQueue implementations available in the system. Each element
	 * returned is a LinkQueue object. It's necessary to create an actual instance
	 * of each object so we can call metadata methods like LinkQueue.getDescription(). 
	 * @return an iterator over the available connectors
	 */
	public static synchronized Iterator getLinkQueues() {
		return ServiceRegistry.lookupProviders(LinkQueue.class);
	}

	
	/**
	 * Returns a new instance of the LinkQueue with the specified class name. This method is thread-safe.
	 * @param className name of the class to load, for example, "com.mypackage.MyLinkQueue"
	 * @return a Connector object
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static LinkQueue getLinkQueue(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class classRef = Class.forName(className);
		return (LinkQueue) classRef.newInstance();
	}
	
	/**
	 * Returns a new instance of the LinkQueue with the specified name. This method is thread-safe.
	 * @param name name of the queue as returned by LinkQueue.getName()
	 * @return the queue, or null if the name was not matched
	 */
	public static LinkQueue getLinkQueueByName(String name) {
		Iterator it = getLinkQueues();
		while (it.hasNext()) {
			LinkQueue queue = (LinkQueue) it.next();
			if (queue.getName().equals(name)) {
				return queue;
			}
		}
		return null;
	}
	
	
	
	
	
	
	
}
