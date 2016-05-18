<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>
<table>
	<tr>
		<th colspan="3">OpenCalais Entity Extractor</th>
	</tr>

	<tr valign="top">
		<td colspan="3">Extracts entities using OpenCalais's web service API. 
		Extracted entities are added as fields in the item. For usage restrictions
		please visit the following link: 
		<a href="http://opencalais.com/documentation/calais-web-service-api/usage-quotas">Usage Quotas</a></td>
	</tr>

	<tr valign="top">
		<td nowrap="nowrap"><b>API Key:</b></td>
		<td><%=currPage.textField("api-key",40)%></td>
		<td>In order to process text, Open Calais requires an API Key. 
		Visit this link (<a href="http://opencalais.com/APIkey">Request API Key</a>)
		to obtain a new key.
		</td>
	</tr>
	
	<tr valign="top">
		<td><b>Tags:</b></td>
		<td><%=currPage.textArea("tags_SPLIT", "40", "5", false)%></td>
		<td>Entities will be extracted from the tags you specify in this box.  
		By default, entities will be extracted from all the tags in the item. 
		Each tag name should be specified on a newline. For example,<br>
		<i>headline</i><br>
		<i>bodytext</i><br>
		</td>
	</tr>
</table>