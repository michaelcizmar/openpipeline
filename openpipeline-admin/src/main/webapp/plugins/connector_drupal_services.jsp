<%@ include file="../WEB-INF/includes/initialize.jsp"%>
<%@ page
	import="org.openpipeline.pipeline.connector.webservice.DrupalServicesConnector.*"%>
<%
	String pageTitle = "Drupal Services Connector";

	GenericConnectorPage currPage = new GenericConnectorPage();
	currPage
			.processPage(pageContext,
					"org.openpipeline.pipeline.connector.webservice.DrupalServicesConnector");
	if (currPage.redirect()) {
		response.sendRedirect("../select_stages.jsp?jobname="
				+ currPage.getJobName());
		return;
	}

%><%@ include file="../WEB-INF/includes/header.jsp"%>

<form name="config_form">
	<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>" />

	<table id="config_table">
		<tr>
			<td colspan="3">This connector crawls a Drupal server using Drupal services, JSON, and REST.</td>
		</tr>
		<tr>
			<td colspan="3">&nbsp;</td>
		</tr>

		<tr valign="top">
			<td><b>Webservice URL:</b></td>
			<td><%=currPage.textField("webservice-url", 40)%></td>
			<td>The webservice URL for the crawl. Example:<br> <i>
					http://www.mysite.com/services/json/</i><br></td>
		</tr>

		<tr valign="top">
			<td><b>Drupal Services method:</b></td>
			<td><%=currPage.textField("method", 40)%></td>
			<td>The webservice method for retrieving data.</td>
		</tr>

		<tr>
			<td class="cmdlink" colspan="3"><a
				href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td>
		</tr>

	</table>
	<input type="hidden" name="next" value="submit">
</form>

<%@ include file="../WEB-INF/includes/footer.jsp"%>

