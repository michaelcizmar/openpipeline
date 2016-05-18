<%@ page import = "org.openpipeline.server.pages.*, org.openpipeline.pipeline.stage.DiskWriter" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<table>
	<tr>
		<th colspan="3">S3 File Uploader Stage</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Uploads one or more files to Amazon S3.</td>
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
		<td><b>S3 Path:</b></td>
		<td><%=currPage.textField("root-dir")%></td>
		<td>This is the directory within the bucket, where all the data will be uploaded.</td>
	</tr>
</table>