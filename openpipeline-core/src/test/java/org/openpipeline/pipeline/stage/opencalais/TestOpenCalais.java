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
package org.openpipeline.pipeline.stage.opencalais;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.ws.Service;

import junit.framework.TestCase;

	
public class TestOpenCalais extends TestCase {
	private String openCalaisURL = "http://api.opencalais.com/enlighten/?wsdl";
	
	private String LICENSE_ID = "license here";
	
	public void test() throws MalformedURLException{
		URL wsdlLocation = new URL(this.openCalaisURL);
		Service service = Service.create(wsdlLocation, Calais.SERVICE);
		CalaisSoap calaisSoap =  service.getPort(Calais.CalaisSoap12, CalaisSoap.class);
		
		String response = calaisSoap.enlighten(LICENSE_ID, "content here", "");
		System.out.println(response);
	}
}
