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

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.openpipeline.server.Users;

public class UsersPage extends AdminPage {
	
	/**
	 * Process a request for the users.jsp page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {

		super.processPage(pageContext);

		try {
			
			Users.getInstance().reload();
			
			ServletRequest request = pageContext.getRequest();
			String userName = request.getParameter("delete");
			if (userName != null && userName.trim().length() > 0) {
				Users users = Users.getInstance();
				users.deleteUserInfo(userName);
				super.addMessage("User deleted:" + userName);
			}

		} catch (Exception e) {
			super.handleError("Error in SelectStagesPage: " + e.toString(), e);
		}
	}

	public List<String> getUserNames() throws IOException {
		return Users.getInstance().getUserNames();
	}
	
}
