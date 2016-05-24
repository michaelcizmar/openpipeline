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
package org.openpipeline;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openpipeline.pipeline.docfilter.TestDocFilterFactory;
import org.openpipeline.pipeline.docfilter.TestXMLFilter;
import org.openpipeline.pipeline.item.TestAttributeDef;
import org.openpipeline.pipeline.stage.TestFlattener;
import org.openpipeline.server.Server;

/**
 * Test suite that includes all OpenPipeline unit tests
 */
public class AllTests extends TestCase {

    public void test() {
    	
    	System.setProperty("app.home", "/dev/diesel.search4/openpipeline/openpipeline-launcher");
    	Server.getServer().getHomeDir();
    	
        TestSuite suite = new TestSuite();
		suite.addTest(new TestSuite(TestDocFilterFactory.class));
		suite.addTest(new TestSuite(TestAttributeDef.class));
		suite.addTest(new TestSuite(TestFlattener.class));
		suite.addTest(new TestSuite(TestXMLFilter.class));
		//suite.addTest(new TestSuite(TestHTMLSkipReader.class));
		//suite.addTest(new TestSuite(TestHTMLAnalyzer2.class)); temporary removal
		
		//suite.addTest(new TestSuite(TestHTMLFilter.class));
		junit.textui.TestRunner.run(suite);
    }

    
}


