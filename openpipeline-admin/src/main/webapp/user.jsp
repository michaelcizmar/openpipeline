<% 
String pageTitle = "User";
UserPage currPage = new UserPage();
currPage.processPage(pageContext);
if (currPage.redirect()) {
	response.sendRedirect("users.jsp");
	return;
}

%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>

Edit the user information below.
<p>&nbsp;</p>
<form name="editform" method="post">
<table id="config_table">
<tr>
	<td><b>User Name</b></td>
	<td><%=currPage.getUserName()%></td>
	<td>The user's login name.</td>
</tr>
<tr>
	<td><b>Password</b></td>
	<td><%=currPage.getPassword()%></td>
	<td>The user's password</td>
</tr>
<tr valign="top">
	<td><b>Pages</b></td>
	<td><%=currPage.getPages()%></td>
	<td><p>The pages the user is allowed to see.</p>
	    <p>If you select "all", then all pages will be visible to the user. All pages
	    will always be visible to the admin.</p></td>
</tr>

<tr><td colspan="3"><a href="javascript:document.editform.submit()">save</a></td></tr>
</table>
<input type="hidden" name="action" value="save"> 
</form>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
