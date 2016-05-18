<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "CSV Crawler"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.csvcrawler.CSVCrawler");
if (currPage.redirect()) {
	response.sendRedirect("../select_stages.jsp?jobname=" + currPage.getJobName());
	return;
}


%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">

<tr><td colspan="3">
<p>
This is a simple connector that ingests a CSV file and produces one item per row.
</p>
<p>
It assumes that your file is <a href="http://en.wikipedia.org/wiki/Comma-separated_values">standard comma-separated format</a>.
</p>

</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr valign=top>
<td><b>Filename:</b></td><td><%=currPage.textField("filename", 40)%></td>
<td>The full path to the csv file. Does not support URLs.</td>
</tr>

<tr valign=top>
<td><b>Encoding:</b></td><td><%=currPage.textField("encoding", 10, 10, "UTF-8")%></td>
<td>The character encoding of the file. For example, UTF-8, ISO 8859-1.</td>
</tr>

<tr valign=top>
<td><b>Item ID column:</b></td>
<td><%=currPage.textField("itemid-col")%></td>
<td>This is the column to use to populate the itemId in
each item.</td>
</tr>


<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>

