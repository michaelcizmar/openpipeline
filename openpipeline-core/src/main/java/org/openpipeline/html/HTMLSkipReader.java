package org.openpipeline.html;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openpipeline.pipeline.item.TextValue;
import org.openpipeline.util.FastStringBuffer;

/**
 * This isn't used anywhere.
 * A Reader over HTML text that skips the HTML markup.
 */
public class HTMLSkipReader extends Reader {
	
	/*
	 * TODO this is seriously inefficient. Do a bit of hacking on the jsoup source
	 * to set and return Readers. Or char arrays.
	 */
	
	private StringReader stringReader;

	public HTMLSkipReader(String str) {
		reset(str);
	}

	public HTMLSkipReader(TextValue text) {
		reset(text.toString());
	}

	public HTMLSkipReader(Reader reader) throws IOException {
		FastStringBuffer buf = new FastStringBuffer();
		buf.append(reader);
		reset(buf.toString());
	}

	private void reset(String text) {
		Document doc = Jsoup.parse(text);
		String docText = doc.body().text();
		String title = doc.title();
		if (title.length() > 0) {
			docText = title + " " + docText;
		}
		stringReader = new StringReader(docText);
	}
	
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		return stringReader.read(cbuf, off, len);
	}


	@Override
	public void close() throws IOException {
	}



}
