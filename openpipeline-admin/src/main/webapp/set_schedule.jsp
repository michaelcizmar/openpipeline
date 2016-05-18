<%@ include file = "WEB-INF/includes/initialize.jsp" %><% 
String pageTitle = "Set Schedule"; 

//must be above the header, so any errors get reported correctly
SetSchedulePage currPage = new SetSchedulePage();
currPage.processPage(pageContext);
if (currPage.redirect()) {
	response.sendRedirect("view_jobs.jsp");
	return;
}

%><%@ include file = "WEB-INF/includes/header.jsp" %>
<form name="set_schedule_form">
<input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>

<table id="config_table">

<tr><td colspan="3">Set the schedule for the job. The job will execute at the time(s) specified below.</td></tr>
<tr><td colspan="3">&nbsp;</td></tr>

<tr valign="top">
<td nowrap="nowrap"><b>Job Name:</b></td>
<td><%=currPage.getJobName()%></td>
<td>&nbsp;</td>
</tr>


<tr valign="top">
<td><b>Frequency:</b></td>
<td>
	<%=currPage.radioButton("schedtype", "ondemand", true)%>
	<label for="ondemand">On Demand Only</label>
</td>
<td>
Execute the job only on demand. Start the job using the <a href="view_jobs.jsp">View Jobs</a> page.
</td>
</tr>

<tr valign="top">
<td>&nbsp;</td>
<td>
	<%=currPage.radioButton("schedtype", "onetime", false)%>
	<label for="onetime">One Time Only</label>
</td>
<td>
Execute the job one time only, at the date/time specified below.
</td>
</tr>

<tr valign="top">
<td>&nbsp;</td>
<td>
	<%=currPage.radioButton("schedtype", "periodic", false)%>
	<label for="periodic">Every</label>
	<%=currPage.selectField("period-interval", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60")%>
	<%=currPage.selectField("period", "minutes,hours,days,weeks,months")%>

</td>
<td>
Execute the job on a periodic basis, starting at the date/time specified below.
</td>
</tr>

<tr valign="top">
<td>&nbsp;</td>
<td>
	<%=currPage.radioButton("schedtype", "cronexp", false)%>
	<label for="cronexp">Enter a custom cron expression:</label><br>
	<%=currPage.textField("cronexp", 50)%>
</td>
<td>
Schedule the job using a <a href="http://quartz.sourceforge.net/javadoc/org/quartz/CronTrigger.html" target="_blank">cron expression</a>. Cron expressions allow you to create
much more flexible schedules, like "Monday through Friday at 1:00am". 
</td>
</tr>

<tr valign="top">
<td nowrap="nowrap"><b>Starting Date:</b></td>
<td>
	<script type="text/javascript">
		$(function() {
			$("#startdate").datepicker({ dateFormat: 'yy-mm-dd' });
		});
	</script>
	<input name="startdate" type="text" id="startdate" size="20">
</td>
<td>The date the job will first execute.</td>
</tr>

<tr valign="top">
<td><b>Starting Time:</b></td>
<td>
	<%=currPage.selectField("starthour", "00,01,02,03,04,05,06,07,08,09,10,11,12")%>
	:
	<%=currPage.selectField("startminute", "00,05,10,15,20,25,30,35,40,45,50,55")%>
	<%=currPage.selectField("ampm", "am,pm")%>
</td>
<td>The time the job will first execute.</td>
</tr>


<tr><td class="cmdlink" colspan="3"><a href="javascript:document.set_schedule_form.submit()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="next">
</form>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
