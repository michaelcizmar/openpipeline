<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<table>
	<tr>
		<th colspan="3">Sentence Extractor Stage</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Extracts sentences from an item and adds them to the <i>sentence</i> annotation list.</td>
	</tr>

	<tr valign="top">
		<td><b>Locale Language:</b></td>
		<td><%=currPage.textField("language", 10)%></td>
		<td>Specify the locale language. Examples:<br><i>en</i>&nbsp;(=English)<br><i>gr</i>&nbsp;(=German)<br><i>es</i>&nbsp;(=Spanish)<br>The sentence extractor will use the default value, if you do not wish to specify this property.</td>
	</tr>
</table>