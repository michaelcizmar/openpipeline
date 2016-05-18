<%@ page import = "org.openpipeline.scheduler.*"%> 
<%
 	String pageTitle = "View Jobs"; 

 ViewJobsPage currPage = new ViewJobsPage();
 currPage.processPage(pageContext);

 List jobs = currPage.getJobs();
 Iterator it = jobs.iterator();
 %>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>
<script>
function confirmRemove(jobname, link) {
	if (confirm("Remove " + jobname + "? \n\n(This will delete the config file from /jobs)")) {
		window.location.replace(link);
	}
}
</script>

This table displays all of the jobs in the system and their current status.  
<a href="view_jobs.jsp">Refresh page.</a>
<br/>
<br/>

<table id="config_table" class="rowhover">
<tr>
	<th>Job</th>
	<th>Schedule</th>
	<th>Next</th>
	<th>Last Message</th>
	<th>Warnings</th>
	<th>Errors</th>
	<th>Action</th>
	<th>Log</th>
	<th class="center">Remove</th>
</tr>

<% while (it.hasNext()) {
	JobInfo job = (JobInfo)it.next();
	
	String jobLink = "&jobname=" + job.getJobName();
	String startStopLink;
	String startStopText;
	String removeLink;
	String lastMessage = job.getLastMessage();
	String warningCount = (job.getWarningCount() == 0 ? "" : job.getWarningCount() + "");
	String errorCount = (job.getErrorCount() == 0 ? "" : job.getErrorCount() + "");
	
	if (job.getIsRunning()) {
		startStopLink = "view_jobs.jsp?action=stop" + jobLink;
		startStopText = "stop";
		removeLink = "javascript:alert('To remove a running job, stop it first')";
		lastMessage = "<img src='images/running.gif'/>" + lastMessage;

	} else {
		
		startStopLink = "view_jobs.jsp?action=start" + jobLink;
		startStopText = "start";
		removeLink = "javascript:confirmRemove('" + job.getJobName() + "', 'view_jobs.jsp?action=remove" + jobLink + "')";
	}
	
	String setScheduleLink = "set_schedule.jsp?jobname=" + job.getJobName();
	String startJobLink = "";
	String endJobLink = "";
	if (job.getPageName() != null) {
		startJobLink = "<a href=\"plugins/" + job.getPageName() + "?jobname=" + job.getJobName() + "\">";
		endJobLink = "</a>";
	}
	
	%>
	<tr>
	  <td><%=startJobLink%><%=job.getJobName()%><%=endJobLink%></td>
	  <td><a href="<%=setScheduleLink%>"><%=job.getSchedule()%></a></td>
	  <td><%=job.getNextFireTime()%></td>
	  <td style='vertical-align: top'><%=lastMessage%></td>
	  <td><%=warningCount%></td>
	  <td><%=errorCount%></td>
	  <td><a href="<%=startStopLink%>"><%=startStopText%></a></td>
	  <td><a href="<%=job.getLogLink()%>">log</a></td>
	  <td class="center"><a href="<%=removeLink%>">x</a></td>
	</tr>
<% } %>
</table>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
