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
package org.openpipeline.logger;

import junit.framework.TestCase;

/**
 * This class demonstrates how to replace the internals of log4j
 * so it uses a subclass for logging. This technique isn't used,
 * because we've switched to logback, but don't delete it just
 * yet, in case we have to switch back.
 */
public class TestLog4j extends TestCase {
	
	/*
	public void test() {
		
		Object guard = new Object();
		MyHierarchy hierarchy = new MyHierarchy();
		LogManager.setRepositorySelector(new DefaultRepositorySelector(hierarchy), guard);
		
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("test");
		logger.info("foo");
	}

	class MyHierarchy extends Hierarchy {
		org.apache.log4j.spi.LoggerFactory factory = new MyLogFactory();
		
		public MyHierarchy() {
			super(new MyLogger("root", Level.INFO));
		}

		@Override
		public org.apache.log4j.Logger getLogger(String name) {
			org.apache.log4j.Logger logger = super.getLogger(name, factory);
			
			ConsoleAppender app = new ConsoleAppender();
			
			logger.addAppender(app);
			
			return logger;
		}
	}
	
	class MyLogFactory implements org.apache.log4j.spi.LoggerFactory {
		public org.apache.log4j.Logger makeNewLoggerInstance(String name) {
			return new MyLogger(name);
		}
	}
	
	public class MyLogger extends org.apache.log4j.Logger {

		protected MyLogger(String name, Level level) {
			super(name);
			super.setLevel(level);
		}
		
		protected MyLogger(String name) {
			super(name);
			System.out.println("created mylogger");
		}

		@Override
		public void info(Object message) {
			super.info("mylogger:" + message);
		}
		
	}
	
	
	
	public void xtest() {
		String home = "C:/dev/openpipeline/trunk/openpipeline";
		System.setProperty("app.home", home);
		
		
		//LogManager.setRepositorySelector(selector, guard)
		
		
	    Logger logger0 = LoggerFactory.getLogger("test.0");
	    logger0.info("log to test logger");

	    Logger logger1 = LoggerFactory.getLogger("test.1");
	    logger1.info("log to test1");
		
	    logger1.info("log to test1");
	    
	    
	    logger0.info("log to test logger");
	    
		
		/*
		// init the logger
		Server.getServer();
		
		Logger logger = Logger.getLogger("foo");
		
		logger.info("foo bar");
		* /
		
	}
*/

}
