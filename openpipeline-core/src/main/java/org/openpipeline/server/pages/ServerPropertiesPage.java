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

import javax.servlet.jsp.PageContext;

import org.openpipeline.server.Server;

public class ServerPropertiesPage extends AdminPage {

	
	private String[] propsToSave;


	/**
	 * Process a request for the server_properties page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {
		super.processPage(pageContext);

		try {

			if (super.getParam("update") != null) {
				Server server = Server.getServer();
				for (int i = 0; i < propsToSave.length; i++) {
					String name = propsToSave[i];
					String value = super.getParam(name);
					server.setProperty(name, value);
				}

				server.saveProperties();
			} 

		} catch (Throwable t) {
			super.handleError("Error on server properties page", t);
		}
	}


	public void setPropertiesToSave(String [] propsToSave) {
		this.propsToSave = propsToSave;
	}

}
