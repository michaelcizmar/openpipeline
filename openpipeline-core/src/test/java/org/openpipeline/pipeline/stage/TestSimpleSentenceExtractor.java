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

import junit.framework.TestCase;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.TokenList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.XMLConfig;
public class TestSimpleSentenceExtractor extends TestCase {
	private Stage sentenceExtractor;
	private Item item = new Item();

	@Override
	public void setUp() throws Exception {
		// Import text.
		this.item.importXML(this.xml);
		
		//	Initialize the stage.
		this.sentenceExtractor = new SimpleSentenceExtractor();
		
		//	Set parameter(s).
		XMLConfig params = new XMLConfig();
		params.setProperty("language", "en");
		this.sentenceExtractor.setParams(params);
	}

	public void test() throws PipelineException {
		this.sentenceExtractor.initialize();
		this.sentenceExtractor.processItem(this.item);
		TokenList sentences = (TokenList) this.item.getRootNode().getAnnotations("sentence");
		for (int i = 0; i < sentences.size(); i++) {
			assertEquals(sentences.get(i).toString(), this.correctSentences[i]);
		}
	}

	private String xml = "<item>\"Hello World!\" This is an example of a simple sentence extractor.</item>";
	private String[] correctSentences = { "\"Hello World!\" ", "This is an example of a simple sentence extractor." };
}
