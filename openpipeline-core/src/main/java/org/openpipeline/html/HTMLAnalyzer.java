package org.openpipeline.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openpipeline.pipeline.item.TextValue;
import org.openpipeline.util.CharSpan;
import org.openpipeline.util.FastCharArrayReader;
import org.openpipeline.util.FastStringBuffer;



/**
 * Parses HTML and breaks it into blocks.
 * Blocks contain text. The rule is that if the system
 * encounters a block-level html tag, a new block is started.
 * Inline tags do not start new blocks.
 * <p>
 * This class marches through the HTML in the 
 * specified range in the buffer, and appends the clean
 * html to the end of the buffer.
 */
public class HTMLAnalyzer {
	
	/*
	 * Text block is the smallest unit of text. It has an offset and size.
	 * Each character entity gets its own block.
	 * Lists of text blocks fall within block-level elements, and may skip
	 * over inline elements.
	 * The main list is a list of textblock lists.
	 */
	
	
	/*
	 * We have to do sentences in this class, instead of
	 * separately, otherwise we have no way to tie the sentence
	 * back to the right char offset in the html.
	 */
	
	static final Pattern INLINE_TAG_PATTERN = 		  
			Pattern.compile("(<|</)(" +
	  		"b|" +
	  		"i|" +
	  		"span|" +
	  		"em|" +
	  		"font|" +
	  		"br|" +
	  		"strong|" +
	  		"u)",
	  		Pattern.CASE_INSENSITIVE);		
	
	private HTMLLexer lexer = new HTMLLexer();
	private List<Line> lines = new ArrayList();
	private Line currentLine;
	
	private boolean inHead;
	private boolean inTitle;
	
	private SentenceAnalyzer sentenceAnalyzer;
	private ArrayList<Span> sentences = new ArrayList();
	
	private FastStringBuffer tagBuf = new FastStringBuffer();
	private FastStringBuffer cleanOutputBuf;
	
	private int startOfCleanText;
	private int sizeOfCleanText;
	
	private void clear() {
		lines.clear();
		inHead = false;
		inTitle = false;
		currentLine = null;
		sentences.clear();
	}

	public void parse(CharSpan text, FastStringBuffer cleanOutputBuf, boolean doSentences) throws IOException {
		
		clear();
		
		this.cleanOutputBuf = cleanOutputBuf;
		this.startOfCleanText = cleanOutputBuf.size();
		
		// populate the lines array, push clean text to output
		createDOM(text);
		
		this.sizeOfCleanText = cleanOutputBuf.size() - startOfCleanText;
		
		if (!doSentences) {
			return;
		}
		
		if (sentenceAnalyzer == null) {
			sentenceAnalyzer = new SentenceAnalyzer();
		}
		
		// roll through lines, get sentence boundaries
		for (Line line: lines) {
			
			// a line is a list of text blocks between block-level elements
			if (line.size() == 0 || line.isWhitespace()) {
				continue;
			}

			int cleanStartLine = line.getCleanTextStart();
			int cleanEndLine = line.getCleanTextEnd();
			TextValue cleanTextLineSpan = new TextValue(cleanOutputBuf);
			cleanTextLineSpan.setOffset(cleanStartLine + text.getOffset());
			cleanTextLineSpan.setSize(cleanEndLine - cleanStartLine);
			
			int firstSentence = sentences.size();
			sentenceAnalyzer.addSentences(cleanTextLineSpan, sentences, line.getInTitle());
			int lastSentence = sentences.size();
				
			for (int i = firstSentence; i < lastSentence; i++) {
				Span sentence = sentences.get(i);
					
				// the sentenceAnalyzer sets the boundaries 
				// relative to the start of the clean text for the line.
				// Must adjust so sentence boundaries are relative to
				// the text attribute.
				sentence.setText(text);
					
				sentence.setCleanTextStart(sentence.getCleanTextStart() + cleanStartLine);
				sentence.setCleanTextEnd(sentence.getCleanTextEnd() + cleanStartLine);
					
				// now fill in the boundaries into the html, which is what gets written to the sentence binary
				sentence.setHtmlStart(line.getOrigHTMLOffset(sentence.getCleanTextStart()));
				sentence.setHtmlEnd(line.getOrigHTMLOffset(sentence.getCleanTextEnd()));
			}
		}
	}
			
			

	public int getStartOfCleanText() {
		return startOfCleanText;
	}

	public int getSizeOfCleanText() {
		return sizeOfCleanText;
	}

	/**
	 * Parse the html into an array of blocks. It's not really a DOM.
	 * @param text
	 * @throws IOException
	 */
	private void createDOM(CharSpan text) throws IOException {
	
		FastCharArrayReader reader = new FastCharArrayReader();
		reader.init(text.getArray(), text.getOffset(), text.size());
		
		lexer.reset(reader);
		
		boolean noIndexMode = false;
		
		while (true) {
			
			int result = lexer.lex();
			if (result == HTMLLexer.EOF) {
				break;
			}

			switch (result) {
			case HTMLLexer.STARTNOINDEX:
				noIndexMode = true;
				break;
			case HTMLLexer.ENDNOINDEX:
				noIndexMode = false;
				break;
			}
			if (noIndexMode) {
				continue;
			}
			
			switch (result) {
			case HTMLLexer.TEXT:
				addBlock(text, lexer);
				break;
			case HTMLLexer.NBSP:
				addBlock(text, lexer, ' ');
				break;
			case HTMLLexer.AMP:
				addBlock(text, lexer, '&');
				break;
			case HTMLLexer.GT:
				addBlock(text, lexer, '>');
				break;
			case HTMLLexer.LT:
				addBlock(text, lexer, '<');
				break;
			case HTMLLexer.QUOT:
				addBlock(text, lexer, '"');
				break;
			case HTMLLexer.APOS:
				addBlock(text, lexer, '\'');
				break;

			case HTMLLexer.IMG:
			case HTMLLexer.STARTANCHOR:
			case HTMLLexer.ENDANCHOR:
				// inline tags. ignore.
				break;
				
            case HTMLLexer.STARTTAG:
            case HTMLLexer.ENDTAG:
            	// if the tag is not an inline tag, then start a new row
            	if (!isInlineTag(lexer)) {
            		startNewLine();
            	}
            	break;
            	
            case HTMLLexer.STARTTITLE:
        		startNewLine();
            	inTitle = true;
            	break;
            case HTMLLexer.ENDTITLE:
        		startNewLine();
            	inTitle = false;
                break;
				
            case HTMLLexer.STARTHEAD:
        		startNewLine();
            	inHead = true;
            	break;
            case HTMLLexer.ENDHEAD:
        		startNewLine();
            	inHead = false;
            	break;
                
			case HTMLLexer.SYMBOL_DECIMAL:
				addBlock(text, lexer, HTMLUtils.convertSymbolDecimal(lexer.getText()));
				break;
			case HTMLLexer.SYMBOL_HEX:
				addBlock(text, lexer, HTMLUtils.convertSymbolHex(lexer.getText()));
				break;
			//case HTMLLexer.SYMBOL_CHAR:
			//	buf.append(convertSymbolChar(matchedText));
			//	break;

			case HTMLLexer.CHAR:
			case HTMLLexer.WHITESPACE:
			case HTMLLexer.NEWLINE:
				// treat these as text
				// will need them inside a row to delimit tokens
				addBlock(text, lexer);
				break;

			default:
				
            	// character entities are returned as 100000 + the entity value
            	if (result > 100000) {
            		char entity = (char)(result - 100000);
            		addBlock(text, lexer, entity);
            	}
				
				// everything else ignored, including unrecognized tags
			}
		}

	}
	
	
	private void startNewLine() {
		currentLine = null;
	}

	
	private boolean isInlineTag(HTMLLexer lexer) {
		lexer.getText(tagBuf);
		return INLINE_TAG_PATTERN.matcher(tagBuf).find();
	}

	
	private void addBlock(CharSpan text, HTMLLexer lexer) {
		addBlock(text, lexer, (char)0);
	}

	/**
	 * Add a block to the current row.
	 * @param lexer
	 * @return
	 */
	private void addBlock(CharSpan text, HTMLLexer lexer, char charEntity) {
		if (currentLine == null) {
			//currentLine = new Line(htmlBuf);
			currentLine = new Line();
			currentLine.setInTitle(inTitle);
			lines.add(currentLine);
		}
		
		// omit everything in the head, except title
		if (inHead && !inTitle) {
			return;
		}
		
		// create the block, append the clean text
		Span block = new Span();
		block.setText(text);
		block.setHtmlStart(lexer.getOffset());
		block.setHtmlEnd(lexer.getOffset() + lexer.getSize());
		
		// make clean text offsets relative to the text attr
		block.setCleanTextStart(cleanOutputBuf.size() - text.getOffset());
		if (charEntity == 0) {
			cleanOutputBuf.append(text.getArray(), text.getOffset() + lexer.getOffset(), lexer.getSize());
		} else {
			cleanOutputBuf.append(charEntity);
		}
		block.setCleanTextEnd(cleanOutputBuf.size() - text.getOffset());
		
		// if this block is contiguous with the previous one then consolidate 
		// them. Reduces the number of blocks, speeds things up.
		int blockCount = currentLine.size();
		if (blockCount > 0) {
			Span prev = currentLine.get(blockCount - 1);
			if (lexer.getOffset() == prev.getHtmlEnd()) {
				// extend prev's size
				prev.setHtmlEnd(lexer.getOffset() + lexer.getSize());
				prev.setCleanTextEnd(cleanOutputBuf.size() - text.getOffset());
				return;
			}
		}
		
		currentLine.add(block);
	}

	
	@SuppressWarnings("serial")
	public class Line extends ArrayList<Span> {
		private boolean inTitle;
		
		public void setInTitle(boolean inTitle) {
			this.inTitle = inTitle;
		}
		
		public int getCleanTextEnd() {
			return get(size() - 1).getCleanTextEnd();
		}

		public int getCleanTextStart() {
			return get(0).getCleanTextStart();
		}

		/**
		 * Get the original html offset from the offset into the clean text.
		 * @param cleanTextStart offset relative to the start of the clean text for the attribute.
		 * @return
		 */
		public int getOrigHTMLOffset(int cleanTextStart) {
			
			// just do a linear search. A binary search would be faster, but more complicated.
			// there should never be a request for an offset which is between blocks,
			// but if there is, treat it as part of the previous block
			int ptr = size() - 1;
			while (ptr >= 0) {
				Span block = get(ptr);
				int offsetIntoBlock = cleanTextStart - block.getCleanTextStart();
				if (offsetIntoBlock >= 0) {
					return block.getHtmlStart() + offsetIntoBlock;
				}
				ptr--;
			}
			// should never get here
			throw new Error("cleanTextStart not found:" + cleanTextStart);
		}

		public boolean getInTitle() {
			return inTitle;
		}

		public boolean isWhitespace() {
			for (Span span: this) {
				if (!span.isWhitespace()) {
					return false;
				}
			}
			return true;
		}

		public boolean containsCleanOffset(int cleanOff) {
			if (size() == 0) {
				return false;
			}
			int cleanStartLine = get(0).getCleanTextStart();
			int cleanEndLine = get(size() - 1).getCleanTextEnd();
			return (cleanOff >= cleanStartLine) && (cleanOff < cleanEndLine);
		}
	}
	
	public List<Span> getSentences() {
		return sentences;
	}

	public int getOrigHTMLOffset(int cleanOff) {
		for (Line line: lines) {
			if (line.containsCleanOffset(cleanOff)) {
				return line.getOrigHTMLOffset(cleanOff);
			}
		}
		throw new Error("cleanTextStart not found:" + cleanOff);
	}

	public List<Line> getLines() {
		return lines;
	}
	

}
