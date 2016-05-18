<%@ page import = "org.openpipeline.server.pages.*" %>
<%
ConfigureStagesPage currPage = (ConfigureStagesPage)session.getAttribute("currpage");
%>

<table>

	<tr>
		<th colspan="3">Attribute Splitter</th>
	</tr>

	<tr valign="top">
		<td colspan="3">
		<p>
		Splits an attribute value into parts using a regular expression,
		and adds each part as a new attribute value.
		</p>
		<p>
		For example, suppose there is an attribute named "colors" with the value
		"red,green,blue". You want to add a new multi-valued attribute named
		"color" with three separate values. Set "Attribute to Split" to "colors",
		"Attribute to Add" to "color", and "Split expression" to ",".
		</p>
		<p>
		To remove the old "colors" attribute, add an Attribute Remover stage
		to the pipeline.
		</p>
		</td>
	</tr>

	<tr valign="top">
		<td><b>Attribute to Split:</b></td>
		<td><%=currPage.textField("attribute-to-split")%></td>
		<td>Specify the name of the attribute to split</td>
	</tr>

	<tr valign="top">
		<td><b>Attribute to Add:</b></td>
		<td><%=currPage.textField("attribute-to-add")%></td>
		<td>Specify the name of the attribute where the individual parts
		will be added.</td>
	</tr>

	<tr valign="top">
		<td><b>Split expression:</b></td>
		<td><%=currPage.textField("split-expression")%></td>
		<td>This is a regular expression that matches the characters
		on which the value should be split. Internally, this stage
		uses the <a href="http://download.oracle.com/javase/6/docs/api/java/lang/String.html#split%28java.lang.String%29">
		Java String.split() method</a>. See the Javadoc for more detail on
		how this value works.</td>
	</tr>


</table>	
	