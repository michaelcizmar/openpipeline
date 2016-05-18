<%@ page import = "org.openpipeline.server.pages.*, java.util.*"%> 
<%

// use caller to redirect the user back to the calling page
String caller = request.getParameter("caller");
if (caller == null || caller.equals(""))
	caller = "index.jsp";

LoginPage loginPage = new LoginPage();
LoginPage currPage = loginPage; // needed elsewhere
loginPage.setPageContext(pageContext);
if (loginPage.login()) {
	%><jsp:forward page="<%=caller%>"/><%
}

String pageTitle = "Login"; 
%>
<%@ include file = "WEB-INF/includes/header.jsp" %>

<br>
<br>
<form name="loginform" action="login.jsp" method="post">
<table>
	<tr>
		<td>Enter user name:</td>
		<td><input type="text" name="opuser" size="20"></td>
	</tr>
	<tr>
		<td>Enter password:</td>
		<td><input type="password" name="oppassword" size="20"></td>
		<td>&nbsp;&nbsp;&nbsp;&nbsp;
		<input type="submit" value="Login" name="login"></td>
	</tr>

	<tr>
		<td>Remember this password using a cookie&nbsp;&nbsp;</td>
		<td colspan="2"><input type="checkbox" name="remember" checked></td>
	</tr>

</table>
<br>
<br>
<br>
(For first-time users, the user name is "admin" and the password is "admin". 
Please change this password by visiting the Users page.)
<input type="hidden" name="caller" value="<%=caller%>">
</form>

<%@ include file = "WEB-INF/includes/footer.jsp" %>

