<%@ include file="../WEB-INF/includes/initialize.jsp"%>
<%@ page import="org.openpipeline.pipeline.connector.webcrawler.*"%>
<%
	String pageTitle = "Web Crawler";

	GenericConnectorPage currPage = new GenericConnectorPage();
	currPage
			.processPage(pageContext,
					"org.openpipeline.pipeline.connector.webcrawler.WebCrawler");
	if (currPage.redirect()) {
		response.sendRedirect("../select_stages.jsp?jobname="
				+ currPage.getJobName());
		return;
	}

	String DEFAULT_TIME_OUT_TIME = Integer
			.toString(Fetcher.DEFAULT_TIME_OUT_TIME);
	String DEFAULT_MAX_LINK_DEPTH = Integer
			.toString(WorkerThread.DEFAULT_MAX_LINK_DEPTH);
	boolean DEFAULT_IGNORE_DYNAMIC_PAGES = WorkerThread.DEFAULT_IGNORE_DYNAMIC_PAGES;
	String DEFAULT_DOC_LOGGING_COUNT = Integer
			.toString(WorkerThread.DEFAULT_DOC_LOGGING_COUNT);
	String DEFAULT_NUMBER_OF_THREADS = Integer
			.toString(WorkerThreadPool.DEFAULT_NUMBER_OF_WORKER_THREADS);

	String DEFAULT_DATABASE = "linkqueue";
	String DEFAULT_TABLE = "linkqueue";
	String DEFAULT_USERNAME = LinkDB.DEFAULT_USERNAME;
	String DEFAULT_PASSWORD = LinkDB.DEFAULT_PASSWORD;
	String DEFAULT_DATABASE_URL = LinkDB.DEFAULT_DATABASE_URL;
%><%@ include file="../WEB-INF/includes/header.jsp"%>

<form name="config_form"><input type="hidden" name="jobname"
	value="<%=currPage.getJobName()%>" />

<table id="config_table">
	<tr>
		<td colspan="3">This connector crawls a specified series of urls.</td>
	</tr>
	<tr>
		<td colspan="3">&nbsp;</td>
	</tr>

	<tr valign="top">
		<td><b>Seed URLs:</b></td>
		<td><%=currPage.textArea("seed-urls_SPLIT", "40", "5", false)%></td>
		<td>The starting URLs for the crawl. The crawler will index these
		pages and then follow the links in them to find other pages. Put each
		URL on a separate line. Example:<br>
		<i> http://www.mysite.com<br>
		http://anothersite.com/mypage.htm</i></td>
	</tr>

	<tr valign="top">
		<td><b>Include Patterns:</b></td>
		<td><%=currPage.textArea("include-patterns_SPLIT", "40", "5",
					false)%></td>
		<td>Pages to be indexed must match at least one of the patterns
		here. Example:<br>
		<i> http://www.mysite.com*<br>
		http://anothersite.com/*.pdf</i><br>
		With these patterns the crawler would index any page on mysite.com and
		any .pdf on anothersite.com. Patterns use wildcards. Use "*" to match
		any sequence of characters and "?" to match any single character. <b>Note:</b>
		you should enter at least one pattern here, otherwise the crawler
		could try to index an unlimited number of sites.</td>
	</tr>

	<tr valign="top">
		<td><b>Exclude Patterns:</b></td>
		<td><%=currPage.textArea("exclude-patterns_SPLIT", "40", "5",
					false)%></td>
		<td>Pages that match a pattern here will be excluded from
		indexing. Example:<br>
		<i> *.pdf<br>
		*.doc</i><br>
		These patterns would exclude any PDFs or .doc files from being
		indexed.</td>
	</tr>

	<tr valign="top">
		<td><b>Timeout:</b></td>
		<td nowrap="nowrap"><%=currPage.textField("timeout", 10, 10,
					DEFAULT_TIME_OUT_TIME)%>
		seconds</td>
		<td>The maximum number of seconds the crawler should wait for a
		page before giving up.</td>
	</tr>

	<tr valign="top">
		<td><b>Crawler:</b></td>
		<td><%=currPage.checkbox("crawler", true)%></td>
		<td>If checked, the crawler will follow links. If unchecked, the
		crawler will only index the pages listed in Seed URLs above.</td>
	</tr>

	<tr valign="top">
		<td><b>Max Link Depth:</b></td>
		<td nowrap="nowrap"><%=currPage.textField("max-link-depth", 10, 10,
					DEFAULT_MAX_LINK_DEPTH)%></td>
		<td>The maximum number of links the crawler will traverse from a
		seed URL.</td>
	</tr>

	<tr valign="top">
		<td><b>Ignore Dynamic Pages:</b></td>
		<td><%=currPage.checkbox("ignore-dynamic-pages",
					DEFAULT_IGNORE_DYNAMIC_PAGES)%></td>
		<td>If checked, then pages that have parameters will be ignored.
		For example, http://mysite.com/mypage.jsp?name=value will be ignored
		if checked, and indexed like any other page if unchecked.</td>
	</tr>

	<tr valign="top">
		<td><b>Doc Logging Count:</b></td>
		<td><%=currPage.textField("doc-logging-count", 10, 10,
					DEFAULT_DOC_LOGGING_COUNT)%></td>
		<td>If empty, all documents indexed will be logged. If filled in
		with a number (1000, for example), then only the line "DocReader: docs
		added=1000" will be added on a periodic basis to the log.</td>
	</tr>

	<tr valign="top">
		<td><b>Number of threads:</b></td>
		<td><%=currPage.textField("number-of-worker-threads", 10, 10,
					DEFAULT_NUMBER_OF_THREADS)%></td>
		<td>The number of threads to use.</td>
	</tr>

	<%
		LinkQueuePage lqPage = new LinkQueuePage();
		lqPage.processPage(currPage);
	%>

	<tr>
		<td colspan="3">
		<p>&nbsp;</p>
		<hr>
		<p>&nbsp;</p>
		</td>
	</tr>

	<tr valign="top">
		<td></td>
		<td><b>Derby Link Queue Setup</b></td>
		<td>LinkQueue keeps track of the documents that have been
		processed, and provides a way of recrawling any docs that may have
		been deleted. Provides some persistence between different runs of this
		crawler.</td>
	</tr>

	<tr valign="top">
		<td><b>Database URL:</b></td>
		<td><%=currPage.textField("database-url", 10, 10,
					DEFAULT_DATABASE_URL)%></td>
		<td>The connection URL for the database, for linkqueues that use
		it. <br>

		<p>For Derby, it should take the form:<br>
		<i>jdbc:derby:linkqueue;create=true</i><br>
		where "linkqueue" is the database name. Will be replaced with the
		database name param below.</p>

		</td>
	</tr>

	<tr valign="top">
		<td><b>Database Username:</b></td>
		<td><%=currPage.textField("username", 10, 10, DEFAULT_USERNAME)%></td>
		<td>The username for the database.</td>
	</tr>

	<tr valign="top">
		<td><b>Database Password:</b></td>
		<td><%=currPage.textField("password", 10, 10, DEFAULT_PASSWORD)%></td>
		<td>The password for the database.</td>
	</tr>

	<tr valign="top">
		<td><b>Database Name:</b></td>
		<td><%=currPage.textField("database", 10, 10, DEFAULT_DATABASE)%></td>
		<td>The name of the database".</td>
	</tr>

	<tr valign="top">
		<td><b>Table Name:</b></td>
		<td><%=currPage.textField("table", 10, 10, DEFAULT_TABLE)%></td>
		<td>The name of the table that stores the linkqueue data.</td>
	</tr>

	<tr>
		<td class="cmdlink" colspan="3"><a
			href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td>
	</tr>

</table>
<input type="hidden" name="next" value="submit"></form>

<%@ include file="../WEB-INF/includes/footer.jsp"%>

