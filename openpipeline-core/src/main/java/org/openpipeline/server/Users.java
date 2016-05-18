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
package org.openpipeline.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openpipeline.util.XMLConfig;

/**
 * Manages users, passwords, and their permissions.
 */
public class Users {
	
	/* file format of users.xml:
	 * <users>
	 *   <user>
	 *     <username>admin</username>
	 *     <password>admin</password>
	 *     <!-- admin can always see all pages -->
	 *   </user>
	 *   <user>
	 *     <username>bob</username>
	 *     <password></password>
	 *     <page>all</page>             <!-- all pages allowed -->
	 *   </user>
	 *   <user>
	 *     <username>fred</username>
	 *     <password></password>
	 *     <page>add_job.jsp</page>     <!-- only specific pages allowed -->
	 *     <page>view_jobs.jsp</page> 
	 *   </user>
	 * </users>
	 */
	
	private static final String FILENAME = "config/users.xml";
	private static Users users;
	
	private XMLConfig userXML;
	private HashMap namePageMap;
	
	synchronized static public Users getInstance() throws IOException {
		if (users == null) {
			users = new Users();
			users.reload();
		}
		return users;
	}
	
	public void reload() throws IOException {
		String homeDir = Server.getServer().getHomeDir();
		userXML = new XMLConfig();
		namePageMap = new HashMap();
		
		File file = new File(homeDir, FILENAME);
		if (file.exists()) {
			userXML = new XMLConfig();
			userXML.load(file);
		}
		
		if (userXML.getChildren().size() == 0) {
			// add the default admin user
			userXML.setName("users");
			XMLConfig child = userXML.addChild("user");
			child.addProperty("username", "admin");
			child.addProperty("password", "admin");
			child.addProperty("page", "all");
			save();
		}
		
		loadNamePageMap();
	}

	private void save() throws IOException {
		File file = new File(Server.getServer().getHomeDir(), FILENAME);
		userXML.save(file);
	}

	/**
	 * The namePageMap include an entry for every username:pagename combination.
	 * It provides a very fast way to see if a particular user can see a 
	 * particular page.
	 */
	private void loadNamePageMap() {
		for (XMLConfig child: userXML.getChildren()) {
			String username = child.getProperty("username");

			List <String> pages = child.getValues("page");
			for (String page: pages) {
				String key = username + ":" + page;
				namePageMap.put(key, key);
			}
		}
	}

	/**
	 * Return true if this user is allowed to see this page.
	 * @param userName the username
	 * @param pageName the page name, for example, "foo.jsp"
	 * @return true if access allowed
	 */
	public boolean isAllowed(String userName, String pageName) {
		
		String key = userName + ":" + pageName;
		if (namePageMap.containsKey(key)) {
			return true;
		}
		
		key = userName + ":all";
		if (namePageMap.containsKey(key)) {
			return true;
		}
		
		return false;
	}

	public List<String> getUserNames() {
		List list = new ArrayList();
		for (XMLConfig child: userXML.getChildren()) {
			String un = child.getProperty("username");
			list.add(un);
		}
		return list;
	}
	
	public void saveUserInfo(XMLConfig userInfo) throws IOException {
		userInfo.setName("user");
		String userName = userInfo.getProperty("username");
		int num = findUser(userName);
		if (num == -1) {
			userXML.addChild(userInfo);
		} else {
			userXML.getChildren().set(num, userInfo); // replace old node with new one
		}
		save();
		reload();
	}


	public boolean deleteUserInfo(String userName) throws IOException {
		int num = findUser(userName);
		if (num == -1) {
			return false;
		} else {
			userXML.getChildren().remove(num);
			save();
			reload();
			return true;
		}
	}

	public XMLConfig getUserInfo(String userParam) {
		int num = findUser(userParam);
		if (num == -1) {
			return null;
		} else {
			return userXML.getChildren().get(num);
		}
	}
	
	private int findUser(String userName) {
		if (userName == null) {
			return -1;
		}
		
		List<XMLConfig> children = userXML.getChildren();
		for (int i = 0; i < children.size(); i++) {
			XMLConfig child = children.get(i);
			String un = child.getProperty("username");
			if (userName.equals(un)) {
				return i;
			}
		}
		return -1;
	}

	public boolean tryLogin(String userParam, String passwordParam) {
		
		int num = findUser(userParam);
		if (num == -1) {
			return false;
		}
		
		XMLConfig userInfo = userXML.getChildren().get(num);
		String password = userInfo.getProperty("password");

		if (passwordParam == null)
			passwordParam = "";
		if (password == null) 
			password = "";
		
		if (password.equals(passwordParam)) {
			return true;
		}
		
		return false;
	}
	
	
}
