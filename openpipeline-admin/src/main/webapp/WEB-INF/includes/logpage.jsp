<script>
	function scrollToBottom() {
		var lines = document.getElementById("loglines");
		lines.scrollTop = lines.scrollHeight;
	}
</script>


<form name="logform" style="margin-bottom: 0">
<table width="100%" height="50%" cellpadding="5" cellspacing="0" >

<tr height="1%">
	<td nowrap><%=logLabel%>
	<br><br>
	Click the <a href="view_jobs.jsp">View Jobs</a> page to see the status of the current jobs
	<br><br>
	</td>
</tr>

<tr height="1%">
	<td>Showing the last <%=logPage.getFetchSizeSelect()%> kb of logs of 
	type: <%=logPage.getLogTypeSelect()%> 
	filename: <%=logPage.getLogFilenamesSelect()%>
	&nbsp;&nbsp;&nbsp;&nbsp;
	<a href="javascript:document.logform.submit()">refresh</a>
</td> 
</tr>

<tr><td>&nbsp;</td></tr>

<tr>
<td>
	<body onload="scrollToBottom()">
	<div id="loglines">
	<pre><%=logPage.getLogLines()%></pre>
	</div>
	</body>
</td>
</tr>

</table>
</form>


