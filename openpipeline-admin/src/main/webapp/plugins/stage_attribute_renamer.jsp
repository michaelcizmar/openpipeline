<%@ page import="org.openpipeline.server.pages.*"%>
<%
	ConfigureStagesPage currPage = (ConfigureStagesPage) session
			.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Attribute Renamer</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
			<p>Renames an attribute.</p></td>
	</tr>

	<tr valign="top">
		<td><b>Attribute to Rename:</b>
		</td>
		<td><%=currPage.textField("attribute-to-rename")%></td>
		<td>Specify the name of the attribute to rename</td>
	</tr>

	<tr valign="top">
		<td><b>New Attribute Name:</b>
		</td>
		<td><%=currPage.textField("new-attribute-name")%></td>
		<td>Specify the new name of the attribute.</td>
	</tr>

</table>
