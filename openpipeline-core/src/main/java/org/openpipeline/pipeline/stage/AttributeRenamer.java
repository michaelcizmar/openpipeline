package org.openpipeline.pipeline.stage;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.scheduler.PipelineException;

/**
 * Renames attributes in an item.
 */
public class AttributeRenamer extends Stage implements NodeVisitor {

	private String attributeToRename;
	private String newAttributeName;

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this);
		super.pushItemDownPipeline(item);
	}

	@Override
	public void processNode(Node node) throws PipelineException {
		if (node.getName().equals(attributeToRename)) {
			node.setName(newAttributeName);
		}
	}

	@Override
	public void initialize() {
		attributeToRename = params.getProperty("attribute-to-rename");
		newAttributeName = params.getProperty("new-attribute-name");
	}

	@Override
	public String getDescription() {
		return "Renames specific attributes in an item.";
	}

	@Override
	public String getDisplayName() {
		return "Attribute Renamer";
	}

	@Override
	public String getConfigPage() {
		return "stage_attribute_renamer.jsp";
	}
}
