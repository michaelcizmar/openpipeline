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
package org.openpipeline.pipeline.connector;

import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;

/**
 * Factory class for getting available implementations of Connectors.
 * Depends upon the existence of resource files on the classpath that contain the names of the
 * available classes. Follows the Java ServiceProvider pattern: expects the file names
 * to be in the form "org.openpipeline.pipeline.connector.Connector" (which corresponds to the interface
 * that the service provider implements). Files contain the names of the implementing classes.
 * <p>
 * See http://java.sun.com/javase/6/docs/api/java/util/ServiceLoader.html for more.
 */
public class ConnectorFactory {
	/*
	 * Implementation note:
	 * It may seem odd to use javax.imageio.spi.ServiceRegistry, but it's
	 * widely used, works fine, and is JDK 1.4 compatible.
	 * java.util.ServiceLoader was introduced in JDK 1.6, but we need to be 1.5 compatible.
	 */
	
	/**
	 * Returns an iterator over all the Connectors available in the system. Each element
	 * returned is a Connector object. It's necessary to create an actual instance
	 * of each object so we can call metadata methods like Connector.getDescription(). 
	 * @return an iterator over the available connectors
	 */
	public static Iterator getConnectors() {
		return ServiceRegistry.lookupProviders(Connector.class);
	}

	
	/**
	 * Returns a new instance of the Connector with the specified class name. This method is thread-safe.
	 * @param className name of the class to load, for example, "com.mypackage.MyConnector"
	 * @return a Connector object
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static Connector getConnector(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class classRef = Class.forName(className);
		return (Connector) classRef.newInstance();
	}
}
