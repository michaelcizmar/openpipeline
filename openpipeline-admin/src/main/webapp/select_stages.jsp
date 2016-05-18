<% 
String pageTitle = "Select Stages"; 
SelectStagesPage currPage = new SelectStagesPage();
currPage.processPage(pageContext);
if (currPage.redirect()) {
	response.sendRedirect("configure_stages.jsp?jobname=" + currPage.getJobName());
	return;
}

%>
<%@ include file = "WEB-INF/includes/initialize.jsp" %>
<%@ include file = "WEB-INF/includes/header.jsp" %>
<script language="javascript" type="text/javascript" src="js/admin.js"></script>

<script type="text/javascript" >
$(document).ready(function() {
	
	$("#standard_pipeline_dlg").dialog({
		autoOpen: false, 
		buttons: { 
			"OK": function() {
				// populate the In Use box with the options
				var list = $("#select_pipeline_list").get(0);
				var desc = list.options[list.selectedIndex].title;
				$(this).dialog("close"); 
				$("#selected_stages").html(desc);
			},
			"Cancel": function() { $(this).dialog("close"); }
		},
		modal: true,
	});

	$("#select_pipeline_link").click(function() {
		$("#standard_pipeline_dlg").dialog("open"); 
	});


	$("#available_stages").click(function() {
		// show the description of the selected stage
		var desc = this.options[this.selectedIndex].title;
		$("#stage_desc").html(desc);
	});

	$("#select_pipeline_list").dblclick(function() {
		// populate the In Use box with the options
		var desc = this.options[this.selectedIndex].title;
		$("#standard_pipeline_dlg").dialog("close"); 
		$("#selected_stages").html(desc);
	});
});

function submitSelectStages() {
	gatherOptions('selected_stages', "selected_stages_param", "|");
	document.editform.submit();
}
</script>

<form name="editform" method="post">
<input type="hidden" name="action" value="save">
<input type="hidden" name="selected_stages_param" id="selected_stages_param" value="">

<div id="standard_pipeline_dlg" title="Standard Pipelines" align="center">
<p>Select a preconfigured pipeline below:</p>
<p>&nbsp;</p>
<p>
<select id='select_pipeline_list' multiple="multiple">
<%=currPage.getStandardPipelineOptions()%>
</select>
</p>
</div>

<table id="config_table" style="width:30%">

<tr><td colspan="4">

<p>
Configure the stages that Items will go through in this job.
Available stages are on the left, and the stages that will actually be used 
are on the right. A stage can be added more than once to a pipeline.
</p>
<p>
Click "Select a standard pipeline" below to fill the "In Use" box with
a preconfigured pipeline. These pipelines are
defined in &lt;app home dir&gt;/config/standard-pipelines.xml
</p>
<p>
<a href="http://www.openpipeline.org/plugins/">Find other stages here.</a>
</p>    

</td></tr>

 
<tr>
<td><b>Available</b></td>
<td></td>
<td><b style="float:left">In Use</b><a id="select_pipeline_link" style="float:right" href="javascript:void(0)">Select a standard pipeline...</a></td>
<td>&nbsp;</td>
</tr>

<tr valign="top">
<td>
	<select id="available_stages" name="available_stages" multiple="multiple" size="30" style="width:300px">
	<%=currPage.getAvailStageOptions()%>
	</select>
	<br>
	<br>
</td>

<td>
	<br><br>
	<input type="button" value=">>" name="add_avail" id="add_avail" onclick="addSrcToDest('available_stages', 'selected_stages', false)"  ><br>
	<input type="button" value="&lt;&lt;" name="remove_avail" id="remove_avail" onclick="removeFromList('selected_stages')"  >
</td>

<td>
	<select id="selected_stages" name="selected_stages" multiple="multiple" size="30" style="width:300px">
	<%=currPage.getExistingStageOptions()%>
	</select>
</td>

<td>
	<br><br>
	<input type="button" value="up" id="upbutton" onclick="moveOption('selected_stages', true)"><br>
	<input type="button" value="dn" id="dnbutton" onclick="moveOption('selected_stages', false)">
</td>
</tr>

<tr><td colspan="3"><div id="stage_desc">&nbsp;</div></td></tr>

<tr><td class="cmdlink" colspan="3"><a href="javascript:submitSelectStages()">Next &gt;&gt;</a></td></tr>

</table>
<input type="hidden" name="next" value="submit">
</form>

<%@ include file = "WEB-INF/includes/footer.jsp" %>
