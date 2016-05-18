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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.openpipeline.server.Users;
import org.openpipeline.util.FastStringBuffer;

public class LoginPage extends AdminPage {
	
	private static final String LOGIN_ATTR_NAME = "loggedin";
	private static final String LOGIN_USERNAME = "login_username";
	private static final String LOGIN_COOKIE_NAME = "login";
	
	private PageContext pageContext;

	public void setPageContext(PageContext pageContext) {
		this.pageContext = pageContext;
	}
	
	/**
	 * Log in or log out of the server. Looks for request parameters
	 * "logout" to log out and "opuser"/"oppassword" if attempting to log in.  
	 * @return true if logged in
	 * @throws IOException 
	 */
	public boolean login() throws IOException {
		
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
		HttpSession session = pageContext.getSession();

		String logout = request.getParameter("logout");
		String user = request.getParameter("opuser");
		String password = request.getParameter("oppassword");
		String rememberMe = request.getParameter("remember");

		// check to see if there is a cookie with the password. 
		// enables the Remember Me feature
		if (user == null) {
			String [] userPass = getLoginFromCookie(request);
			user = userPass[0];
			password = userPass[1];
		}

		boolean isLoggedIn = false;
		
		if (logout != null) {
			deleteCookie(response);
			isLoggedIn = false;
			setLoginAttr(session, null, null);
			

		} else if (user != null) {
			
			if (Users.getInstance().tryLogin(user, password)) {
				if (rememberMe != null) {
					setLoginCookie(response, user, password);
				}
				isLoggedIn = true;
				setLoginAttr(session, "true", user);
				
			} else {
				isLoggedIn = false;
				setLoginAttr(session, null, null);
				super.addMessage("Incorrect username or password");
			}
		}
		return isLoggedIn;
	}




	private static void setLoginAttr(HttpSession session, String value, String username) {
		session.setAttribute(LOGIN_ATTR_NAME, value);
		session.setAttribute(LOGIN_USERNAME, username);
	}
	
	
	public boolean isLoggedIn() throws IOException {
		HttpSession session = pageContext.getSession();
		if (session.getAttribute(LOGIN_ATTR_NAME) != null) {
			return true;
		}
		
		// if we get here, this is probably the initial call to this method
		// check to see if we can log in automatically
		String [] userPass = getLoginFromCookie((HttpServletRequest)pageContext.getRequest());
		String user = userPass[0];
		String password = userPass[1];
		
		if (Users.getInstance().tryLogin(user, password)) {
			setLoginAttr(session, "true", user);
			return true;
		}
		
		return false;
	}
	
	/**
	 * If the user is allowed to see the page, return the item, else return "".
	 * @param item the menu item
	 * @param pageName the name of the page to test
	 * @return the item or ""
	 */
	public String showMenuItem(String item, String pageName) throws IOException {
		if (isAllowed(pageName)) {
			return item;
		}
		return "";
	}
	
	/**
	 * If the user is allowed to see any of the listed pages, return the header.
	 * @param head the menu header
	 * @param pageNames the name of the pages to test
	 * @return the header or ""
	 */
	public String showMenuHeader(String head, String [] pageNames) throws IOException {
		for (String name: pageNames) {
			if (isAllowed(name)) {
				return head;
			}
		}
		return "";
	}
	
	/**
	 * Show a submenu, limiting the items to what the user is allowed to see
	 * @param header submenu header
	 * @param items menu items, rows separated with tab and the item/page separated with |
	 * @return the menu for display
	 * @throws IOException 
	 */
	public String menu(String header, String items) throws IOException {
		FastStringBuffer buf = new FastStringBuffer(256);
		buf.append("<dl><dt>");
		buf.append(header);
		buf.append("</dt>");
		
		int count = 0;
		String [] rows = items.split("\t");
		for (String row: rows) {
			String [] parts = row.split("\\|");
			String name = parts[0];
			String page = parts[1];
			if (isAllowed(page)) {
				buf.append("<dd><a href='");
				buf.append(page);
				buf.append("'>");
				buf.append(name);
				buf.append("</a></dd>");
				count++;
			}
		}
		if (count == 0) {
			return "";
		} else {
			buf.append("</dl>");
			return buf.toString();
		}
	}
	 
	
	/**
	 * Return true if the current user is allowed to see this page.
	 * @param pageName the page name, for example, "foo.jsp"
	 * @return true if access allowed
	 * @throws IOException 
	 */
	public boolean isAllowed(String pageName) throws IOException {
		String userName = (String) pageContext.getSession().getAttribute(LOGIN_USERNAME);
		
		// test only the page name, not the path
		int pos = pageName.lastIndexOf('/');
		if (pos > -1) {
			pageName = pageName.substring(pos + 1);
		}
		
		// these should always be visible
		if (pageName.equals("index.jsp") || pageName.startsWith("login.jsp")) {
			return true;
		}
		
		return Users.getInstance().isAllowed(userName, pageName);
	}
	
	
	private static String [] getLoginFromCookie(HttpServletRequest request) {
		String [] userPass = new String [2];
		
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (LOGIN_COOKIE_NAME.equals(cookie.getName())) {
					
					String loginStr = cookie.getValue();
					
					// the format is 2 chars for len of username, username, password
					int userSize = Integer.parseInt(loginStr.substring(0, 2));
					userPass[0] = loginStr.substring(2, 2 + userSize);
					userPass[1] = loginStr.substring(2 + userSize);
					break;
				}
			}
		}

		return userPass;
	}
	
	private void setLoginCookie(HttpServletResponse response, String user, String password) {
		
		int userSize = user.length();
		String userSizeStr = userSize + "";
		if (userSizeStr.length() == 1) {
			userSizeStr = "0" + userSizeStr; // pad to two digits
		}

		String loginStr = userSizeStr + user + password;
		
		Cookie cookie = new Cookie(LOGIN_COOKIE_NAME, loginStr);
		cookie.setMaxAge(60 * 60 * 24 * 365); // one year in seconds
		response.addCookie(cookie);
	}

	
	
	private void deleteCookie(HttpServletResponse response) {
		// delete the cookie. the only way to do it is
		// to set max age and send it back
		Cookie loginCookie = new Cookie(LOGIN_COOKIE_NAME, "");
		loginCookie.setMaxAge(0);
		response.addCookie(loginCookie);
	}
	
}
