<%@ page import="org.openpipeline.server.pages.*"%>
<%
	ConfigureStagesPage currPage = (ConfigureStagesPage) session
			.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Date Format Converter</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
			<p>Converts one date format to another.</p>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Date Attribute:</b></td>
		<td><%=currPage.textField("date-attribute")%></td>
		<td>Specify the name of the date attribute</td>
	</tr>

	<tr valign="top">
		<td><b>New Date Attribute Name:</b></td>
		<td><%=currPage.textField("new-date-attribute")%></td>
		<td>Specify the new name of the new attribute for the converted
			date.</td>
	</tr>

	<tr valign="top">
		<td><b>Date input format:</b></td>
		<td><%=currPage.textField("date-input-format")%></td>
		<td>Specify the input date format.</td>
	</tr>

	<tr valign="top">
		<td><b>Date output format:</b></td>
		<td><%=currPage.textField("date-output-format",50, 50,"yyyy/MM/dd hh:mm:ss.SSS")%></td>
		<td>Specify the output date format.</td>
	</tr>


</table>
