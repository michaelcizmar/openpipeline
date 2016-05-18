package org.openpipeline.html;

import java.io.IOException;

import junit.framework.TestCase;

import org.openpipeline.util.CharArraySequence;
import org.openpipeline.util.FastStringBuffer;


public class TestHTMLAnalyzer2 extends TestCase {

	String in = "<html>foo " +
			"<head>" +
			"x<title>title</title>" +
			"<script>stuff here</script>" +
			"</head>" +
			"amp: &amp;  raquo: &#187; " +
			"some <b>bold</b>text punctuation.! Bob's</html>";
	
	String correctOutput = "foo titleamp: &  raquo: � some boldtext punctuation.! Bob's";

	String correctSentences = "this isn't finished";
	
	/*
	int [][] correctOffsets = {
			{1, 7},
			{8, 26},
			{17, 69},
			{34, 98},
			{38, 106}
	};
	*/
	
	public void test() throws IOException {

		CharArraySequence span = new CharArraySequence(); // just a convenient CharSpan implementation
		FastStringBuffer buf = new FastStringBuffer();
		span.setArray(buf.getArray());
		buf.append("junk");
		span.setOffset(buf.size());
		buf.append(in);
		span.setSize(buf.size() - span.getOffset()); 
		buf.append("more junk");
		
		FastStringBuffer cleanOutputBuf = new FastStringBuffer();
		cleanOutputBuf.append("randomjunk");
		
		HTMLAnalyzer htmlAnalyzer = new HTMLAnalyzer();
		htmlAnalyzer.parse(span, cleanOutputBuf, true);
		
		String stripped = new String(cleanOutputBuf.getArray(), htmlAnalyzer.getStartOfCleanText(), htmlAnalyzer.getSizeOfCleanText());
		
		//System.out.println(in);
		
		if (!correctOutput.equals(stripped)) {
			System.out.println(stripped);
			fail();
		}
		
		/*
		int startOfCleanText = htmlAnalyzer.getStartOfCleanText();
		int startOfHTML = token.getOffset();

		for (int [] offs: correctOffsets) {

			int cleanOff = offs[0] + startOfCleanText;
			int correctOrigOff = offs[1] + startOfHTML;
			int actualOrigOff = htmlAnalyzer.getOrigHTMLOffset(cleanOff);
			
			if (actualOrigOff != correctOrigOff) {
				fail("actual=" + actualOrigOff + " correct=" + correctOrigOff);
			}
		}
		*/
		
		String sentences =	htmlAnalyzer.getSentences().toString();
		if (!sentences.equals(correctSentences)) {
			System.out.println(sentences);
			fail();
		}
		
		
	}

}
