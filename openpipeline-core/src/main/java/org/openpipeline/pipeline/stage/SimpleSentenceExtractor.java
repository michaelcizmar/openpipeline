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

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Locale;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.item.Node;
import org.openpipeline.pipeline.item.NodeVisitor;
import org.openpipeline.pipeline.item.Token;
import org.openpipeline.pipeline.item.TokenList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastStringBuffer;

/**
 *	This class extracts sentences from an item and adds them to the <i>sentence</i> annotation list.
 */
public class SimpleSentenceExtractor extends Stage implements NodeVisitor {
	private BreakIterator sentenceIterator;
	private CharIterator charIterator = new CharIterator();

	@Override
	public String getDescription() {
		return "Extracts sentences from an item.";
	}

	@Override
	public String getDisplayName() {
		return "Simple Sentence Extractor";
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		item.visitNodes(this); // traverses the nodes, calls processNode() below once for each

		if (this.nextStage != null) {
			this.nextStage.processItem(item);
		}
	}

	/**
	 * Extracts sentences from a given text node and added them to the "sentence" annotation list.
	 */
	public void processNode(Node node) {
		CharSpan span = node.getValue();
		if (span == null || span.size() == 0) {
			return;
		}

		FastStringBuffer textBuffer = node.getItem().getBuffer();

		TokenList sentenceList = (TokenList) node.getAnnotations("sentence");

		if (sentenceList == null) {
			sentenceList = new TokenList(span.size() / 6); // guestimate the size
			node.putAnnotations("sentence", sentenceList);
		}

		this.charIterator.reset(span);
		this.sentenceIterator.setText(this.charIterator);

		//Extract sentences from the char span.
		int start = this.sentenceIterator.first();
		while (true) {
			int end = this.sentenceIterator.next();
			if (end == BreakIterator.DONE) {
				break;
			}
			// Add a sentence to the annotation list
			Token sentence = new Token();
			sentence.setOffset(start);
			sentence.setSize(end - start);
			sentence.setBuffer(textBuffer);
			sentenceList.append(sentence);

			//Set the start position for the next sentence.
			start = end;
		}
	}

	/**
	 * Initializes the sentence extractor using either the default
	 * locale or a language code e.g. en(=English), fr(=French). 
	 */
	@Override
	public void initialize() throws PipelineException {
		String language = this.params.getProperty("language");
		try {
			// If the language field is empty, use the default locale.
			Locale locale = Locale.getDefault();
			if (language != null && language.length() == 0) {
				locale = new Locale(language);
			}
			this.sentenceIterator = BreakIterator.getSentenceInstance(locale);
		} catch (Exception e) {
			throw new PipelineException("Locale language not found: " + language);
		}
	}

	@Override
	public String getConfigPage() {
		return "stage_sentence_extractor.jsp";
	}
}

/*
 * Class to iterate over a CharSpan character by character.
 */
class CharIterator implements CharacterIterator {
	private CharSpan span;
	private int pos;
	private int start;
	private int end;

	public void reset(CharSpan span) {
		this.span = span;
		this.start = getBeginIndex();
		this.end = getEndIndex();
		this.pos = this.start;
	}

	public char first() {
		return setIndex(this.start);
	}

	public char last() {
		return setIndex(this.end - 1);
	}

	public int getBeginIndex() {
		return this.span.getOffset();
	}

	public int getEndIndex() {
		return this.span.getOffset() + this.span.size();
	}

	public char current() {
		if (this.pos < this.start || this.pos > this.end) {
			return DONE;
		}
		return this.span.getArray()[this.pos];
	}

	public int getIndex() {
		return this.pos;
	}

	public char next() {
		this.pos++;
		if (this.pos > this.end) {
			this.pos = this.end;
			return DONE;
		} else {
			return this.span.getArray()[this.pos];
		}
	}

	public char previous() {
		this.pos--;
		if (this.pos < this.start) {
			this.pos = this.start;
			return DONE;
		}
		return this.span.getArray()[this.pos];
	}

	public char setIndex(int position) {
		if (position < this.start || position > this.end) {
			throw new IllegalArgumentException("Invalid position." + position);
		}
		this.pos = position;
		return this.span.getArray()[this.pos];
	}

	@Override
	public CharIterator clone() {
		try {
			CharIterator clonedObj = (CharIterator) super.clone();
			return clonedObj;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
