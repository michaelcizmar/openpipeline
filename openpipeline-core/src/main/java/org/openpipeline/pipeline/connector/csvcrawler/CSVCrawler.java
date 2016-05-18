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
package org.openpipeline.pipeline.connector.csvcrawler;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.Util;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Crawls CSV files
 */
public class CSVCrawler extends Connector {

	private Logger logger;
	private int rowsProcessed;
	private Item item = new Item();
	
	/*
	private int recsProcessed;


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
	*/

	public void execute() {

		DONE: try {

			logger = super.getLogger();
			logger.info("Starting " + super.getJobName() + "...");
			super.setLastMessage("Running");
			long startOfCrawl = System.currentTimeMillis();
			
			XMLConfig params = super.getParams();
			if (params == null) {
				String msg = "setParams() was not called for CSVCrawler.";
				throw new IllegalStateException(msg);
			}
			
			String filename = params.getProperty("filename"); // could make this a URL instead
			String encoding = params.getProperty("encoding", "UTF-8");
			String itemIdCol = params.getProperty("itemid-col");

			if (super.getInterrupted()) {
				break DONE;
			}

			FileInputStream fis = new FileInputStream(filename);
			InputStreamReader isr = new InputStreamReader(fis, encoding);
			
			CsvReader rows = new CsvReader(isr);
			
			rows.readHeaders();
			String [] headers = rows.getHeaders();
			int colCount = headers.length;
			int itemIdColNum = -1;

			
			// find the number of the column to use for the itemId
			for (int i = 0; i < colCount; i++) {
				if (headers[i].equals(itemIdCol)) {
					itemIdColNum = i;
				}
			}
			if (itemIdColNum == -1) {
				throw new PipelineException("The column to use for the itemId was not found in the result set: " + itemIdCol + " cols:" + Arrays.asList(headers).toString());
			}

			rowsProcessed = 0;
			while (rows.readRecord()) {

				if (super.getInterrupted()) {
					break DONE;
				}
				
				if (item.hasAttributeDefs()) {
					Map defs = item.getAttributeDefs();
					item.clear();
					item.setAttributeDefs(defs);
				} else {
					item.clear();
				}
				
				for (int i = 0; i < colCount; i++) {
					String value = rows.get(i);
					
					if (value == null) {
						continue;
					}
					value = value.trim();
					if (value.length() == 0) {
						continue;
					}
					
					item.getRootNode().addNode(headers[i], value);

					if (i == itemIdColNum) {
						item.setItemId(value);
					}
				}
				
				try {
					super.getStageList().processItem(item);
				} catch (PipelineException e) {
					super.error("Error processing item " + item.getItemId()
							+ ". Message = " + e.getMessage());
				}
				
				rowsProcessed++;
			}
	
			rows.close();
			isr.close();
			fis.close();

			String elapsedStr = Util.getFormattedElapsedTime(System.currentTimeMillis() - startOfCrawl);
			String msg = "CSVCrawler ended. Rows processed: " + rowsProcessed + " Elapsed time: " + elapsedStr;

			logger.info(msg);
			super.setLastMessage(msg);
			
		} catch (Exception e) {
			super.error("Error executing CSVCrawler", e);
			super.setLastMessage("Error: " + e.toString());

		} 		
	}


	public String getDescription() {
		return "A simple crawler for a CSV file";
	}

	public String getDisplayName() {
		return "CSV Crawler";
	}

	public String getLastMessage() {
		String msg = super.getLastMessage();
		if (rowsProcessed > 0) {
			msg += "/recs processed=" + rowsProcessed;
		}
		return msg;
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}

	public String getPageName() {
		return "connector_csv_crawler.jsp";
	}

	public String getShortName() {
		return "CSVCrawler";
	}



}
