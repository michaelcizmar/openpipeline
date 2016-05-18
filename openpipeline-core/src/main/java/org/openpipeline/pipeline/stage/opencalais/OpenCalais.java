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
package org.openpipeline.pipeline.stage.opencalais;

import java.util.Arrays;
import java.util.List;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeList;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.pipeline.item.Token;
import org.openpipeline.pipeline.item.TokenList;
import org.openpipeline.pipeline.stage.Stage;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastStringBuffer;

/**
 *	This class is responsible for generating annotations for a given
 *	item using the OpenCalais (http://www.opencalais.com) web services API.
 *	The entities are returned in a pre-defined simple XML format. For
 *	more details see: 
 *	<a href="http://opencalais.com/documentation/calais-web-service-api/interpreting-api-response/simple-format">Simple Format</a>
 */
public class OpenCalais extends Stage implements NodeVisitor {
	/*
	 * This stage can make only 40,000 requests per day. 
	 * For each request, the text cannot be greater than 100,000 characters.
	 */
	private final static int MAX_CONTENT_LEN = 100000; // This is the limit enforced by OpenCalais Web Services API.
	private String apiKey;
	private CalaisSoap calaisClient;
	private String paramXML = "";
	private String[] tagNames;

	@Override
	public String getDescription() {
		return "Extracts entities using OpenCalais's Web Service API. ";
	}

	@Override
	public String getConfigPage() {
		return "stage_opencalais.jsp";
	}

	@Override
	public String getDisplayName() {
		return "OpenCalais Entity Extractor";
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this); // traverses the nodes, calls processNode() below once for each

		if (this.nextStage != null) {
			this.nextStage.processItem(item);
		}
	}

	@Override
	public void initialize() throws PipelineException {
		/*
		 * Set the API key.
		 */
		this.apiKey = this.params.getProperty("api-key");
		Calais calais = new Calais();
		this.calaisClient = calais.getCalaisSoap();

		/*
		 * Set the tag names.
		 */
		List<String> tags = this.params.getValues("tags");
		this.tagNames = new String[tags.size()];
		for (int i = 0; i < tags.size(); i++) {
			String tag = tags.get(i);
			this.tagNames[i] = tag.trim().toLowerCase();
		}

		Arrays.sort(this.tagNames);

		/*
		 * Construct paramXML 
		 */
		this.paramXML = getParamXML();
	}

	/**
	 * TODO: May be this should be made available on the stage configuration page.
	 * OpenCalais Web Service parameters.
	 * @return
	 */
	private String getParamXML() {
		FastStringBuffer buf = new FastStringBuffer();
		buf
				.append("<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
		buf
				.append("<c:processingDirectives c:contentType=\"text/txt\" c:enableMetadataType=\"GenericRelations\" c:outputFormat=\"Text/Simple\">");
		buf.append("</c:processingDirectives>");
		buf.append("</c:params>");
		return buf.toString();
	}

	public void processNode(Node node) throws PipelineException {
		CharSpan span = node.getValue();
		if (span == null || span.size() == 0) {
			return;
		}

		/*
		 * Extract entities from tags specified by the user.
		 */
		String nodeTagName = node.getName();
		int index = Arrays.binarySearch(this.tagNames, nodeTagName);
		if (index < 0)
			return;

		Node itemRoot = node.getItem().getRootNode();

		if (span.size() <= MAX_CONTENT_LEN) {
			extractEntities(itemRoot, span.toString());
		} else {
			/*
			 * If the content length is greater than 100,000. 
			 * Text is broken into sentences and mulitple
			 * requests are made to the OpenCalais web service API.
			 */
			TokenList sentences = (TokenList) node.getAnnotations("sentences");
			FastStringBuffer buf = new FastStringBuffer();
			for (int i = 0; i < sentences.size(); i++) {
				Token sentence = sentences.get(i);
				if (buf.size() + sentence.size() > MAX_CONTENT_LEN) {
					extractEntities(itemRoot, buf.toString());
					buf.clear();
				}
				buf.append(span.getArray(), sentence.getOffset(), sentence.getOffset() + sentence.size());
			}

			//Catch the last one.
			if (buf.size() > 0) {
				extractEntities(itemRoot, buf.toString());
			}
		}
	}

	/**
	 * @param itemRoot
	 * @param content
	 * @throws PipelineException
	 */
	private void extractEntities(Node itemRoot, String content) throws PipelineException {
		Item responseXML = new Item();

		// Process an node content via OpenCalais Web Service API.
		String response = this.calaisClient.enlighten(this.apiKey, content, this.paramXML);
		responseXML.importXML(response);

		/*
		 * Parse OpenCalais Simple XML response.
		 */
		Node root = responseXML.getRootNode();
		if ("Error".equals(root.getName())) {
			throw new PipelineException(root.toString());
		}
		NodeList itemChildren = itemRoot.getChildren();

		NodeList children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Node child = children.get(i);
			if ("CalaisSimpleOutputFormat".equals(child.getName())) {
				NodeList entities = child.getChildren();
				for (int j = 0; j < entities.size(); j++) {
					Node entity = entities.get(j);
					itemChildren.append(entity);
				}
			}
		}
	}
}
