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
package org.openpipeline.pipeline.docfilter;

import junit.framework.TestCase;

public class TestDocFilterFactory extends TestCase {

	public void testGetFilter() {
		DocFilterFactory factory = new DocFilterFactory();
		DocFilter filt = factory.getFilterByFilename("/yo/gjgjg/ggg.hhhhello.htm");

		//if (!(filt instanceof HTMLFilter))
		//	fail("htm");

		//filt = factory.getFilterByFilename("hello.jsp");
		//if (!(filt instanceof HTMLFilter))
		//	fail("jsp");

		filt = factory.getFilterByFilename("hello.txt");
		if (!(filt instanceof PlainTextFilter))
			fail("txt");
	}

	public void testGetMimeType() {
		DocFilterFactory factory = new DocFilterFactory();
		DocFilter filt = factory.getFilterByMimeType("text/html");
		//if (!(filt instanceof HTMLFilter)){
		//	fail("The returned filter should be a HTMLfilter.");
		//}
		
		filt = factory.getFilterByMimeType("text/plain");
		if (!(filt instanceof PlainTextFilter)){
			fail("The returned filter should be a PlainTextFilter.");
		}

	}
}
