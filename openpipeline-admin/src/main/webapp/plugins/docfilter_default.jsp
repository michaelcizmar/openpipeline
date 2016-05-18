<%@ page import = "org.openpipeline.server.pages.*,org.openpipeline.pipeline.docfilter.*,org.openpipeline.pipeline.stage.*,java.util.*" %>
<% 
	DocFilter docFilter = (DocFilter)session.getAttribute("docfilter"); 
	ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
	
	String name = DocFilterStage.PARAM_PREFIX + "." + docFilter.getClass().getName();
	String displayName = docFilter.getDisplayName();
%>

<tr><td colspan="3">&nbsp;</td></tr>
<tr><th colspan="3"><%=displayName%> -- <%=docFilter.getDescription()%></th></tr>

<tr>
<td><b>Enabled</b></td>
<td colspan="2"><%=currPage.checkbox(name + ".enabled", true)%></td>
</tr>

<tr>
<td><b>Extensions</b></td>
<td colspan="2"><%=currPage.textField(name + ".extensions", 40)%></td>
</tr>

<tr>
<td><b>Mimetypes</b></td>
<td colspan="2"><%=currPage.textField(name + ".mimetypes", 40)%></td>
</tr>
