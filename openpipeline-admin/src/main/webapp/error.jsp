<%@ page isErrorPage = "true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<%
	//Construct the base URL.

	String contextPath = request.getContextPath();
	StringBuffer url = request.getRequestURL();
	String uri = request.getRequestURI();
	String baseURL = url.substring(0,url.indexOf(uri))+ contextPath + '/';
%>
<head>
<base href="<%=baseURL%>" />
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="-1"/>
<title>Admin Application</title>
<link rel="stylesheet" type="text/css" href="screen.css"/>
</head>


<table width="100%" cellspacing="0" cellpadding="0" border="0">
<tr>
	<td bgcolor="white" colspan="2">
    <a href="index.jsp"><img src="images/logo_error.gif" alt="logo" border="0"/></a>
	</td>
</tr>

<tr>
	<td width="10%" bgcolor="white">&nbsp;</td>
	<td bgcolor="white">

	<br/>
	<br/>
	<b>We're sorry!</b> There has been an error processing your request.<br/><br/>

	<%@ page import = "org.openpipeline.util.Util" %>
	<%=Util.formatError(request, true)%>


	</td>
</tr>
</table>


