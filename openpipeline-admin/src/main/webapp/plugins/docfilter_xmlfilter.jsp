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

<tr>
<td><b>Items Tag</b></td>
<td><%=currPage.textField(name + ".items-tag", 40, -1, XMLFilter.DEFAULT_ITEMS_TAG)%></td>
<td>The name of the tag in the XML file that indicates the start of a multi-item section.
This needs to be the first tag in the file.
Defaults to "<%=XMLFilter.DEFAULT_ITEMS_TAG%>".</td>
</tr>

<tr>
<td><b>Item Tag</b></td>
<td><%=currPage.textField(name + ".item-tag", 40, -1, XMLFilter.DEFAULT_ITEM_TAG)%></td>
<td>The tag that starts an item in a multi-item file.
Defaults to "<%=XMLFilter.DEFAULT_ITEM_TAG%>".</td>
</tr>

<tr>
<td><b>ItemId Tag</b></td>
<td><%=currPage.textField(name + ".itemid-tag", 40, -1, XMLFilter.DEFAULT_ITEMID_TAG)%></td>
<td>The tag that encloses the itemId for the item in a multi-item file.
Defaults to "<%=XMLFilter.DEFAULT_ITEMID_TAG%>".</td>
</tr>

<tr>
<td><b>Attributes Tag</b></td>
<td><%=currPage.textField(name + ".attribute-tag", 40, -1, XMLFilter.DEFAULT_ATTRIBUTE_TAG)%></td>
<td>The tag that defines an attribute. This one probably shouldn't be changed.
Defaults to "<%=XMLFilter.DEFAULT_ATTRIBUTE_TAG%>".</td>
</tr>

<tr>
<td><b>Omit Whitespace</b></td>
<td><%=currPage.checkbox(name + ".omit-whitespace", XMLFilter.DEFAULT_OMIT_WHITESPACE)%></td>
<td>Check this box to omit ignorable whitespace from the item. This includes
spaces, tabs, newlines, etc. between tags in cases where there is no other content
between the tags. Defaults to "<%=XMLFilter.DEFAULT_OMIT_WHITESPACE%>".</td>
</tr>
