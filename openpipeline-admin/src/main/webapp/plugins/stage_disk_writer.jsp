<%@ page import = "org.openpipeline.server.pages.*, org.openpipeline.pipeline.stage.DiskWriter" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<table>
	<tr>
		<th colspan="3">Disk Writer Stage</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Writes items to disk. Each item is converted
		to XML, and stored as a file in the output directory. The filename is the
		itemId plus ".xml". If the itemId contains characters not allowed in a filename, they are
		replaced with underscores. This stage is useful for debugging.</td>
	</tr>

	<tr valign="top">
		<td><b>Output directory:</b></td>
		<td><%=currPage.textField("output-dir", 50, 1024, DiskWriter.DEFAULT_OUTPUT_DIR)%></td>
		<td>The directory where the files will be written.</td>
	</tr>

	<tr valign="top">
		<td><b>Include annotations:</b></td>
		<td><%=currPage.checkbox("include-annotations", false)%></td>
		<td>If any tokens or annotations have been added to the item by an earlier stage in the 
		pipeline, check this box to include them in the output files.</td>
	</tr>
	
		<tr valign="top">
		<td><b>Include binary:</b></td>
		<td><%=currPage.checkbox("include-binary", true)%></td>
		<td>If the item carries any binary content, write it to disk as a file alongside the .xml.</td>
	</tr>
	
</table>