<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Attribute Remover</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Removes attributes from an item.</td>
	</tr>

	<tr valign="top">
		<td><b>Attributes to include:</b></td>
		<td><%=currPage.textArea("attributes-to-include_SPLIT", "40", "10", false)%></td>
		<td>Specify the names of the attributes to include in the output. To be included, an
		attribute must match at least one entry in the list. Wildcards (*, ?) are allowed.
		To include all attributes, leave this box empty. Example:<br>
		<i>
		Title<br>
		TitleText<br>
		Text<br>
		MyAttr*<br>
		</i>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Attributes to exclude:</b></td>
		<td><%=currPage.textArea("attributes-to-exclude_SPLIT", "40", "10", false)%></td>
		<td>Specify the names of the attributes to exclude from the output. If an attribute
		name matches any entry in the list, it will not appear in the output. 
		Wildcards (*, ?) are allowed. Example:<br>
		<i>
		Href*<br>
		Par*<br>
		</i>
		</td>
	</tr>

</table>	
	