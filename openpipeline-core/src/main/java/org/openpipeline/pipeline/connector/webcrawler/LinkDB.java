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
import java.util.LinkedList;

import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * An extension of the implementation of LinkQueue which stores information in a
 * MySQL database.
 */
public class LinkDB implements LinkQueue {

	public static final String DEFAULT_JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String DEFAULT_DATABASE = "linkqueue_linkDB";
	public static final String DEFAULT_TABLE = "linkqueue_linkDB";
	public static final String DEFAULT_USERNAME = "root";
	public static final String DEFAULT_PASSWORD = "root";
	public static final String DEFAULT_DATABASE_URL = "jdbc:derby:linkDB;create=true";

	private Connection connection;
	protected Logger logger;
	protected boolean debug;

	/* prepared statements */
	protected PreparedStatement fetchNextUncrawledStatement;
	private PreparedStatement fetchNextUncrawledFromDomainStatement;
	private PreparedStatement getSignatureStatement;
	private PreparedStatement getLastCrawlStatement;
	private PreparedStatement removeStatement;
	protected PreparedStatement updateStatement;
	private PreparedStatement insertStatement;
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
					.getHomeDir()
					+ "/derby");

			String user = params.getProperty("username", DEFAULT_USERNAME);
			String password = params.getProperty("password", DEFAULT_PASSWORD);
			String database = params.getProperty("database", DEFAULT_DATABASE);
			String jdbcDriver = params.getProperty("jdbc-driver",
					DEFAULT_JDBC_DRIVER);
			String table = params.getProperty("table", DEFAULT_TABLE);

			if (!database.equals(DEFAULT_DATABASE)) {
				database += "_linkDB";
			}

			if (!table.equals(DEFAULT_DATABASE)) {
				table += "_linkDB";
			}

			String url = getDatabaseUrl(database);

			// table = database;
			dbTable = database + "." + table;

			Class.forName(jdbcDriver);
			connection = DriverManager.getConnection(url, user, password);

			String createTableSQL = "CREATE TABLE " + dbTable + " ("
					+ "id VARCHAR(1000) NOT NULL,"
					+ "signature BIGINT DEFAULT 0,"
					+ "lastcrawl BIGINT DEFAULT 0,"
					+ "linkdepth BIGINT DEFAULT 0," + "domain VARCHAR(1000),"
					+ "PRIMARY KEY (id))";
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

			fetchNextUncrawledFromDomainStatement = getPreparedStatement("select * from "
					+ dbTable + " where lastcrawl < ? AND domain=?");

			getSignatureStatement = getPreparedStatement("select signature from "
					+ dbTable + " where id=?");

			getLastCrawlStatement = getPreparedStatement("select lastcrawl from "
					+ dbTable + " where id=?");

			removeStatement = getPreparedStatement("delete from " + dbTable
					+ " where id=?");

			updateStatement = getPreparedStatement("update "
					+ dbTable
					+ " set signature=?, lastcrawl=?, linkdepth=?, domain=? where id=?");

			insertStatement = getPreparedStatement("insert into " + dbTable
					+ " (id, signature, lastcrawl, linkdepth,domain)"
					+ " values (?,?,?,?,?)");

		} catch (Exception e) {
			// not recoverable
			logger.error("Error initializing WebCrawlerLinkDB", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches the next URL to crawl from the linkQueue.
	 * 
	 * @param beforeTimestamp
	 *            containing the time stamp so that only URLs with time stamp
	 *            before this are returned
	 * @return String containing the record of the next URL
	 * 
	 * @throws RuntimeException
	 */
	public String fetchNextUncrawled(long beforeTimestamp) {
		LinkDBRecord nextRecord = fetchNextUncrawledURL(beforeTimestamp);
		return nextRecord.getNextUrl();
	}

	/**
	 * Fetches the next URL to crawl from the linkQueue.
	 * 
	 * @param beforeTimestamp
	 *            containing the time stamp so that only URLs with time stamp
	 *            before this are returned
	 * @return LinkDBRecord containing the record of the next URL
	 * 
	 * @throws RuntimeException
	 */
	public LinkDBRecord fetchNextUncrawledURL(long beforeTimestamp) {

		String id = null;
		ResultSet rs = null;

		try {

			fetchNextUncrawledStatement.setLong(1, beforeTimestamp);
			rs = fetchNextUncrawledStatement.executeQuery();

			if (rs != null && rs.next()) {

				id = rs.getString("id");

				/*
				 * Update: Set the last crawl time to beforeTimestamp so that
				 * this record is not fetched again. The only update after this
				 * would be finalize or delete.
				 * 
				 * This prevents returning an id that is already in process and
				 * updating it when the same id is extracted from websites as a
				 * unseen URL.
				 */
				LinkDBRecord nextRecord = new LinkDBRecord(id);
				nextRecord.setValues(rs);

				updateStatement.setLong(1, nextRecord.getSignature());
				updateStatement.setLong(2, beforeTimestamp);
				updateStatement.setLong(3, nextRecord.getLinkDepth());
				updateStatement.setString(4, nextRecord.getDomain());
				updateStatement.setString(5, id);
				updateStatement.executeUpdate();

				if (debug) {
					logger.debug("WebCrawlerLinkDB found uncrawled item:" + id);
				}

				return nextRecord;
			}

			if (debug) {
				logger.debug("No uncrawled items found in WebCrawlerLinkDB");
			}
			return null;

		} catch (Throwable e) {
			throw new RuntimeException(e);

		} finally {
			closeResultSet(rs);
		}
	}

	/**
	 * Fetches the next N URLs to crawl from the WebCrawlerLinkDB.
	 * 
	 * @param domain
	 *            containing the name of the domain from which URLs are
	 *            requested
	 * 
	 * @param beforeTimestamp
	 *            containing the time stamp so that only URLs with time stamp
	 *            before this are returned
	 * @param N
	 *            containing the number of URLs to fetch
	 * @return LinkedList containing the LinkDBRecord of the next N URL
	 * 
	 * @throws RuntimeException
	 */
	public LinkedList<LinkDBRecord> fetchUncrawledURLs(String domain,
			long beforeTimestamp, int N) {

		String id = null;
		ResultSet rs = null;
		int size = 0;

		LinkedList<LinkDBRecord> urls = new LinkedList<LinkDBRecord>();

		try {

			fetchNextUncrawledFromDomainStatement.setLong(1, beforeTimestamp);
			fetchNextUncrawledFromDomainStatement.setString(2, domain);
			rs = fetchNextUncrawledFromDomainStatement.executeQuery();

			while (rs != null && rs.next() && size < N) {

				id = rs.getString("id");

				/*
				 * Update: Set the last crawl time to beforeTimestamp so that
				 * this record is not fetched again
				 * 
				 * This prevents returning an id that is already in process
				 */
				LinkDBRecord nextRecord = new LinkDBRecord(id);
				nextRecord.setValues(rs);

				updateStatement.setLong(1, nextRecord.getSignature());
				updateStatement.setLong(2, beforeTimestamp);// TODO
				updateStatement.setLong(3, nextRecord.getLinkDepth());
				updateStatement.setString(4, nextRecord.getDomain());
				updateStatement.setString(5, id);
				updateStatement.executeUpdate();

				urls.add(nextRecord);
				size++;

				if (debug) {
					logger.debug("WebCrawlerLinkDB found uncrawled item:" + id);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);

		} finally {

			closeResultSet(rs);
		}

		return urls;
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

	public long getSignature(String id) {
		long signature = -1;
		ResultSet rs = null;

		try {
			getSignatureStatement.setString(1, id);
			rs = getSignatureStatement.executeQuery();
			if (rs != null && rs.next()) {
				signature = rs.getLong("signature");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			closeResultSet(rs);
		}
		return signature;
	}

	/**
	 * Get the timestamp of the last time the item was crawled, or -1 if the id
	 * is not found.
	 * 
	 * @param id
	 *            containing the item id
	 * @return long containing the timestamp
	 * 
	 */
	public long getLastCrawl(String id) {

		long lastCrawl = 0;
		ResultSet rs = null;

		try {
			getLastCrawlStatement.setString(1, id);
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

	public void remove(String id) {
		try {

			removeStatement.setString(1, id);
			removeStatement.executeUpdate();

			if (debug) {
				logger.debug("WebCrawlerLinkDB : " + dbTable + " removing id:"
						+ id);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Updates the URL record in the WebCrawlerLinkDB, creates a new record if
	 * needed.
	 * 
	 * @param linkDBRecord
	 *            containing the URL information
	 * @throws RuntimeException
	 */
	public void update(LinkDBRecord linkDBRecord) {

		/* The update statement checks the last crawl time. */
		try {

			String id = linkDBRecord.getNextUrl();
			String domain = linkDBRecord.getDomain();
			long signature = linkDBRecord.getSignature();
			long lastCrawl = linkDBRecord.getLastCrawlTime();
			long linkDepth = linkDBRecord.getLinkDepth();

			long currentLastCrawl = getLastCrawl(id);
			if (currentLastCrawl > lastCrawl) {
				// Avoid updating records that are in process or finished. This
				// can happen if a URL in progress is also extracted as a new
				// URL and is not being added to the queue.
				if (debug) {
					logger.debug("WebCrawlerLinkDB : "
							+ " No update for the finalized item ID:" + id
							+ " signature = " + signature
							+ " new crawl time = " + lastCrawl
							+ " current crawl time = " + currentLastCrawl);
				}
				return;
			}

			// no need to synchronize; only one thread should hit a given id
			updateStatement.setLong(1, signature);
			updateStatement.setLong(2, lastCrawl);
			updateStatement.setLong(3, linkDepth);
			updateStatement.setString(4, domain);
			updateStatement.setString(5, id);
			/*
			 * The update statement looks only at IDs with lastCrawlTime ==
			 * startOfCrawl, i.e. not yet finalized during the current crawl
			 */
			int rowsAffected = updateStatement.executeUpdate();

			if (rowsAffected == 0) {

				insertStatement.setString(1, id);
				insertStatement.setLong(2, signature);
				insertStatement.setLong(3, lastCrawl);
				insertStatement.setLong(4, linkDepth);
				insertStatement.setString(5, domain);
				insertStatement.executeUpdate();

				if (debug) {
					logger.debug("WebCrawlerLinkDB : "
							+ " Inserted new item ID:" + id + " signature = "
							+ signature + " last crawl time = " + lastCrawl);
				}
			} else if (debug) {
				logger.debug("WebCrawlerLinkDB : " + " Updated item ID:" + id
						+ " signature = " + signature + " last crawl time = "
						+ lastCrawl);
			}
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
				logger.error("Error initializing WebCrawlerLinkDB", e);
				throw new RuntimeException(e);
			}
		}
	}

	private void createTable(String database, String table,
			String createTableSQL, String createIndexSQL) throws SQLException {

		DatabaseMetaData dmd = connection.getMetaData();
		ResultSet rs = dmd.getTables(null, database.toUpperCase(), table
				.toUpperCase(), null);
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

	@Deprecated
	public void update(String id, long signature, long lastCrawl) {

		try {
			LinkDBRecord nextRecord = new LinkDBRecord(id);
			nextRecord.setLastCrawlTime(lastCrawl);
			nextRecord.setSignature(signature);

			update(nextRecord);

		} catch (Throwable e) {
			/* do nothing */
		}
	}

	protected void write(Logger logger) throws Exception {

		PreparedStatement selectAllStatement = getPreparedStatement("select * from "
				+ dbTable);
		ResultSet rs = selectAllStatement.executeQuery();
		if (rs == null)
			return;
		while (rs.next()) {
			String nextUrl = rs.getString("id");
			String domain = rs.getString("domain");
			long lastCrawlTime = rs.getLong("lastcrawl");
			long signature = rs.getLong("signature");
			long linkDepth = rs.getLong("linkdepth");

			logger.info("Id: " + nextUrl + "\t lastCrawlTime: " + lastCrawlTime
					+ "\t signature: " + signature + "\t linkDepth: "
					+ linkDepth + "\t domain: " + domain);
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

	public void setParams(XMLConfig params) {
		this.params = params;
	}
}
