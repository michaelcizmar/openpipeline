<%@ page import="org.openpipeline.server.pages.*"%>
<%
	ConfigureStagesPage currPage = (ConfigureStagesPage) session
			.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Item Replicatorr</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
			<p>Replicates items.</p></td>
	</tr>

	<tr valign="top">
		<td><b>Replication Count:</b>
		</td>
		<td><%=currPage.textField("replication-count")%></td>
		<td>Specify the number of times the item must be replicated. <b></b>(Default: 30)</b></td>
	</tr>
</table>
