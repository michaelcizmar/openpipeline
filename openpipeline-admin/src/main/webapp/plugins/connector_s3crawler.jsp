<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "S3 File Crawler"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.S3Crawler");
if (currPage.redirect()) {
	response.sendRedirect("../select_stages.jsp?jobname=" + currPage.getJobName());
	return;
}

%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table>
	<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

	<tr>
		<th colspan="3">S3 File Scanner</th>
	</tr>

	<tr valign="top">
		<td colspan="3">This connector scans for files in a Amazon S3 bucket.</td>
	</tr>

	<tr valign="top">
		<td><b>Access Key Id:</b></td>
		<td><%=currPage.textField("access-key")%></td>
		<td>Amazon access key id.</td>
	</tr>
	
	<tr valign="top">
		<td><b>Secret Key:</b></td>
		<td><%=currPage.textField("secret-key")%></td>
		<td>Amazon secret key. Each "Access Key Id" is paired with a "Secret Key". The connector will use this key pair to fetch files from S3.</td>
	</tr>
	
	<tr valign="top">
		<td><b>Bucket Name:</b></td>
		<td><%=currPage.textField("bucket")%></td>
		<td>Amazon S3 bucket name.</td>
	</tr>
		
	<tr valign="top">
		<td><b>Files/Directories on S3:</b></td><td><%=currPage.textArea("fileroots_SPLIT", "40", "5", false)%></td>
		<td>The files and directories to index. Put each file or directory on a 
		separate line. Example:<br>
		<i>C:\mydirectory\myfile.doc<br>
		/mydir/mysubdir</i></td>
	</tr>
	
	<tr valign="top">
		<td><b>Include Patterns:</b></td><td><%=currPage.textArea("include-patterns_SPLIT", "40", "5", false)%></td>
		<td>Files/Directories to be indexed must match at least one of the patterns here. Example:<br>
		<i>
			files/*<br>
			images/*<br>
			2012/images/*<br>
		</i><br>
			Patterns use wildcards. Use "*" to match any 
			sequence of characters and "?" to match any single character. Leave this box
			empty to index all folders.
		</td>
	</tr>

	<tr valign="top">
		<td><b>Exclude Patterns:</b></td><td><%=currPage.textArea("exclude-patterns_SPLIT", "40", "5", false)%></td>
		<td>Files/Directories that match one of the patterns here will be excluded from indexing. Example:<br>
		<i>
			files/*<br>
			images/*<br>
			2012/images/*<br>
		</i>
		</td>
	</tr>
	
	<%@ include file = "../WEB-INF/includes/linkqueue.jsp" %>
	
	<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>
</table>

<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>