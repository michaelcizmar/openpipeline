<% 
String pageTitle = "Users"; 
UsersPage currPage = new UsersPage();
currPage.processPage(pageContext);
%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>

	<p>
	Add and delete users of this admin app here. Note that this page is not generally used
	for managing logins for an end-user app.
	</p>

	<% 
	List <String> usernames = currPage.getUserNames();
	boolean hasDeletes = usernames.size() > 1;
	String deleteHeader = hasDeletes ? "<th>&nbsp;</th>" : "";
	
	%>
	<table id="config_table" class="rowhover" style="width:25%">
	<tr><th>Username</th><%=deleteHeader%></tr>
	
	<% for (int i = 0; i < usernames.size(); i++) {
		String username = usernames.get(i);
		String editLink = "<a href=\"user.jsp?user=" + username + "\">" + username + "</a>";
		String deleteLink = "&nbsp;";
		if (!"admin".equals(username)) {
			deleteLink = "<a href=\"users.jsp?delete=" + username + "\">delete</a>";
		}
		%>
		<tr>
		<td><%=editLink%></td>
		<td><%=deleteLink%></td>
		</tr>
	<% } %>
	
	</table>
	<table id="config_table" >
		<tr><td><br><a href="user.jsp?user=_newuser">add new...</a></td></tr>
	</table>
	
	

<%@ include file = "WEB-INF/includes/footer.jsp" %>
