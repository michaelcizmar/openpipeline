<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<table>
	<tr>
		<th colspan="3">Item Sender</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Sends an item to one or more remote machines for further processing.</td>
	</tr>

	<tr valign="top">
		<td><b>Remote Servers:</b></td>
		<td><%=currPage.textArea("server_addresses_SPLIT", "40", "5", false)%></td>
		<td>To send an item to a remote server, you need to add the connector name listening at the remote server and web service URL. You can add
		multiple remote destinations by specifying each destination on a new line.<br>
		For example,<br>
		ItemReceiver0, http://localhost:8080/webservices/ItemReceiver<br>
		ItemReceiver3, http://localhost:9000/webservices/ItemReceiver<br>
		</td>
	</tr>
	
		<tr valign="top">
		<td><b>Selection Method:</b></td>
		<td><%=currPage.selectField("selection-method", "Round Robin,Random", "round-robin,random")%></td>
		<td>A remote destination will be selected for sending an item based on the selected approach. (Default: <i>Round Robin</i>)<br>
		</td>
	</tr>
</table>