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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.zip.Adler32;

import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueueFactory;
import org.openpipeline.pipeline.item.AttributeDef;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.pipeline.stage.StageList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.Util;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Crawls SQL databases.
 */
public class SQLDatabaseCrawler extends Connector {

	private int recsProcessed;
	private Item item = new Item();
	private Logger logger;
	private LinkQueue linkQueue;
	private long startOfCrawl;

	private int itemsAdded;
	private int itemsDeleted;
	private int itemsUnchanged;
	private Adler32 adler32 = new Adler32();

	private PreparedStatement indexStatement = null;
	private String driver;
	private String url;
	private String user;
	private String password;
	private String beforeSQL;
	private String indexSQL;
	private String afterSQL;
	private String itemIdCol;
	private int fetchSize = 1000;
	private String lastPrimaryKey = "";
	private String primaryKeyCol;
	private String[] cols;
	private int itemIdColNum;
	private int primaryKeyColNum;
	private boolean inferDataTypes;

	public void execute() {

		startOfCrawl = System.currentTimeMillis();
		Connection con = null;
		ResultSet rs = null;

		itemsAdded = 0;
		itemsDeleted = 0;
		itemsUnchanged = 0;

		DONE: try {

			extractParams();

			logger = super.getLogger();
			logger.info("Starting " + super.getJobName() + "...");
			super.setLastMessage("Running");

			Class.forName(driver);
			con = DriverManager.getConnection(url, user, password);

			if (super.getInterrupted()) {
				break DONE;
			}

			if (beforeSQL != null) {
				logger.info("Processing before SQL:" + beforeSQL);
				Statement state = con.createStatement();
				try {
					state.executeUpdate(beforeSQL);
				} finally {
					state.close();
				}
			}

			if (super.getInterrupted()) {
				break DONE;
			}

			indexStatement = con.prepareStatement(indexSQL,
					java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			indexStatement.setFetchSize(fetchSize);

			logger.info("Processing index SQL:" + indexSQL);

			int totalRowCount = 0;
			while (true) {
				int rowCount = runQuery();
				if (rowCount == 0) {
					break;
				}

				totalRowCount += rowCount;
				super.setLastMessage("Total rows processed: " + totalRowCount);

				if (super.getInterrupted()) {
					break DONE;
				}
			}

			if (afterSQL != null) {
				logger.info("Processing after SQL:" + afterSQL);
				Statement state = con.createStatement();
				try {
					state.executeUpdate(afterSQL);
				} finally {
					state.close();
				}
			}

			itemsDeleted = lookForDeletes(super.getStageList());

			String elapsedStr = Util.getFormattedElapsedTime(System
					.currentTimeMillis() - startOfCrawl);
			String msg = "SQLCrawler ended. Items added: " + itemsAdded
					+ " deleted: " + itemsDeleted + " unchanged: "
					+ itemsUnchanged + " Elapsed time: " + elapsedStr;

			logger.info(msg);
			super.setLastMessage(msg);

		} catch (Exception e) {
			super.error("Error executing SQLDatabaseCrawler", e);
			super.setLastMessage("Error: " + e.toString());

		} finally {

			try {
				if (rs != null) {
					rs.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * Extract all of the parameters from the params object.
	 */
	private void extractParams() {

		XMLConfig params = super.getParams();
		if (params == null) {
			String msg = "setParams() was not called for SQLCrawler.";
			throw new IllegalStateException(msg);
		}

		String linkQueueName = params.getProperty("linkqueue-name");
		linkQueue = LinkQueueFactory.getLinkQueueByName(linkQueueName);
		if (linkQueue != null) {
			linkQueue.setParams(params); // inits the queue, could throw error
		}

		driver = params.getProperty("jdbc-driver");
		url = params.getProperty("jdbc-url");
		user = params.getProperty("jdbc-user");
		password = params.getProperty("jdbc-password");

		beforeSQL = params.getProperty("before-sql");
		indexSQL = params.getProperty("index-sql");
		afterSQL = params.getProperty("after-sql");

		itemIdCol = params.getProperty("itemid-col");
		primaryKeyCol = params.getProperty("primary-key-col");

		fetchSize = params.getIntProperty("fetch-size");

		inferDataTypes = params.getBooleanProperty("infer-data-types", true);
	}

	/**
	 * Fetch one page of results and process it.
	 * 
	 * @return
	 * @throws SQLException
	 * @throws PipelineException
	 */
	private int runQuery() throws SQLException, PipelineException {

		logger.info("Running index SQL for primary key [" + primaryKeyCol
				+ "] > " + lastPrimaryKey);
		ResultSetMetaData meta = indexStatement.getMetaData();
		if (meta != null) {
			if (cols == null) {
				populateColumns(meta, item);
			}

			String dataType = (String) item.getAttributeDefs().get(primaryKeyCol)
					.getPropertiesMap().get("datatype");
			if ("N".equals(dataType)) {
				indexStatement
						.setInt(1, Integer.getInteger(lastPrimaryKey, 0));
			} else {
				indexStatement.setString(1, lastPrimaryKey);
			}
		} else {
			indexStatement.setString(1, lastPrimaryKey);
		}
		ResultSet rs = indexStatement.executeQuery();

		if (cols == null) {
			populateColumns(rs.getMetaData(), item);
		}

		int colCount = rs.getMetaData().getColumnCount();
		String primaryKey = "";
		String itemId = "";
		int rowCount = 0;

		while (rs.next()) {

			rowCount++;

			if (item.hasAttributeDefs()) {
				Map defs = item.getAttributeDefs();
				item.clear();
				item.setAttributeDefs(defs);
			} else {
				item.clear();
			}

			adler32.reset();

			for (int i = 1; i <= colCount; i++) {
				String value = rs.getString(i);

				if (value == null) {
					continue;
				}
				value = value.trim();
				if (value.length() == 0) {
					continue;
				}

				item.getRootNode().addNode(cols[i - 1], value);

				if (i == itemIdColNum) {
					itemId = value;
				}
				if (i == primaryKeyColNum) {
					primaryKey = value;
				}

				updateChecksum(value);
			}

			if (hasChanged(itemId, adler32.getValue())) {
				item.setItemId(itemId);

				try {
					super.getStageList().processItem(item);
					itemsAdded++;
				} catch (PipelineException e) {
					super.error("Error processing item " + itemId
							+ ". Message = " + e.getMessage());
				}

			} else {
				itemsUnchanged++;
			}

			if (super.getInterrupted()) {
				break;
			}
		}

		lastPrimaryKey = primaryKey;

		rs.close();

		logger.info("Rows processed: " + rowCount);

		return rowCount;
	}

	/**
	 * Discover the columns in the result set;
	 * 
	 * @throws SQLException
	 * @throws PipelineException
	 */
	private void populateColumns(ResultSetMetaData meta, Item item)
			throws SQLException, PipelineException {

		int colCount = meta.getColumnCount();
		cols = new String[colCount];

		for (int i = 0; i < cols.length; i++) {
			String name = meta.getColumnName(i + 1);
			cols[i] = name;

			if (inferDataTypes) {
				String dataType = "S"; // defaults to string

				int type = meta.getColumnType(i + 1);
				switch (type) {
				case java.sql.Types.BIGINT:
				case java.sql.Types.DECIMAL:
				case java.sql.Types.DOUBLE:
				case java.sql.Types.FLOAT:
				case java.sql.Types.INTEGER:
				case java.sql.Types.NUMERIC:
				case java.sql.Types.REAL:
				case java.sql.Types.SMALLINT:
				case java.sql.Types.TINYINT:
					dataType = "N"; // numeric
					break;

				case java.sql.Types.DATE:
				case java.sql.Types.TIME:
				case java.sql.Types.TIMESTAMP:
					dataType = "D"; // date
					break;
				default:
				}

				AttributeDef def = new AttributeDef();
				def.setAttributeId(name);
				def.put("datatype", dataType);

				item.getAttributeDefs().put(name, def);
			}
		}

		// find the number of the column to use for the itemId
		for (int i = 0; i < colCount; i++) {
			if (cols[i].equals(itemIdCol)) {
				itemIdColNum = i + 1; // add one for jdbc
			}
		}
		if (itemIdColNum == 0) {
			throw new PipelineException(
					"The column to use for the itemId was not found in the result set: "
							+ itemIdCol + " cols:" + cols.toString());
		}

		// find the number of the column to use for the primaryKey
		for (int i = 0; i < colCount; i++) {
			if (cols[i].equals(primaryKeyCol)) {
				primaryKeyColNum = i + 1; // add one for jdbc
			}
		}
		if (itemIdColNum == 0) {
			throw new PipelineException(
					"The column to use as the primary key was not found in the result set: "
							+ primaryKeyCol + " cols:" + cols.toString());
		}
	}

	/**
	 * Calculates a checksum across the record. This method accepts each field
	 * and updates the Adler32 value. (A 64 bit value would be better).
	 */
	private void updateChecksum(String value) {
		int len = value.length();
		for (int i = 0; i < len; i++) {
			char ch = value.charAt(i);
			adler32.update((ch >> 8) & 0xff);
			adler32.update(ch & 0xff);
		}
	}

	/**
	 * Return true if the name was found in the linkqueue and the signature has
	 * changed. Also updates the queue with the current startOfCrawl timestamp.
	 */
	private boolean hasChanged(String id, long sig) {
		if (linkQueue != null) {

			if (id == null) {
				throw new RuntimeException(
						"Error: no itemId defined for record. "
								+ "If you use a link queue, you must also define an "
								+ "itemId/primary key, and it must be present in every record");
			}

			long prevSig = linkQueue.getSignature(id);
			linkQueue.update(id, sig, startOfCrawl);
			if (prevSig == sig) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Find all the records that didn't get touched, and delete them.
	 * 
	 * @param stageList
	 */
	public int lookForDeletes(StageList stageList) throws Exception {
		if (linkQueue == null)
			return 0;

		int itemsDeleted = 0;
		while (true) {
			String id = linkQueue.fetchNextUncrawled(startOfCrawl);
			if (id == null) {
				break;
			}

			itemsDeleted++;
			item.clear();
			item.setItemId(id);
			item.setAction(Item.ACTION_DELETE);

			// push it down the pipeline
			stageList.processItem(item);

			// remove it from the queue
			linkQueue.remove(id);
		}
		return itemsDeleted;
	}

	public String getDescription() {
		return "Crawls tables in a SQL database";
	}

	public String getDisplayName() {
		return "SQL Crawler";
	}

	public String getLastMessage() {
		String msg = super.getLastMessage();
		if (recsProcessed > 0) {
			msg += "/recs processed=" + recsProcessed;
		}
		return msg;
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}

	public String getPageName() {
		return "connector_sql_crawler.jsp";
	}

	public String getShortName() {
		return "SQLCrawler";
	}

	public int getItemsAdded() {
		return itemsAdded;
	}

	public int getItemsDeleted() {
		return itemsDeleted;
	}

	public int getItemsUnchanged() {
		return itemsUnchanged;
	}

}
