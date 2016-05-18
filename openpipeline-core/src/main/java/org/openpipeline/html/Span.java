package org.openpipeline.html;


/**
 * Describes a span of text within a buffer. Contains
 * pointers to the start and end of the span within
 * HTML, and the matching span in the text stripped of html.
 *
 */
public class Span {
	// offsets are relative to the start of text attr
	private int htmlStart;
	private int htmlEnd;
	private int cleanTextStart;
	private int cleanTextEnd;
	
	private boolean inTitle;
	private int wordOffset;
	private int wordCount;
	private CharSequence text;
	

	public int getHtmlStart() {
		return htmlStart;
	}

	public void setHtmlStart(int htmlStart) {
		this.htmlStart = htmlStart;
	}

	public int getHtmlEnd() {
		return htmlEnd;
	}

	public void setHtmlEnd(int htmlEnd) {
		this.htmlEnd = htmlEnd;
	}

	public int getCleanTextStart() {
		return cleanTextStart;
	}

	public void setCleanTextStart(int cleanTextStart) {
		this.cleanTextStart = cleanTextStart;
	}

	public int getCleanTextEnd() {
		return cleanTextEnd;
	}

	public void setCleanTextEnd(int cleanTextEnd) {
		this.cleanTextEnd = cleanTextEnd;
	}

	public boolean isInTitle() {
		return inTitle;
	}

	public void setInTitle(boolean inTitle) {
		this.inTitle = inTitle;
	}

	public int getWordOffset() {
		return wordOffset;
	}

	public void setWordOffset(int wordOffset) {
		this.wordOffset = wordOffset;
	}

	/*
	public void addOffset(int offset) {
		htmlStart += offset;
		htmlEnd += offset;
		cleanTextStart += offset;
		cleanTextEnd += offset;
	}
	*/

	public int getHtmlSize() {
		return htmlEnd - htmlStart;
	}

	/**
	 * Just for debugging
	 */
	public void setText(CharSequence text) {
		this.text = text;
	}
	
	public String toString() {
		if (text == null) {
			return "no text";
		}
		return "wordoff=" + wordOffset + ": html:\"" + text.subSequence(htmlStart, htmlEnd).toString() + "\" plain:\"" + text.subSequence(cleanTextStart, cleanTextEnd) + '"';
	}

	public int getCleanTextSize() {
		return cleanTextEnd - cleanTextStart;
	}
	
	public boolean isWhitespace() {
		for (int i = cleanTextStart; i < cleanTextEnd; i++) {
			if (!Character.isWhitespace(text.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}
	
	public int getWordCount() {
		return wordCount;
	}
}
