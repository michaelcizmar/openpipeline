<%@ page import = "org.openpipeline.server.pages.*,org.openpipeline.pipeline.docfilter.*,java.util.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<style>
  tr { vertical-align: top; }
</style>

<table id="config_table">
<tr>
	<th colspan="3">Configure Doc Filters</th>
</tr>
	
<tr><td colspan="3">
<p>
Connectors that fetch documents usually need to run the files through a filter to get 
the text (or XML) inside the document. A "docfilter" performs this task. For
each document, the system looks at the filename or URL to get a file extension
(.txt, .html, .doc, etc.) and selects the appropriate docfilter. Sometimes, as with a
web crawl, there may be no match for the extension, but there may be a mimetype available.
That can be used as a fallback. <a href="http://www.openpipeline.org/plugins/">Find other doc filters here.</a>
</p>
<p>
This page makes it possible to map extensions and mimetypes to docfilters. It
can be useful for changing the default system behavior.
</p>
<p>
Enter the set of extensions and/or mimetypes that each docfilter will handle. 
Separate entries with commas. Entries are not case-sensitive.
</p>
<p>
To specify a default filter, one that handles all documents not matched
by other filters, enter "*" in the extension field.
</p>
</td></tr>

<%@ include file = "stage_docfilters_subpage.jsp" %>

</table>
