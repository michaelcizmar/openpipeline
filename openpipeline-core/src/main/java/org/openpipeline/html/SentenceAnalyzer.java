package org.openpipeline.html;

import java.text.BreakIterator;
import java.util.List;

import org.openpipeline.util.CharArrayIterator;
import org.openpipeline.util.CharSpan;

/**
 * Populates a list of sentence boundaries.
 */
public class SentenceAnalyzer {

	private CharArrayIterator charIterForSentences = CharArrayIterator.newSentenceInstance();
	private BreakIterator sentenceIter = BreakIterator.getSentenceInstance();
	
	/**
	 * Analyze the text in the array, populate a list of sentences.
	 * inTitle is just a flag for whether the sentence is in the title 
	 * of the doc or not.
	 */
	public void addSentences(CharSpan text, List<Span> sentences, boolean inTitle) {
		
		// the BreakIterator returns contiguous, non-overlapping spans corresponding to sentences
		charIterForSentences.setText(text.getArray(), text.getOffset(), text.size());
		sentenceIter.setText(charIterForSentences);

		// roll through the sentences, tokenize them
		int start = sentenceIter.first();
		while (true) {
			int end = sentenceIter.next();
			if (end == BreakIterator.DONE) {
				break;
			}
			
			Span span = new Span();
			span.setCleanTextStart(start);
			span.setCleanTextEnd(end);
			span.setInTitle(inTitle);
			span.setText(text);
			
			trimWhitespace(text, span);
			
			if (span.getCleanTextSize() == 0) {
				continue;
			}
			
			sentences.add(span);
			
			start = end;
		}
	}

	
	/**
	 * The BreakIterator insists on including superfluous whitespace
	 * before and after sentences. Strip it.
	 */
	private void trimWhitespace(CharSpan text, Span span) {

		int start = span.getCleanTextStart() + text.getOffset();
		int end = span.getCleanTextEnd() + text.getOffset();
		char [] array = text.getArray();
			
		while (start < end && Character.isWhitespace(array[start])) {
			start++;
		}
		
		end--; // point at last char in sentence
		while (end > start && Character.isWhitespace(array[end])) {
			end--;
		}
		end++; // point it past end of sentence
		
		span.setCleanTextStart(start - text.getOffset());
		span.setCleanTextEnd(end - text.getOffset());
	}
	
}	
	
	
	
	
	
	

