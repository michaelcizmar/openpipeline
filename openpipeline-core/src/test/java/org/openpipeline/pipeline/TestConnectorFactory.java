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
package org.openpipeline.pipeline;

import java.util.Iterator;

import junit.framework.TestCase;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.pipeline.connector.ConnectorFactory;

public class TestConnectorFactory extends TestCase {
	

	public void test() {
	
		Iterator it = ConnectorFactory.getConnectors();
		while (it.hasNext()) {
			Connector con = (Connector) it.next();
			
			System.out.println(con.getDescription());
		}
		
		
	}
	
	
	
}
