<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Regex Extractor</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
		<p>
		Scans an attribute value for strings that match a regular
		expression, extracts those strings, and adds them as separate
		attribute values.
		</p>
		<p>
		For example, to extract a US zipcode, use the expression:<br/>
		</p>
		<p>
		\d{5}(-\d{4})*
		</p>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Attributes to include:</b></td>
		<td><%=currPage.textArea("attributes-to-include_SPLIT", "40", "10", false)%></td>
		<td>Specify the names of the attributes to scan. To be included, an
		attribute must match at least one entry in the list. Wildcards (*, ?) are allowed.
		To include all attributes, leave this box empty. Example:<br>
		<i>
		Title<br>
		TitleText<br>
		Text<br>
		MyAttr*<br>
		</i>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Attributes to exclude:</b></td>
		<td><%=currPage.textArea("attributes-to-exclude_SPLIT", "40", "10", false)%></td>
		<td>Specify the names of the attributes to exclude from the scan. If an attribute
		name matches any entry in the list, it will not appear in the output. 
		Wildcards (*, ?) are allowed. Example:<br>
		<i>
		Href*<br>
		Par*<br>
		</i>
		</td>
	</tr>
	
	<tr valign="top">
		<td><b>Attribute to Add:</b></td>
		<td><%=currPage.textField("attribute-to-add")%></td>
		<td>Specify the name of the attribute where the strings that the regular
		expression matches will be added.</td>
	</tr>

	<tr valign="top">
		<td><b>Regular Expression:</b></td>
		<td><%=currPage.textArea("regex", "40", "10", true)%></td>
		<td>This is a regular expression that matches the characters
		to extract. See the <a href="http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html">Java Pattern class</a>
		for more detail on the	syntax which is allowed here.
		</td>
	</tr>
	

</table>	
	