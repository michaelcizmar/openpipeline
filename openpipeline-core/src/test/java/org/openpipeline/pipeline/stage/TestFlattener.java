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

import junit.framework.TestCase;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.FastStringBuffer;

/**
 * Test to make the Flattener stage flattens things correctly.
 */
public class TestFlattener extends TestCase {
	
	public void test() throws PipelineException {
		Item item = new Item();
		item.importXML(xml);
		// System.out.println(item.toString());
		
		Flattener flattener = new Flattener();
		flattener.processItem(item);
		
		FastStringBuffer buf = new FastStringBuffer();
		item.appendXMLtoBuffer(buf);
		
		if (!buf.equals(correct)) {
			System.out.println(buf.toString());
			fail();
		}
		
	}

	
	String xml = 
		"<item>" +
			"text" +
			"<sub0>" +
				"<sub1></sub1>" +                   // empty tags not added
				"<sub1 name=\"value\">" +
					"before" +
					"<sub2>between</sub2>" +
					"after" +
				"</sub1>" +
			"</sub0>" +
		"</item>";
	
	
	String correct = "<item>text<sub1 name=\"value\"></sub1><sub1>before</sub1><sub2>between</sub2><sub1>after</sub1></item>";
}
