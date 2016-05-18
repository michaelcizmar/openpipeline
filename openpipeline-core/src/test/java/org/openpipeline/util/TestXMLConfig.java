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
package org.openpipeline.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import junit.framework.TestCase;

public class TestXMLConfig extends TestCase {
	
	final static String SEP = System.getProperty("line.separator");
	
	public void testRead() throws IOException {
		
		XMLConfig config = new XMLConfig();
		
		StringReader reader = new StringReader(configString);
		
		config.load(reader);
		
		assertEquals(config.getProperty("admin"), "Sam");
		assertEquals(config.getIntProperty("timeout"), 60);
		
		XMLConfig node = config.getChild("users");
		List vals = node.getValues("username");
		if (!"bob & bill".equals(vals.get(0)))
			fail();
		if (!"fred".equals(vals.get(1)))
			fail();
		
		double gigahertz = config.getChild("server").getDoubleProperty("gigahertz");
		assertEquals(gigahertz, 3.0, 0.1);
	}

	public void testWriteRead() throws IOException {
		XMLConfig config = new XMLConfig();
		config.setProperty("admin", "Sam");
		config.setProperty("timeout", 60 + "");
		
		XMLConfig usersNode = config.addChild("users");
		usersNode.setProperty("username", "bob & bill");
		usersNode.addProperty("username", "fred");

		XMLConfig serverNode = config.addChild("server");
		serverNode.setProperty("gigahertz", "3.0");
		
		String outfile = "/temp/testconfig/config.xml";
		config.save(outfile);
		
		char [] buf = new char [10000];
		FileReader reader = new FileReader(outfile);
		int size = reader.read(buf);
		String newStr = new String(buf, 0, size);
		
		if (!newStr.equals(configString)) {
			fail("mismatch");
		}
	}
	
	public void testApplyTo() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		XMLConfig conf = new XMLConfig();
		conf.setProperty("myString", "strval");
		conf.setProperty("myInt", "2");
		conf.setProperty("otherType", "other");

		ApplyToTester t = new ApplyToTester();
		conf.applyTo(t);
		
		if (!t.getMyString().equals("strval")) {
			fail();
		}
		if (t.getMyInt() != 2) {
			fail();
		}
		if (t.getOtherType() != null) {
			fail();
		}
		
	}
	
	@SuppressWarnings("unused")
	class ApplyToTester {
		private String myString;
		private int myInt;
		private char [] otherType;
		
		public void foo(){}
		private void bar(){}
		
		public void setMyString(String str) {
			this.myString = str;
		}
		public void setMyInt(int i) {
			this.myInt = i;
		}
		public void setOtherType(char [] chars) {
			this.otherType = chars;
		}
		public String getMyString() {
			return myString;
		}
		public int getMyInt() {
			return myInt;
		}
		public char[] getOtherType() {
			return otherType;
		}
	}
	
	
	
	
	String configString = 
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SEP +
	"<root>" + SEP +
	"  <admin>Sam</admin>" + SEP +
	"  <timeout>60</timeout>" + SEP +
	"  <users>" + SEP +
	"    <username>bob &amp; bill</username>" + SEP +
	"    <username>fred</username>" + SEP +
	"  </users>" + SEP +
	"  <server>" + SEP +
	"    <gigahertz>3.0</gigahertz>" + SEP +
	"  </server>" + SEP +
	"</root>" + SEP;

	/*
	String configString = 
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SEP +
	"<entry name=\"root\">" + SEP +
	"  <entry name=\"admin\" value=\"Sam\"/>" + SEP +
	"  <entry name=\"timeout\" value=\"60\"/>" + SEP +
	"  <entry name=\"user\">" + SEP +
	"    <entry name=\"username\" value=\"bob\"/>" + SEP +
	"  </entry>" + SEP +
	"  <entry name=\"user\">" + SEP +
	"    <entry name=\"username\" value=\"fred\"/>" + SEP +
	"  </entry>" + SEP +
	"  <entry name=\"server\" value=\"MyServer\">" + SEP +
	"    <entry name=\"gigahertz\" value=\"3.0\"/>" + SEP +
	"  </entry>" + SEP +
	"</entry>" + SEP;

	
	
/*	
	String configString = 
		"<entry name=\"root\">" + SEP +
		"<entry name=\"admin\" value=\"Sam\"/>" + SEP +
		"<entry name=\"timeout\" value=\"60\"/>" + SEP +
		"<entry name=\"user\">" + SEP +
		"<entry name=\"username\" value=\"bob\"/>" + SEP +
		"</entry>" + SEP +
		"<entry name=\"user\">" + SEP +
		"<entry name=\"username\" value=\"fred\"/>" + SEP +
		"</entry>" + SEP +
		"<entry name=\"server\" value=\"MyServer\">" + SEP +
		"<entry name=\"gigahertz\" value=\"3.0\"/>" + SEP +
		"</entry>" + SEP +
		"</entry>" + SEP;
*/
}
