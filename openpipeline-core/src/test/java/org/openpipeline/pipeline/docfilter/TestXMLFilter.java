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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;


public class TestXMLFilter extends TestCase {

	String xml = "<foo><bar>bar</bar></foo>";
	
	public void test() throws Exception {
		
		System.setProperty("app.home", "/temp");
		
		InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		
		Item item = new Item();
		DocBinary docBinary = new DocBinary();
		docBinary.setInputStream(in);
		item.setDocBinary(docBinary);
		
		XMLFilter filt = new XMLFilter();
		
		filt.processItem(item);
		
		String out = item.toString();
		
		if (!out.equals(xml)) {
			fail();
		}
		
	}
}
