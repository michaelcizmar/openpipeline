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
import org.openpipeline.pipeline.item.NodeList;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.scheduler.PipelineException;

/**
 * Flattens an XML hierarchy, promotes all leaf nodes to the top.
 */
public class Flattener extends Stage implements NodeVisitor {

	private Node newRoot;
	
	@Override
	public void processItem(Item item) throws PipelineException {
		
		newRoot = new Node(item, null);
		newRoot.setName(item.getRootNode().getName());
		item.visitNodes(this);
		item.setRootNode(newRoot);
		
		if (nextStage != null) {
			nextStage.processItem(item);
		}
	}
	

	public void processNode(Node node) {
		
		if (node.getValue() != null || node.hasAttributes()) {
			
			String name = node.getName();
			if (name == null) {

				// this is a text-only node. Use the parent's name, unless the parent is the root node.
				Node parent = node.getParent();
				if (parent != null && parent.getParent() != null) {
					name = parent.getName();
				}
			}
			
			Node newNode = newRoot.addNode(name);
			newNode.setValue(node.getValue());
			
			// if the node has any attributes, add them
			if (node.hasAttributes()) {
				int attrCount = node.getAttributeCount();
				NodeList attrs = node.getAttributes();
				for (int i = 0; i < attrCount; i++) {
					Node attr = attrs.get(i);
					newNode.addAttribute(attr);
				}
			}
		}
	}
	
	
	@Override
	public String getDescription() {
		return "Flattens an XML hierarchy, promotes all leaf nodes to the top.";
	}

	@Override
	public String getDisplayName() {
		return "Flattener";
	}



	
}
