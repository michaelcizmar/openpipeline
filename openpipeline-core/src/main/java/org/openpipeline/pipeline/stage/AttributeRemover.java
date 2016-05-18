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

import java.util.List;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.WildcardMatcher;

/**
 * Removes attributes from an item. Attributes can be specified
 * by name to be included or excluded. Wildcards supported.
 */
public class AttributeRemover extends Stage {
	/* 
	 * To remove attributes in an XML hierarchy of tags:
	 * Start at the top.
	 * For each child, 
	 *   if it is excluded, skip it. Don't go further down the tree.
	 *   if it is included, then include it and all its children.
	 *   if it is neither included nor excluded, then we need to look
	 *     at its children. Recurse down and repeat the process. If
	 *     we get to a leaf node and it is neither included nor excluded,
	 *     then include it unless there are some includes, in which case 
	 *     it should be excluded. 
	 */
	
	private WildcardMatcher includes = new WildcardMatcher();
	private WildcardMatcher excludes = new WildcardMatcher();
	
	
	
	@Override
	public void initialize() throws PipelineException {
		includes.setCaseSensitive(true);
		excludes.setCaseSensitive(true);
		
		List<String> attrsToInclude = params.getValues("attributes-to-include");
		List<String> attrsToExclude = params.getValues("attributes-to-exclude");
		
		if (attrsToInclude.size() == 0 && attrsToExclude.size() == 0) {
			throw new PipelineException("You must specify either includes or excludes or both in the Attribute Remover stage");
		}
		
		includes.setIncludePatterns(attrsToInclude);
		excludes.setExcludePatterns(attrsToExclude);
	}
	

	@Override
	public void processItem(Item item) throws PipelineException {
		
		// can't use the NodeVisitor here because we're removing nodes as we go.
		recurseNode(item.getRootNode());
		
		super.pushItemDownPipeline(item);
	}

	/**
	 * Rolls through the nodes recursively. Returns false if a node
	 * should be removed. Removes nodes as it goes.
	 * @param tag
	 * @return false if a node should be removed
	 */
	private boolean recurseNode(Node node) {
		
		String namePath = node.getNamePath();
		
		if (!excludes.isIncluded(namePath)) {
			return false;
		}
		
		if (includes.isIncluded(namePath)) {
			return true;
		}
		
		// if we get here, then the node is neither included nor excluded. 
		// must recurse
		// if it happens that this node has a child that *is* included,
		// then this node will be included, but its attributes should not be.
		if (node.hasAttributes()) {
			node.getAttributes().setSize(0);
		}

		if (node.hasChildren()) {
			int childCount = node.getChildCount();
			NodeList children = node.getChildren();
			int ptr = 0;
			
			for (int i = 0; i < childCount; i++) {
				Node child = children.get(i);
				if (!recurseNode(child)) {
					continue;
				}
				
				children.put(ptr, child);
				ptr++;
			}
			children.setSize(ptr);

		} else {
			// node does not have children. It's a leaf.
			// it failed the includes.isIncluded() test above,
			// which means there are some includes, and this node
			// isn't in the list
			return false;
		}
	
		return true;
	}


	@Override
	public String getDescription() {
		return "Removes specific attributes from an item.";
	}

	@Override
	public String getDisplayName() {
		return "Attribute Remover";
	}
	
	@Override
	public String getConfigPage() {
		return "stage_attribute_remover.jsp";
	}
}
