<%
// this exists as a separate page so that other stages that need docfilters can use it
%>
<tr>
	<td><b>Add metadata:</b></td>
	<td><%=currPage.checkbox("add-metadata", true)%></td>
	<td>If checked, this stage will add doctype, url, lastupdate, and filesize fields to the item.</td>
</tr>

<%
DocFilterFactory factory = new DocFilterFactory();
List<DocFilter> list = factory.getDocFilters();
for (DocFilter docFilter: list) {
	session.setAttribute("docfilter", docFilter);
	String configPage = docFilter.getConfigPage();
	%><jsp:include page="<%=configPage%>"></jsp:include><%
}
%>

