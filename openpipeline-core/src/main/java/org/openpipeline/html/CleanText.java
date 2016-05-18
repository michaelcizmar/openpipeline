package org.openpipeline.html;

import java.io.IOException;
import java.io.Reader;

import org.openpipeline.util.FastCharArrayReader;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.IntArray;

/**
 * A CleanText contains text which has been cleaned of HTML.
 * For each character in the clean text, this object can
 * return an offset into the original HTML.
 * <p>
 * This class marches through the HTML in the 
 * specified range in the buffer, and appends the clean
 * html to the end of the buffer.
 */
public class CleanText {
	
	private FastStringBuffer itemBuf = new FastStringBuffer();
	private IntArray cleanTextOffsets = new IntArray();
	private IntArray origHTMLOffsets = new IntArray();
	private boolean spaceNeeded;
	private int startOfHTML;
	private int startOfCleanText;
	private int sizeOfCleanText;

	
	public void reset(FastStringBuffer buf, int offset, int length) throws IOException {
		clear();
		itemBuf = buf;
		startOfHTML = offset;
		startOfCleanText = itemBuf.size();
		
		FastCharArrayReader reader = new FastCharArrayReader();
		reader.init(itemBuf.getArray(), offset, length);
		createArrays(reader);
	}
	
	/*
	public void reset(Reader reader) throws IOException {
		clear();
		createArrays(reader);
	}
	
	public void reset(TextValue text) throws IOException {
		clear();
		itemBuf = text.getBuffer();
		startOfHTML = text.getOffset();
		startOfCleanText = itemBuf.size();
		
		FastCharArrayReader reader = new FastCharArrayReader();
		reader.init(text.getArray(), text.getOffset(), text.size());
		createArrays(reader);
	}
	*/
	
	public void clear() {
		cleanTextOffsets.clear();
		origHTMLOffsets.clear();
		spaceNeeded = false;
		startOfHTML = 0;
		startOfCleanText = 0;
		sizeOfCleanText = 0;
	}
	

	private void createArrays(Reader reader) throws IOException {
		
		HTMLLexer lexer = new HTMLLexer();
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
				copyTokenToBuffer(lexer, itemBuf);
				break;
			case HTMLLexer.NBSP:
				copyCharToBuffer(lexer, ' ', itemBuf);
				break;
			case HTMLLexer.AMP:
				copyCharToBuffer(lexer, '&', itemBuf);
				break;
			case HTMLLexer.GT:
				copyCharToBuffer(lexer, '>', itemBuf);
				break;
			case HTMLLexer.LT:
				copyCharToBuffer(lexer, '<', itemBuf);
				break;
			case HTMLLexer.QUOT:
				copyCharToBuffer(lexer, '"', itemBuf);
				break;
			case HTMLLexer.APOS:
				copyCharToBuffer(lexer, '\'', itemBuf);
				break;

            case HTMLLexer.STARTTITLE:
            case HTMLLexer.ENDTITLE:
            	spaceNeeded = true;
            	// the actual title gets copied as text
                break;
				
				/*
				 * TODO
				 * add a \n when <br> and <p> tags are seen.
				 * 
				 */
				
			case HTMLLexer.SYMBOL_DECIMAL:
				copyCharToBuffer(lexer, HTMLUtils.convertSymbolDecimal(lexer.getText()), itemBuf);
				break;
			case HTMLLexer.SYMBOL_HEX:
				copyCharToBuffer(lexer, HTMLUtils.convertSymbolHex(lexer.getText()), itemBuf);
				break;
			//case HTMLLexer.SYMBOL_CHAR:
			//	buf.append(convertSymbolChar(matchedText));
			//	break;

			case HTMLLexer.CHAR:
			case HTMLLexer.WHITESPACE:
				copyTokenToBuffer(lexer, itemBuf);
				break;

			default:
				
            	// character entities are returned as 100000 + the entity value
            	if (result > 100000) {
            		char entity = (char)(result - 100000);
    				copyCharToBuffer(lexer, entity, itemBuf);
            	}
				
				// everything else ignored, including unrecognized tags
			}
		}
		
		sizeOfCleanText = itemBuf.size() - startOfCleanText;
	}
	
	
	/**
	 * Add the offsets to the offsets array before any text added.
	 * @param lexer
	 */
	private void addOffsets(HTMLLexer lexer) {
		// if we need some spacing, and the previous char isn't whitespace, add it
		if (spaceNeeded) {
			if (itemBuf.size() > startOfCleanText) { // if this isn't the first token
				char ch = itemBuf.charAt(itemBuf.size() - 1);
				if (!Character.isWhitespace(ch)) {
					itemBuf.append(' ');
				}
			}
			spaceNeeded = false;
		}
		
		// store the offsets
		cleanTextOffsets.append(itemBuf.size());
		origHTMLOffsets.append(lexer.getOffset() + startOfHTML);
	}
	
	/**
	 * Add text from lexer to buffer. Adds the exact text that was recognized.
	 */
	private void copyTokenToBuffer(HTMLLexer lexer, FastStringBuffer buf) {
		addOffsets(lexer);
		
		// copy to buffer
		int size = lexer.getSize();
		buf.ensureCapacity(buf.size() + size);
		lexer.getText(buf.getArray(), buf.size());
		buf.setSize(buf.size() + size);
	}
	
	/**
	 * Add text from lexer to buffer. Adds text that was transformed somehow.
	 * /
	private void copyStringToBuffer(HTMLLexer lexer, String text, FastStringBuffer buf) {
		addOffsets(lexer);
		buf.append(text);
	}
	*/
	
	/**
	 * Add text from lexer to buffer. Adds text that was transformed somehow.
	 */
	private void copyCharToBuffer(HTMLLexer lexer, char ch, FastStringBuffer buf) {
		addOffsets(lexer);
		buf.append(ch);
	}
	
	
	/**
	 * Given the character offset into the clean text, return
	 * the matching offset into the original HTML.
	 */
	public int getOrigHTMLOffset(int cleanTextOffset) {
		int result = cleanTextOffsets.binarySearch(cleanTextOffset);
		
		// exactly at the beginning of a block of text
		if (result >= 0) {
			return origHTMLOffsets.get(result);
		}

		// somewhere inside a block
		int containingElement = -result - 2;
		int offsetIntoContainingElement = cleanTextOffset - cleanTextOffsets.get(containingElement);
		
		return origHTMLOffsets.get(containingElement) + offsetIntoContainingElement;
	}
	

	/**
	 * Return a Reader over the clean text.
	 */
	public Reader getReader() {
		FastCharArrayReader reader = new FastCharArrayReader();
		reader.init(itemBuf.getArray(), startOfCleanText, sizeOfCleanText);
		return reader;
	}

	/**
	 * Return the text stripped of HTML. This is mostly for debugging.
	 */
	public String getCleanText() {
		return new String(itemBuf.getArray(), startOfCleanText, sizeOfCleanText);
	}

	public int getStartOfCleanText() {
		return startOfCleanText;
	}

	public int getSizeOfCleanText() {
		return sizeOfCleanText;
	}

}
