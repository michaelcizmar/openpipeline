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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.pipeline.item.Token;
import org.openpipeline.pipeline.item.TokenList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.CharSpan;

public class SimpleTokenizer extends Stage implements NodeVisitor {
	
	final public static int TYPE_WORD = 1;
	final public static int TYPE_NUMBER = 2;

	// Split on anything except a letter or a digit
	// \w is a regex expression for a word char. See java.util.regex.Pattern
	private Pattern pattern = Pattern.compile("[\\w]+");
	private Matcher matcher = pattern.matcher("");
	

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this); // traverses the nodes, calls processNode() below once for each
		
		if (nextStage != null) {
			nextStage.processItem(item);
		}
	}
	
	public void processNode(Node node) {
		if (node.getValue() != null && node.getValue().size() > 0) {
			addTokens(node);			
		}
	}
	
	/**
	 * Add the tokens to this text node. This method is public so it can
	 * be used on individual nodes in special cases.
	 * @param node the node to tokenize
	 */
	public void addTokens(Node node) {
		CharSpan span = node.getValue();
		
		TokenList tokenList = (TokenList) node.getAnnotations("token");
		if (tokenList == null) {
			tokenList = new TokenList(span.size() * 6); // guestimate the size
			node.putAnnotations("token", tokenList);
		}
		
		int wordOffset = 0;
		matcher.reset(span);
		
		while (matcher.find()) {
			int off = matcher.start();
			int size = matcher.end() - off;
			
			Token token = new Token();
			token.setBuffer(node.getItem().getBuffer());
			token.setOffset(off + span.getOffset());
			token.setSize(size);
			token.setType(TYPE_WORD);
			token.setWordOffset(wordOffset);
			tokenList.append(token);

			wordOffset++;
		}
	}
	

	@Override
	public String getDescription() {
		return "A simple tokenizer that breaks on whitespace, numbers, and punctuation ";
	}


	@Override
	public String getDisplayName() {
		return "Simple Tokenizer";
	}


	
}
