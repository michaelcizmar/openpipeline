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
package org.openpipeline.pipeline.stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;

/**
 * Factory class for getting available implementations of Stages.
 * Depends upon the existence of resource files on the classpath that contain the names of the
 * available classes. Follows the Java ServiceProvider pattern: expects the file names
 * to be in the form "org.openpipeline.pipeline.stage.Stage" (which corresponds to the interface
 * that the service provider implements). Files contain the names of the implementing classes.
 * <p>
 * See http://java.sun.com/javase/6/docs/api/java/util/ServiceLoader.html for more.
 */
public class StageFactory {

	
	/**
	 * Returns an iterator over all the Stages available in the system. Each element
	 * returned is a Stage object. It's necessary to create an actual instance
	 * of each object so we can call metadata methods like Stage.getDescription(). 
	 * @return an iterator over the available stages
	 */
	public static Iterator getStages() {
		// alphabetize them
		ArrayList<Stage> list = new ArrayList();
		Iterator<Stage> it = ServiceRegistry.lookupProviders(Stage.class);
		while (it.hasNext()) {
			Stage stage = it.next();
			list.add(stage);
		}
		Collections.sort(list, new StageComp());
		return list.iterator();
	}
	
	static class StageComp implements Comparator {
		public int compare(Object o1, Object o2) {
			Stage s1 = (Stage) o1;
			Stage s2 = (Stage) o2;
			return s1.getDisplayName().compareTo(s2.getDisplayName());
		}
	}

	
	/**
	 * Returns a new instance of the Stage with the specified class name. This method is thread-safe.
	 * @param className name of the class to load, for example, "com.mypackage.MyStage"
	 * @return a Stage object
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public static Stage getStage(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class classRef = Class.forName(className);
		return (Stage) classRef.newInstance();
	}
}
