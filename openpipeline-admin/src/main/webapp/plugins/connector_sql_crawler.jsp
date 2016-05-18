<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "SQL Crawler"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.SQLDatabaseCrawler");
if (currPage.redirect()) {
	response.sendRedirect("../select_stages.jsp?jobname=" + currPage.getJobName());
	return;
}


%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">

<tr><td colspan="3">
<p>
This connector scans a SQL database.
</p>
<p>
When processing more than a small number of rows, it's necessary to break the
the SQL queries into chunks. When you select a large number of rows from
a SQL server without chunking them it can cause a variety of problems, including
delays in processing on the server, timeouts, and out-of-memory conditions.
</p>
<p>
The options below provide a way of fetching data as a series of smaller queries.
To make this work, your main query ("Index SQL" below) must contain a primary key,
that is, a column which has a unique value for all records. You must enter the
name of that column in the "Primary Key" field below, and then sort on it
in your Index SQL query ("ORDER BY primaryKey"). You must also include a "WHERE" 
clause that contains "primaryKey &gt; ?".
</p>
<p>
The system will call your Index SQL repeatedly. Each time it will replace the "?"
in the "WHERE" clause with the highest primary key it saw in the last fetch. In this
way it will page through the result set, never holding more that a limited number of
rows in memory at any one time.
</p>
<p>
The system will limit the size of each result set in memory to the number specified in the
"Fetch size" section below. This will reduce out-of-memory errors. To limit the number
of rows fetched, though, you must also include a clause in the Index SQL to limit
the number of rows the database server will return. In MySQL or PostgresSQL, use
the "LIMIT" keyword; in MS SQL Server use "TOP"; in Oracle use "ROWNUM", in
Sybase use "SET rowcount", and in Firebird use "FIRST".
</p>

</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr valign=top>
<td><b>JDBC Driver:</b></td><td><%=currPage.textField("jdbc-driver", 40)%></td>
<td>The class name of the JDBC driver to use.
Examples:<br/><br/>
<p><i>
com.mysql.jdbc.Driver<br>
com.microsoft.sqlserver.jdbc.SQLServerDriver<br></i>
</p>
The .jar file for the driver must be on the classpath. Put it in the /lib directory.
</td>
</tr>

<tr valign=top>
<td><b>JDBC URL:</b></td><td><%=currPage.textField("jdbc-url", 40)%></td>
<td>The URL connection string required by the driver.
Example:<br/><br/>
<p><i>
jdbc:mysql://localhost/mydatabase<br>
jdbc:sqlserver://my_server_name:1433;database=my_database<br>
</i></p>
</td>
</tr>

<tr valign=top>
<td><b>JDBC User:</b></td><td><%=currPage.textField("jdbc-user")%></td>
<td>The user name for logging in to the database.</td>
</tr>

<tr valign=top>
<td><b>JDBC Password:</b></td><td><%=currPage.passwordField("jdbc-password")%></td>
<td>The password for logging in to the database.</td>
</tr>

<tr valign=top>
<td><b>Before SQL:</b></td>
<td><%=currPage.textArea("before-sql", "40", "5", false)%></td>
<td>This is any SQL statement to execute before extracting data from the database. See the
Developer's Guide for strategies on how to use this parameter.</td>
</tr>

<tr valign=top>
<td><b>Index SQL:</b></td>
<td><%=currPage.textArea("index-sql", "40", "5", false)%></td>
<td>Any legal select statement that returns the data to be indexed. 
Must return a single result set. Must be in the following form:<br/><br/>
<p><i>
SELECT [list of columns]<br/>
FROM [table or related tables with a join clause]<br/>
WHERE [primary key column] &gt; ?<br/>
ORDER BY [primary key column]<br/>
LIMIT 1000; // this will depend on the server<br/>
</i></p>
See the top of this page for an explanation.
</td>
</tr>

<tr valign=top>
<td><b>After SQL:</b></td>
<td><%=currPage.textArea("after-sql", "40", "5", false)%></td>
<td>Any sql statement to execute after indexing is complete.</td>
</tr>

<tr valign=top>
<td><b>Index SQL Primary Key:</b></td>
<td><%=currPage.textField("primary-key-col")%></td>
<td>The primary key of the table (or related tables) that appear
in the Index SQL field below. 
</td>
</tr>

<tr valign=top>
<td><b>Index SQL Item ID:</b></td>
<td><%=currPage.textField("itemid-col")%></td>
<td>This is the column to use to populate the itemId in
each item. It will almost always be the same as the 
primary key column above.
</td>
</tr>

<tr valign=top>
<td><b>Fetch size:</b></td>
<td><%=currPage.selectField("fetch-size", "10,100,1000,10000")%></td>
<td>The number of rows to select in one fetch from the server.
See the top of this page for an explanation.
</td>
</tr>

<%@ include file = "../WEB-INF/includes/linkqueue.jsp" %>

<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>

