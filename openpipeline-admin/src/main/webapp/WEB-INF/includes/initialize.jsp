<%@ page import =
"org.openpipeline.server.*,org.openpipeline.server.pages.*,
java.io.*, java.util.*" %>
<%@ page contentType="text/html; charset=utf-8"%><%
/**
 * This is the initialization code for each page.
 */
request.setCharacterEncoding("UTF-8");

LoginPage loginPage = new LoginPage();
loginPage.setPageContext(pageContext);

// if not logged in
if (!loginPage.isLoggedIn()) {
	String caller = request.getServletPath();
	String queryStr = request.getQueryString();
	if (queryStr != null) {
		caller = caller + "?" + queryStr;
	}
	%>

	<jsp:forward page="/login.jsp">
	<jsp:param name="caller" value="<%=caller.toString()%>" />
	</jsp:forward>
	
<% } else {
	
	// we're logged in, see if we can access the page
	String pageName = request.getServletPath();
	if (!loginPage.isAllowed(pageName)) {
		throw new Exception("Current user is not allowed to see this page:" + pageName);
	}

%>


<% } %>