<%@ page import="org.openpipeline.server.pages.*"%>
<%
	ConfigureStagesPage currPage = (ConfigureStagesPage) session
			.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Attribute Value Converter</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
			<p>Modifies the value of an attribute.</p>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Attribute to Modify:</b></td>
		<td><%=currPage.textArea("attribute-to-modify_SPLIT", "40", "5", false)%></td>
		<td>Specify the name of the attribute, value to be modified and the new value. One attribute per line,
		 each line contains attribute, matching pattern, replacement value separated by ":".
	
		<br>
		 E.g., to replace "MSWord", "application/MSWord" in the attribute "doctype" with "Word", use
		 	<br>
				doctype:*word*:Word
			</br>
		</br>
		</td>
	</tr>

</table>
