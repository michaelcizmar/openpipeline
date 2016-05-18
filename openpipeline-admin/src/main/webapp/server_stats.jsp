<% 
String pageTitle = "Server Statistics"; 
ServerStatsPage currPage = new ServerStatsPage();
currPage.processPage(pageContext);
%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>

<br>
<form method="post" action="server_properties.jsp">
<table>
<tr><td nowrap>Java version:</td><td><%=currPage.getJavaVersion()%></td></tr>
<tr><td nowrap>OpenPipeline version:</td><td><%=Server.getServer().getVersion()%></td></tr>
<tr><td nowrap>Server home directory (app.home):</td><td><%=currPage.getServerHomeDir()%></td></tr>
<tr><td nowrap>Uptime:</td><td><%=currPage.getUptime()%></td></tr>
<tr><td nowrap>Memory usage:</td><td><%=currPage.getMemUsage()%></td></tr>
<tr><td nowrap>Max memory allocated to JVM:</td><td><%=currPage.getMaxMemory()%></td></tr>
<tr><td nowrap>Number of processors:</td><td><%=currPage.getAvailProcessors()%></td></tr>
<tr><td valign="top">Class Path:</td><td><%=currPage.getClassPath()%></td></tr>

</table>
</form>
<%@ include file = "WEB-INF/includes/footer.jsp" %>
