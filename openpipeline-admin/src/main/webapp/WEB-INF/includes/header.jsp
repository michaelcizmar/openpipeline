<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<%
	//Construct the base URL.
	String contextPath = request.getContextPath();
	StringBuffer url = request.getRequestURL();
	String uri = request.getRequestURI();
	String baseURL = url.substring(0, url.indexOf(uri)) + contextPath + '/';
%>

<head>
<base href="<%=baseURL%>" />
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="-1"/>
<title>Admin Application</title>

<%@ include file = "js_css.jsp" %>
	
</head>
<body id="index">

<div id="header">
<div class="cap">&nbsp;</div>
<a href="index.jsp"><img src="images/logo.gif" alt="logo" border="0"/></a>

<% if (pageTitle != null) { %>
	<div id="title">
		<h2><%=pageTitle%></h2>
	</div>
<% } %>

</div>
<table id="wrapper" cellpadding="0" cellspacing="0" width="100%">
	<tbody>
		<tr>
			<td id="sidebar">

			<%@ include file = "menu.jsp" %>

			</td>
			<td id="container">&nbsp;</td>
			<td id="content">
			<%@ include file = "messages.jsp" %>
