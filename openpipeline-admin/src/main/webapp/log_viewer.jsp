<% 
String pageTitle = "Log Viewer"; 
AdminPage currPage = null;
%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>
<% 
	StatusLogPage logPage = new StatusLogPage();
	logPage.setDefaultLogType("server");
	logPage.processRequest(request);
	String logLabel = "";
%>
<%@ include file="WEB-INF/includes/logpage.jsp" %>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
