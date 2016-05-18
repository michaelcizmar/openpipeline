package org.openpipeline.pipeline.stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.scheduler.PipelineException;

/**
 * Changes the attribute value in an item.
 */
public class AttributeValueConverter extends Stage implements NodeVisitor {

	private Map<String, Map<String, String>> attributeToModify;

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this);
		super.pushItemDownPipeline(item);
	}

	@Override
	public void processNode(Node node) throws PipelineException {

		if (attributeToModify.containsKey(node.getName())) {

			Map<String, String> tmp = attributeToModify.get(node.getName());
			String value = node.getValue().toString();

			for (String pattern : tmp.keySet()) {

				String newValue = tmp.get(pattern);

				if (value.matches(pattern)) {
					node.setValue(newValue);
					return;
				}
			}
		}
	}

	@Override
	public void initialize() {

		attributeToModify = new HashMap<String, Map<String, String>>();

		List<String> attributeToModifyList = params
				.getValues("attribute-to-modify");
		for (String attribute : attributeToModifyList) {

			if (attribute.trim().isEmpty()) {
				continue;
			}

			String[] parts = attribute.split(":");
			if (parts.length != 3) {
				continue;
			}

			Map<String, String> tmp = attributeToModify.get(parts[0]);
			if (tmp == null) {
				tmp = new HashMap<String, String>();
			}
			tmp.put(parts[1], parts[2]);

			attributeToModify.put(parts[0], tmp);
		}
	}

	@Override
	public String getDescription() {
		return "Renames specific attributes in an item.";
	}

	@Override
	public String getDisplayName() {
		return "Attribute Value Converter";
	}

	@Override
	public String getConfigPage() {
		return "stage_attribute_value_converter.jsp";
	}
}
