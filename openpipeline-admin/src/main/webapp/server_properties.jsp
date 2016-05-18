<% 
String pageTitle = "Server Properties"; 
ServerPropertiesPage currPage = new ServerPropertiesPage();
String [] props = {"debug"};
currPage.setPropertiesToSave(props);
currPage.processPage(pageContext);
%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>
<%

Server server = Server.getServer();
String debugChecked = (server.getDebug() ? " checked " : "");

%>
<br>
<form method="post" action="server_properties.jsp">
<table>

<tr><td nowrap>Debug:</td><td><input type="checkbox" name="debug" <%=debugChecked%> value="Y"></td></tr>

<tr><td nowrap>&nbsp;</td><td><input type="submit" name="update" value="Update"></td></tr>
<tr><td colspan="2">&nbsp;</td></tr>
<tr><td nowrap colspan="2">If any of the properties above change, the server should be restarted. Click <a href="stop.jsp">Stop the Server</a> and then restart.</td></tr>
</table>
</form>
<%@ include file = "WEB-INF/includes/footer.jsp" %>
