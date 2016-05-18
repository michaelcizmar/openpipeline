<% 
String pageTitle = "Configure Stages"; 
ConfigureStagesPage currPage = new ConfigureStagesPage();
currPage.processPage(pageContext);
if (currPage.redirect()) {
	response.sendRedirect("set_schedule.jsp?jobname=" + currPage.getJobName());
	return;
}

session.setAttribute("currpage", currPage);

%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>

<script>
function refresh(stageId, redirect) {
	document.getElementById("newstageid").value = stageId;
	document.getElementById("redirect").value = redirect;
	document.editform.submit();
}
</script>

<form name="editform" method="post">

<table id="config_table">
<tr><td colspan="2">
Configure stages here. Stages that need to be configured have a "config" link next to the name.
<br><br>
</td></tr>

<tr>

  <td valign="top">
  <table>
    <tr><th nowrap="nowrap">Stage #</th><th>Name</th></tr>
<% 
List stageLinks = currPage.getStageLinks();
if (stageLinks.size() == 0) {
	%><tr><td colspan="2" nowrap="nowrap">No stages to configure</td></tr><%
	
} else {
	for (int i = 0; i < stageLinks.size(); i++) {
		%><tr><td><%=i%></td><td nowrap="nowrap"><%=stageLinks.get(i)%></td></tr><%
	}
}
%>
  </table>
  </td>
  
  <td valign="top" width="100%">
  <input type="hidden" name="jobname" value="<%=currPage.getJobName()%>"/>
  <input type="hidden" name="currstageid" value="<%=currPage.getCurrStageId()%>"/>
  <input type="hidden" id="newstageid" name="newstageid" value=""/>
  <input type="hidden" id="redirect" name="redirect" value="false"/>
  
  <%if (currPage.getCurrConfigPage() != null) { 
		String configPage = "plugins/" + currPage.getCurrConfigPage();
		%><jsp:include page="<%=configPage%>"></jsp:include><%
    }
  %>
  </td>
  
</tr>  

<tr><td class="cmdlink" colspan="2"><a href="javascript:refresh('', true)">Next &gt;&gt;</a></td></tr>

</table>
</form>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
