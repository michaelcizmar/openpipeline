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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.openpipeline.server.Users;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.XMLConfig;

public class UserPage extends AdminPage {
	
	private String userName;
	private String password;
	private PageContext pageContext;
	private boolean redirect;
	
	/**
	 * Process a request for the user.jsp page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {
		this.pageContext = pageContext;
		super.processPage(pageContext);

		try {
			ServletRequest request = pageContext.getRequest();
			String userParam = request.getParameter("user");
			String action = request.getParameter("action");
			
			Users users = Users.getInstance();
			
			XMLConfig userInfo;
			if (userParam == null || "_newuser".equals(userParam)) {
				userInfo = new XMLConfig();
			} else {
				userInfo = users.getUserInfo(userParam);
			}
			
			userName = userInfo.getProperty("username");
			password = userInfo.getProperty("password");
			
			
			if ("save".equals(action)) {
				userInfo.setProperty("username", request.getParameter("opusername"));
				userInfo.setProperty("password", request.getParameter("oppassword"));
				
				// remove all pages from the userInfo
				userInfo.removeChild("page");
				
				String [] pages = request.getParameterValues("oppage");
				if (pages != null) {
					for (String page: pages) {
						userInfo.addProperty("page", page);
					}
				}
				
				users.saveUserInfo(userInfo);
				redirect = true;
			}

		} catch (Exception e) {
			super.handleError("Error in UserPage: " + e.toString(), e);
		}
	}

	public boolean redirect() {
		return redirect;
	}
	
	public String getUserName() {
		if (userName == null) {
			return "<input type='text' name='opusername'>";
		} else {
			return userName + " <input type='hidden' name='opusername' value='" + userName + "'>";
		}
	}
	
	public String getPassword() {
		String pw = password == null ? "" : password;
		return "<input type='password' name='oppassword' value='" + pw + "'>";
	}
	
	public String getPages() throws IOException {
		if ("admin".equals(userName)) {
			return 
			"<input type=hidden name=oppage value='all'/>" +
			"<input type=checkbox checked disabled/> all";
		}
		
		ArrayList jsps = new ArrayList();
		String webappPath = pageContext.getServletContext().getRealPath("");
		getJsps(jsps, webappPath);
		
		jsps.add(0, "all");
		
		Users users = Users.getInstance();

		FastStringBuffer buf = new FastStringBuffer();
		for (int i = 0; i < jsps.size(); i++) {
			String name = (String) jsps.get(i);
			String checked = users.isAllowed(userName, name) ? " checked " : "";
			buf.append("<input type=checkbox name=oppage value='" + name + "' " + checked + "/> ");
			buf.append(name);
			buf.append("<br>");
		}
		
		return buf.toString();
	}
	
	
	private void getJsps(ArrayList jsps, String path) {
		File file = new File(path);
		File [] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			
			// get any jsp page except those under WEB-INF
			File page = files[i];
			if (page.isDirectory()) {
				// recurse
				String subPath = page.getAbsolutePath();
				if (!subPath.contains("WEB-INF")) {
					getJsps(jsps, subPath);
				}
				
			} else {
				String name = page.getName();
				if (name.endsWith(".jsp")) {
					jsps.add(name);
				}
			}
		}
	}
	
	
}
