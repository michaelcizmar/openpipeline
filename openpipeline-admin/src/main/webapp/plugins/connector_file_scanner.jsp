<%@ page import = "org.openpipeline.pipeline.connector.*" %>
<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "File Scanner"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.FileScanner");
if (currPage.redirect()) {
	response.sendRedirect("../select_stages.jsp?jobname=" + currPage.getJobName());
	return;
}

%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">

<tr><td colspan="3">This connector scans files in a file system.</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

<tr valign="top">
<td><b>Files/Directories:</b></td><td><%=currPage.textArea("fileroots_SPLIT", "40", "5", false)%></td>
<td>The files and directories to index. Put each file or directory on a 
separate line. Example:<br>
<i>C:\mydirectory\myfile.doc<br>
/mydir/mysubdir</i></td>
</tr>

<tr valign="top">
<td><b>Include Patterns:</b></td><td><%=currPage.textArea("include-patterns_SPLIT", "40", "5", false)%></td>
<td>Files to be indexed must match at least one of the patterns here. Example:<br>
<i>*.pdf<br>
*/mydir/*.doc</i><br>
Patterns use wildcards. Use "*" to match any 
sequence of characters and "?" to match any single character. Leave this box
empty to index all files specified above. <b>Note:</b> Be sure to use the correct file
separator for your platform, i.e. / for Unix, \ for Windows, etc.
</td>
</tr>

<tr valign="top">
<td><b>Exclude Patterns:</b></td><td><%=currPage.textArea("exclude-patterns_SPLIT", "40", "5", false)%></td>
<td>Files that match one of the patterns here will be excluded from indexing. Example:<br>
<i>*.pdf<br>
*/mydir/*</i><br>
These patterns would exclude all PDFs and any files in the "mydir" directory.
</td>
</tr>

<tr valign="top">
<td><b>Subdirs:</b></td><td><%=currPage.checkbox("subdirs", true)%></td>
<td>If checked, the crawler will look for files in subdirectories of any
directories specified in Files/Directories above.</td>
</tr>

<tr valign="top">
<td><b>Compressed Files:</b></td><td><%=currPage.checkbox("compressed-files", true)%></td>
<td>If checked, the crawler will look for files inside files
with a .zip extension.</td>
</tr>

<tr valign="top">
<td><b>Max file size (mb):</b></td><td><%=currPage.textField("max-file-size", 10, 10, FileScanner.MAX_FILE_SIZE_DEFAULT + "")%></td>
<td>Enter the size of the largest file to process, in megabytes. Defaults to 100mb.</td>
</tr>

<tr valign="top">
<td><b>Doc Logging Count:</b></td><td><%=currPage.textField("doc-logging-count")%></td>
<td>If empty, all documents indexed will be logged. If filled in with a number
(1000, for example), then only the line "DocReader: docs added=1000" will be added
on a periodic basis to the log.
</td>
</tr>

<%@ include file = "../WEB-INF/includes/linkqueue.jsp" %>

<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>

