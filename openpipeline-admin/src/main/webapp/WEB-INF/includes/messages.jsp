<%
	if (currPage != null) {
		List msgs = currPage.getMessages();
		%>
		<table id="msg_table">
			<% for (int i = 0; i < msgs.size(); i++) {
				%><tr><td><%=((String) msgs.get(i))%></td></tr><%
				}
			%>
		</table>
		<%
	}
%>