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
package org.openpipeline.pipeline.connector.webcrawler;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * An extension of the implementation of LinkQueue which stores information
 * about crawled domains in a MySQL database.
 */
public class DomainDB implements LinkQueue {

	private static final String DEFAULT_JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String DEFAULT_DATABASE = "linkqueue_domainDB";
	private static final String DEFAULT_TABLE = "linkqueue_domainDB";
	private static final String DEFAULT_USERNAME = "root";
	private static final String DEFAULT_PASSWORD = "root";
	private static final String DEFAULT_DATABASE_URL = "jdbc:derby:domainDB;create=true";

	private Connection connection;
	protected Logger logger;
	protected boolean debug;

	/* prepared statements */
	protected PreparedStatement fetchNextUncrawledStatement;
	private PreparedStatement removeStatement;
	protected PreparedStatement updateStatement;
	private PreparedStatement insertStatement;
	private PreparedStatement getLastCrawlStatement;
	protected String dbTable;

	private XMLConfig params;

	/**
	 * Creates a database or opens the connection to an existing database
	 */
	public void initialize() {
		logger = Server.getServer().getLogger();
		debug = Server.getServer().getDebug();

		if (params == null) {
			throw new IllegalArgumentException("params cannot be null.");
		}

		try {
			System.setProperty("derby.system.home", Server.getServer()
					.getHomeDir() + "/derby");

			String user = params.getProperty("username", DEFAULT_USERNAME);
			String password = params.getProperty("password", DEFAULT_PASSWORD);
			String database = params.getProperty("database", DEFAULT_DATABASE);
			String jdbcDriver = params.getProperty("jdbc-driver",
					DEFAULT_JDBC_DRIVER);
			String table = params.getProperty("table", DEFAULT_TABLE);

			if (!database.equals(DEFAULT_DATABASE)) {
				database += "_domainDB";
			}

			if (!table.equals(DEFAULT_DATABASE)) {
				table += "_domainDB";
			}

			String url = getDatabaseUrl(database);

			dbTable = database + "." + table;

			Class.forName(jdbcDriver);
			connection = DriverManager.getConnection(url, user, password);

			String createTableSQL = "CREATE TABLE " + dbTable + " ("
					+ "domain VARCHAR(1000) NOT NULL,"
					+ "lastcrawl BIGINT DEFAULT 0," + "PRIMARY KEY (domain))";
			// TODO should it use lastcrawl as long? now it is a string
			String createIndexSQL = "CREATE INDEX lastcrawl_" + table + " ON "
					+ dbTable + "(lastcrawl)";

			createTable(database, table, createTableSQL, createIndexSQL);

			if (true) {
				logger.debug("Created linkQueue: " + dbTable);
			}

			// this is just plain ugly, but probably necessary
			fetchNextUncrawledStatement = getPreparedStatement("select * from "
					+ dbTable + " where lastcrawl < ?");

			removeStatement = getPreparedStatement("delete from " + dbTable
					+ " where domain=?");

			updateStatement = getPreparedStatement("update " + dbTable
					+ " set lastcrawl=? where domain=?");

			insertStatement = getPreparedStatement("insert into " + dbTable
					+ " (domain,lastcrawl)" + " values (?,?)");

			getLastCrawlStatement = getPreparedStatement("select lastcrawl from "
					+ dbTable + " where domain=?");

		} catch (Exception e) {
			// not recoverable
			logger.error("Error initializing WebCrawlerLinkDB", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches the next domain to crawl from the linkQueue.
	 * 
	 * @param beforeTimestamp
	 *            containing the time stamp so that only URLs with time stamp
	 *            before this are returned
	 * @return LinkDBRecord containing the record of the next URL
	 * 
	 * @throws RuntimeException
	 */
	public String fetchNextUncrawled(long beforeTimestamp) {

		String domain = null;
		ResultSet rs = null;

		try {

			fetchNextUncrawledStatement.setLong(1, beforeTimestamp);
			rs = fetchNextUncrawledStatement.executeQuery();

			if (rs != null && rs.next()) {

				domain = rs.getString("domain");

				/*
				 * Update: Set the last crawl time to beforeTimestamp so that
				 * this record is not fetched again
				 * 
				 * This prevents returning an id that is already in process
				 */
				updateStatement.setLong(1, Long.MAX_VALUE);
				updateStatement.setString(2, domain);
				updateStatement.executeUpdate();

				if (debug) {
					logger.debug("WebCrawlerDomainDB found uncrawled item:"
							+ domain);
				}
				return domain;
			}
			return null;

		} catch (SQLException e) {
			throw new RuntimeException(e);

		} finally {
			closeResultSet(rs);
		}
	}

	protected void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void remove(String domain) {
		try {

			removeStatement.setString(1, domain);
			removeStatement.executeUpdate();

			if (debug) {
				logger.debug("WebCrawlerLinkDB : " + dbTable
						+ " removing domain:" + domain);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Inserts a domain record in the WebCrawlerLinkDB, creates a new record if
	 * needed. Only if the requested domain is not flagged as in progress.
	 * 
	 * @param domain
	 *            containing the domain name
	 * @param timeStamp
	 *            containing the new time stamp
	 * @throws RuntimeException
	 */
	public void addNew(String domain, long timeStamp) {
		/* Check if this domain in progress now */
		long currentLastCrawl = getLastCrawl(domain);
		if (currentLastCrawl == Long.MAX_VALUE) {
			return;
		}
		update(domain, timeStamp);

	}

	/**
	 * Updates the domain record in the WebCrawlerLinkDB, creates a new record
	 * if needed.
	 * 
	 * @param domain
	 *            containing the domain name
	 * @param timeStamp
	 *            containing the new time stamp
	 * @throws RuntimeException
	 */
	public void update(String domain, long timeStamp) {
		try {

			updateStatement.setLong(1, timeStamp);
			updateStatement.setString(2, domain);
			/*
			 * The update statement looks only at IDs with lastCrawlTime ==
			 * startOfCrawl, i.e. not yet finalized during the current crawl
			 */
			int rowsAffected = updateStatement.executeUpdate();

			if (rowsAffected == 0) {
				insertStatement.setString(1, domain);
				insertStatement.setLong(2, timeStamp);
				insertStatement.executeUpdate();

				if (debug) {
					logger.debug("WebCrawlerLinkDB : " + dbTable
							+ " Inserted new item ID:" + domain);
				}
			} else if (debug) {
				logger.debug("WebCrawlerLinkDB : " + dbTable
						+ " Updated item ID:" + domain);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the timestamp of the last time a domain was crawled, or -1 if the
	 * domain is not found.
	 * 
	 * @param domain
	 *            containing the domain name
	 * @return long containing the timestamp
	 * 
	 */
	public long getLastCrawl(String domain) {

		long lastCrawl = 0;
		ResultSet rs = null;

		try {
			getLastCrawlStatement.setString(1, domain);
			rs = getLastCrawlStatement.executeQuery();
			if (rs != null && rs.next()) {
				lastCrawl = rs.getLong("lastcrawl");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			closeResultSet(rs);
		}
		return lastCrawl;
	}

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				// not recoverable
				logger.error("Error initializing WebCrawlerLinkDB", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void createTable(String database, String table,
			String createTableSQL, String createIndexSQL) throws SQLException {

		DatabaseMetaData dmd = connection.getMetaData();

		ResultSet rs = dmd.getTables(null, database.toUpperCase(),
				table.toUpperCase(), null);

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

	protected PreparedStatement getPreparedStatement(String sql)
			throws SQLException {
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

	public void setParams(XMLConfig params) {
		this.params = params;
	}

	@Deprecated
	public void update(String id, long signature, long lastCrawl) {

		throw new RuntimeException("Method not implemented.");
	}

	protected void write(Logger logger) throws Exception {

		PreparedStatement selectAllStatement = getPreparedStatement("select * from "
				+ dbTable);
		ResultSet rs = selectAllStatement.executeQuery();
		if (rs == null)
			return;
		while (rs.next()) {

			String domain = rs.getString("domain");
			long lastCrawlTime = rs.getLong("lastcrawl");

			logger.info("Domain: " + domain + "\t lastCrawlTime: "
					+ lastCrawlTime);
		}
	}

	private String getDatabaseUrl(String database) {
		String url = params.getProperty("database-url", DEFAULT_DATABASE_URL);

		String[] parts = url.split(";");

		if (parts.length != 2) {
			return DEFAULT_DATABASE_URL;
		}

		String[] db = parts[0].split(":");

		if (db.length != 3) {
			return DEFAULT_DATABASE_URL;
		}

		return db[0] + ":" + db[1] + ":" + database + ";" + parts[1];
	}

	public String getName() {
		return dbTable;
	}

	@Deprecated
	public long getSignature(String id) {
		throw new RuntimeException("Method not implemented.");
	}
}
