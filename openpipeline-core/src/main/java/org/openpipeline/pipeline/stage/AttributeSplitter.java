/*******************************************************************************
 * Copyright 2010 Dieselpoint, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.openpipeline.pipeline.stage;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.pipeline.item.TextValue;
import org.openpipeline.scheduler.PipelineException;

/**
 * Splits an attribute value into parts.
 */
public class AttributeSplitter extends Stage implements NodeVisitor {

	private String attributeToSplit;
	private String attributeToAdd;
	private String splitExpression;
	
	@Override
	public void initialize() {
		attributeToSplit = params.getProperty("attribute-to-split");
		attributeToAdd = params.getProperty("attribute-to-add");
		splitExpression = params.getProperty("split-expression");
	}
	

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this);
		super.pushItemDownPipeline(item);
	}

	@Override
	public void processNode(Node node) throws PipelineException {
		if (node.getName().equals(attributeToSplit)) {
			
			TextValue val = node.getValue();
			if (val != null) {
				String [] parts = val.toString().split(splitExpression);
				for (String part: parts) {
					node.getItem().getRootNode().addNode(attributeToAdd, part);
				}
			}
		}
	}

	@Override
	public String getDescription() {
		return "Splits an attribute value into parts using a regular expression.";
	}

	@Override
	public String getDisplayName() {
		return "Attribute Splitter";
	}
	
	@Override
	public String getConfigPage() {
		return "stage_attribute_splitter.jsp";
	}


}
