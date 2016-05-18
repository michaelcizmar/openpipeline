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
package org.openpipeline.pipeline.connector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import junit.framework.TestCase;

import org.openpipeline.util.FileUtil;
import org.openpipeline.util.XMLConfig;

public class TestSQLCrawler extends TestCase {
	
	/**
	 * Creates table, does inserts, deletes, recrawls, checks counts at each stage.
	 */
	public void test() throws Exception {

		String dir = "/temp/sqlcrawlertest";
		FileUtil.deleteDir(dir);
		
		System.setProperty("app.home", dir);
		System.setProperty("derby.system.home", dir);
		
		String jdbcDriver = "org.apache.derby.jdbc.EmbeddedDriver";
		String jdbcURL = "jdbc:derby:testdb;create=true";
		String jdbcUser = "root";
		
		Class.forName(jdbcDriver);
		Connection con = DriverManager.getConnection(jdbcURL, jdbcUser, null);
		
		// create, populate the db
		execSQL(con, "create table foo (id varchar(10), bar varchar(10), PRIMARY KEY (id))");
		execSQL(con, "insert into foo (id, bar) values ('a', 'val')");
		execSQL(con, "insert into foo (id, bar) values ('b', 'val')");
		execSQL(con, "insert into foo (id, bar) values ('c', 'val')");

		
		SQLDatabaseCrawler crawler = new SQLDatabaseCrawler();
		
		XMLConfig params = new XMLConfig();
		params.setProperty("jdbc-driver", jdbcDriver);
		params.setProperty("jdbc-url", jdbcURL);
		params.setProperty("jdbc-user", jdbcUser);

		params.setProperty("linkqueue-name", "DerbyLinkQueue");
		
		params.setProperty("index-sql", "select * from foo");
		//params.setProperty("after-sql", "");

		params.setProperty("item_id", "ID");
		
		// linkqueue stuff
		params.setProperty("database-url", "jdbc:derby:linkqueue;create=true");
		params.setProperty("username", "root");
		//params.setProperty("password", value);  // use defaults
		//params.setProperty("database", value);
		//params.setProperty("table", value);		
		
		// initial population, 3 adds
		crawler.setParams(params);
		crawler.execute(); 
		testCounts(crawler, 3, 0, 0);

		// second run, 3 unchanged
		crawler.setParams(params);
		crawler.execute(); 
		testCounts(crawler, 0, 0, 3);

		// third run, 1 add (d) 1 delete (b) 2 unchanged (a & c)
		execSQL(con, "insert into foo (id, bar) values ('d', 'val')");
		execSQL(con, "delete from foo where id='b'");
		crawler.setParams(params);
		crawler.execute(); 
		testCounts(crawler, 1, 1, 2);

		con.close();
	}

	
	
	private void execSQL(Connection con, String sql) throws Exception {
		Statement state = con.createStatement();
		state.executeUpdate(sql);
	}



	private void testCounts(SQLDatabaseCrawler crawler, int itemsAdded, int itemsDeleted, int itemsUnchanged) {
		if (crawler.getItemsAdded() != itemsAdded) {
			fail();
		}
		if (crawler.getItemsDeleted() != itemsDeleted) {
			fail();
		}
		if (crawler.getItemsUnchanged() != itemsUnchanged) {
			fail();
		}
		if (!crawler.getLastMessage().equals("Ended")) {
			System.out.println(crawler.getLastMessage());
			fail();
		}
	}

}
