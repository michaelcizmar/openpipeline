<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ page import = "org.openpipeline.pipeline.connector.*" %>
<%
String pageTitle = "Add Job"; 

// must be above the header, so any errors get reported correctly
AddJobPage currPage = new AddJobPage();
currPage.processPage(pageContext);
if (currPage.redirect()) {
	response.sendRedirect(currPage.redirectPage());
	return;
}

%>
<%@ include file = "WEB-INF/includes/header.jsp" %>
    
    <script>
	    var nextConnectorNum = <%=currPage.getNextConnectorNum()%>
    	
    	function setName(name) {
    		var nameControl = document.add_job_form.jobname;
   			nameControl.value = name + nextConnectorNum;
    	}
    </script>
    <p>
    A job is any OpenPipeline process that can run on a schedule. Jobs are usually crawlers or
    other types of connectors to external data sources.
    </p>
    <p> 
    <a href="http://www.openpipeline.org/plugins/">Find other connectors here.</a>
    </p>

    <form name="add_job_form" method="get" action="add_job.jsp">
    
    <table id="config_table" style="width:70%">
    <%

    Iterator connectors = currPage.getConnectors();
    while (connectors.hasNext()) {
    	Connector con = (Connector)connectors.next();
    	String className = con.getClass().getName(); // unique name for connector
    	
    	%>
    	<tr class="alt">
    	<td nowrap>
	    <input type="radio" 
	           name="conpage" 
	           id="<%=className%>" 
	           value="<%=con.getPageName()%>" 
	           onChange="setName('<%=con.getShortName()%>')">
		<label for="<%=className%>"><%=con.getDisplayName()%></label>
        <br>
	    </td>
	    <td>
	    <%=con.getDescription()%>
	    </td>
	    </tr>
		<% 
	}
    %>
    
    <tr><td colspan="3">&nbsp;</td></tr>

    <tr><td colspan="3">Enter name of connector: <input type="text" name="jobname" size="50"></td></tr>

    <tr><td class="cmdlink" colspan="3"><a href="javascript:document.add_job_form.submit()">Next &gt;&gt;</a></td></tr>
    
    </table>
    
    <input type="hidden" name="next" value="submit">
    </form>
    

<%@ include file = "WEB-INF/includes/footer.jsp" %>