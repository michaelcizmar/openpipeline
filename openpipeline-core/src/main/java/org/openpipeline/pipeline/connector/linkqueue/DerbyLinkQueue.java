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
package org.openpipeline.pipeline.connector.linkqueue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * An implementation of LinkQueue which stores information in MySQL.
 */
public class DerbyLinkQueue implements LinkQueue {
	private static final String JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String DEFAULT_DATABASE = "linkqueue";
	private static final String DEFAULT_TABLE = "linkqueue";
	
	private Connection connection;
	private Logger logger;
	private boolean debug;

	// prepared statements
	private PreparedStatement fetchNextUncrawledStatement;
	private PreparedStatement getSignatureStatement;
	private PreparedStatement removeStatement;
	private PreparedStatement updateStatement;
	private PreparedStatement insertStatement;

	// contains the ids of the items that are currently in process
	private HashMap inProcessMap = new HashMap();

	private void init(XMLConfig params) {
		logger = Server.getServer().getLogger();
		debug = Server.getServer().getDebug();

		if (params == null) {
			throw new IllegalArgumentException("params cannot be null.");
		}

		try {
			System.setProperty("derby.system.home", Server.getServer().getHomeDir() + "/derby");
			
			String url = params.getProperty("database-url");
			String user = params.getProperty("username");
			String password = params.getProperty("password");
			String database = params.getProperty("database", DEFAULT_DATABASE);
			String table = params.getProperty("table", DEFAULT_TABLE);

			String dbTable = database + "." + table;

			Class.forName(JDBC_DRIVER);
			connection = DriverManager.getConnection(url, user, password);
			
			String createTableSQL = 
				"CREATE TABLE " + dbTable + " (" + 
				"id VARCHAR(1000) NOT NULL," + 
				"signature BIGINT DEFAULT 0," + 
				"lastcrawl BIGINT DEFAULT 0," + 
				"PRIMARY KEY (id))";
			
			String createIndexSQL = "CREATE INDEX lastcrawl_" + table + " ON " + dbTable + "(lastcrawl)";
			
			createTable(database, table, createTableSQL, createIndexSQL);
			
			// this is just plain ugly, but probably necessary
			fetchNextUncrawledStatement = getPreparedStatement("select id from " + dbTable
					+ " where lastcrawl < ?");

			getSignatureStatement = getPreparedStatement("select signature from " + dbTable + " where id=?");
			
			removeStatement = getPreparedStatement("delete from " + dbTable + " where id=?");

			updateStatement = getPreparedStatement("update " + dbTable + " set signature=?, lastcrawl=? where id=?");

			insertStatement = getPreparedStatement("insert into " + dbTable + " (id, signature, lastcrawl)"
					+ " values (?,?,?)");

		} catch (Exception e) {
			// not recoverable
			logger.error("Error initializing LinkQueue", e);
			throw new RuntimeException(e);
		}
	}

	public String fetchNextUncrawled(long beforeTimestamp) {
		String id = null;
		ResultSet rs = null;

		try {

			fetchNextUncrawledStatement.setLong(1, beforeTimestamp);
			rs = fetchNextUncrawledStatement.executeQuery();
			while (rs != null && rs.next()) {
				id = rs.getString("id");

				// this prevents returning an id that is already in process
				synchronized (this) {
					if (inProcessMap.get(id) == null) {
						inProcessMap.put(id, id);
						break;
					}
				}
			}
			if (debug) {
				if (id == null) {
					logger.debug("No uncrawled items found in linkqueue");
				} else {
					logger.debug("LinkQueue found uncrawled item:" + id);
				}
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			closeResultSet(rs);
		}

		return id;
	}

	private void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public long getSignature(String id) {
		long signature = -1;
		ResultSet rs = null;

		try {
			getSignatureStatement.setString(1, id);
			rs = getSignatureStatement.executeQuery();
			if (rs != null && rs.next()) {
				signature = rs.getLong("signature");
			}
			if (debug) {
				logger.debug("LinkQueue: Returning signature for item id:" + id + ", signature:" + signature);
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			closeResultSet(rs);
		}

		return signature;
	}

	public void remove(String id) {
		try {
			removeStatement.setString(1, id);
			removeStatement.executeUpdate();
			inProcessMap.remove(id);
			if (debug) {
				logger.debug("LinkQueue: removing id:" + id);
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(String id, long signature, long lastCrawl) {

		try {
			
			// no need to synchronize; only one thread should hit a given id
			updateStatement.setLong(1, signature);
			updateStatement.setLong(2, lastCrawl);
			updateStatement.setString(3, id);
			
			int rowsAffected = updateStatement.executeUpdate();
			if (rowsAffected == 0) {
				insertStatement.setString(1, id);
				insertStatement.setLong(2, signature);
				insertStatement.setLong(3, lastCrawl);
				insertStatement.executeUpdate();
			}

			inProcessMap.remove(id);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				// not recoverable
				logger.error("Error initializing LinkQueue", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void createTable(String database, String table, String createTableSQL, String createIndexSQL)
			throws SQLException {
		DatabaseMetaData dmd = connection.getMetaData();
		ResultSet rs = dmd.getTables(null, database.toUpperCase(), table.toUpperCase(), null);
		if (!rs.next()) {
			// if the table doesn't exist
			execQuery(createTableSQL);
			execQuery(createIndexSQL);
		}
		rs.close();
	}
	
	
	
	private void execQuery(String sql) throws SQLException {
		Statement state = connection.createStatement();
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			SQLException se = new SQLException(e.toString() + " " + sql);
			se.initCause(e);
			throw se;
		} finally {
			state.close();
		}
	}
	
	private PreparedStatement getPreparedStatement(String sql) throws SQLException {
		try {
			return connection.prepareStatement(sql);
		} catch (SQLException e) {
			SQLException se = new SQLException(e.toString() + " " + sql);
			se.initCause(e);
			throw se;
		}
	}
	
	

	public String getDescription() {
		return "Stores data in Derby";
	}

	public String getName() {
		return "DerbyLinkQueue";
	}

	public void setParams(XMLConfig params) {
		init(params);
	}
}
