<%
LinkQueuePage lqPage = new LinkQueuePage();
lqPage.processPage(currPage);
%>

<tr><td colspan="3"><p>&nbsp;</p><hr><p>&nbsp;</p></td></tr>

<tr valign="top">
<td><b>Select Link Queue:</b></td>
<td><%=lqPage.getLinkQueueDropdown()%></td>
<td>
A LinkQueue keeps track of the documents that have been processed, and provides a way
of recrawling any docs that may have been deleted. Provides some persistence between
different runs of this crawler.
</td>
</tr>

<tr valign="top">
<td><b>Database URL:</b></td>
<td><%=currPage.textField("database-url", 30)%></td>
<td>
The connection URL for the database, for linkqueues that use it. <br>
<br>
<p>
For MySQL, it should take the form:<br>
<i>jdbc:mysql://&lt;host_name&gt;:&lt;port_number&gt;</i><br>
Omit the &lt;database_name&gt; param on the URL.<br>
</p>

<p>
For Derby, it should take the form:<br>
<i>jdbc:derby:linkqueue;create=true</i><br>
where "linkqueue" is the database name. The database name
param below is ignored.
</p>

</td>
</tr>

<tr valign="top">
<td><b>Database Username:</b></td>
<td><%=currPage.textField("username")%></td>
<td>The username for the database.</td>
</tr>

<tr valign="top">
<td><b>Database Password:</b></td>
<td><%=currPage.textField("password")%></td>
<td>The password for the database.</td>
</tr>

<tr valign="top">
<td><b>Database Name:</b></td>
<td><%=currPage.textField("database")%></td>
<td>The name of the database; defaults to "linkqueue".</td>
</tr>

<tr valign="top">
<td><b>Table Name:</b></td>
<td><%=currPage.textField("table")%></td>
<td>The name of the table that stores the linkqueue data; defaults to "linkqueue".</td>
</tr>
