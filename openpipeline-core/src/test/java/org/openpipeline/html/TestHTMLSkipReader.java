package org.openpipeline.html;

import java.io.IOException;
import java.io.StringReader;

import org.openpipeline.util.FastStringBuffer;

import junit.framework.TestCase;


public class TestHTMLSkipReader extends TestCase {

	String in = "<html>foo <head>x<title>title</title><script>stuff here</script>some <b>bold</b>text </html>";
	String correctOutput = "foo  x title some  bold text ";

	public void test() throws IOException {
		StringReader r = new StringReader(in);
		HTMLSkipReader hsr = new HTMLSkipReader(r);
		
		FastStringBuffer buf = new FastStringBuffer();
		buf.append(hsr);
		
		String out = buf.toString();
		if (!out.equals(correctOutput)) {
			fail("in=" + in + " out=" + out);
		}
	}

}
