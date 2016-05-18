<%@ include file = "../WEB-INF/includes/initialize.jsp" %>
<% 
String pageTitle = "Item Receiver"; 

GenericConnectorPage currPage = new GenericConnectorPage();
currPage.processPage(pageContext, "org.openpipeline.pipeline.connector.ItemReceiverConnector");
if (currPage.redirect()) {
	response.sendRedirect("../select_stages.jsp?jobname=" + currPage.getJobName());
	return;
}


%><%@ include file = "../WEB-INF/includes/header.jsp" %>

<form name="config_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">
<tr><td colspan="3">This connector receives one or more items from remote machines.</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr><td colspan="3">There are (currently) no options to configure for this connector.</td></tr>

<tr><td class="cmdlink" colspan="3"><a href="javascript:document.config_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "../WEB-INF/includes/footer.jsp" %>

