package org.openpipeline.logger;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class TestLogger extends TestCase  {

	public void test() throws IOException {
		
		String home = "/dev/openpipeline/trunk/openpipeline-launcher";
		System.setProperty("app.home", home);
		
		LoggerSetup.init(home);
		
		Logger logger = LoggerFactory.getLogger("com.dieselpoint.search.Index.myindexname1");
		Logger logger2 = LoggerFactory.getLogger("com.dieselpoint.search.Index.myindexname2");
		
		logger.info("this is for myindexname1");
		logger2.info("this is for myindexname2");
		
		
	}
	
}
