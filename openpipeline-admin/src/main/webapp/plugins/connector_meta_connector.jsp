<%@ page import = "org.openpipeline.pipeline.connector.*" %>
<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "Meta Connector"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.MetaConnector");
if (currPage.redirect()) {
	response.sendRedirect("../set_schedule.jsp?jobname=" + currPage.getJobName());
	return;
}

%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">

<tr><td colspan="3">This connector runs other jobs in the system. It doesn't process
any data directly on its own; it only fires the selected jobs below in 
sequence. When one job completes, the next is started.
</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

start here

<tr valign="top">
<td><b>Files/Directories:</b></td><td><%=currPage.textArea("fileroots_SPLIT", "40", "5", false)%></td>
<td>The files and directories to index. Put each file or directory on a 
separate line. Example:<br>
<i>C:\mydirectory\myfile.doc<br>
/mydir/mysubdir</i></td>
</tr>




<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>

