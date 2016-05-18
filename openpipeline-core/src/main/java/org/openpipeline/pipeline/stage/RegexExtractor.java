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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.pipeline.item.TextValue;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.WildcardMatcher;

/**
 * Scans an attribute value for strings that match a regular
 * expression, extracts those strings, and adds them as separate
 * attribute values.
 */
public class RegexExtractor extends Stage implements NodeVisitor {

	private String attributeToAdd;
	private String regex;
	private Pattern pattern;
	private Matcher matcher;
	private WildcardMatcher attributeIncluder = new WildcardMatcher();
	
	@Override
	public void initialize() {
		attributeIncluder.setCaseSensitive(true);
		
		List<String> attrsToInclude = params.getValues("attributes-to-include");
		List<String> attrsToExclude = params.getValues("attributes-to-exclude");
		
		attributeIncluder.setIncludePatterns(attrsToInclude);
		attributeIncluder.setExcludePatterns(attrsToExclude);

		attributeToAdd = params.getProperty("attribute-to-add");
		regex = params.getProperty("regex");
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher("");
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this);
		super.pushItemDownPipeline(item);
	}

	@Override
	public void processNode(Node node) throws PipelineException {
		
		if (!attributeIncluder.isIncluded(node.getName())) {
			return;
		}
		
		TextValue val = node.getValue();
		if (val != null) {
			matcher.reset(val);
			while (matcher.find()) {
				String substring = matcher.group();
				node.getItem().getRootNode().addNode(attributeToAdd, substring);
			}
		}
			
	}

	@Override
	public String getDescription() {
		return "Extracts strings that match a regular expression from an attribute value.";
	}

	@Override
	public String getDisplayName() {
		return "Regex Extractor";
	}
	
	@Override
	public String getConfigPage() {
		return "stage_regex_extractor.jsp";
	}


}
