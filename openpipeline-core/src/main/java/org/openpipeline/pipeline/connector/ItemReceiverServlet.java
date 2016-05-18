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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.scheduler.PipelineScheduler;

/**
 * A servlet that receives items and pushed them down a pipeline.
 */
public class ItemReceiverServlet extends HttpServlet {
	/*
	 * TODO
	 * make sure itemId gets set
	 * make sure action gets put into the xml
	 * add docbinary to the xml
	 * test
	 * 
	 */
	
	
	private static final long serialVersionUID = 1L;

	private ConcurrentHashMap<String, ItemReceiverConnector> connectors = new ConcurrentHashMap();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String jobName = req.getParameter("jobname");
		
		ItemReceiverConnector connector;
		try {
			connector = getConnector(jobName);
		} catch (Exception e) {
			String msg = "An attempt to retrieve the connector generated the following error:" +
				e.toString();
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, msg);
			return;
		}
		
		ServletInputStream in = req.getInputStream();
		InputStreamReader reader = new InputStreamReader(in, "UTF-8");

		// must create a new item because this method is multithreaded
		Item item = new Item();

		try {
			item.importXML(reader);
			connector.processItem(item);

			resp.getWriter().write("ok");
			resp.getWriter().close();
			
		} catch (PipelineException e) {
			String msg = "Error importing xml or processing item";
			connector.error(msg, e);
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, msg + e.toString());
		}
	}

	
	/**
	 * Returns the Connector for the given jobName, if it exists.
	 */
	private ItemReceiverConnector getConnector(String jobName) throws Exception {
		ItemReceiverConnector con = connectors.get(jobName);
		if (con == null) {

			// doesn't really need to be synchronized
			// it's ok if two copies get created at the same time
			synchronized (this) {
				PipelineScheduler scheduler = PipelineScheduler.getInstance();
				Connector tmp = scheduler.getJob(jobName);
				if (tmp == null) {
					throw new Exception("Invalid jobName. Either the job does not exist or " +
							"the job instance is not running. jobName=" + jobName);
				}
				if (!(tmp instanceof ItemReceiverConnector)) {
					throw new Exception("Invalid jobName. The job is " +
							"not of type ItemReceiverConnector. jobName=" + jobName);
				}
				con = (ItemReceiverConnector) tmp;
				connectors.put(jobName, con);
			}
		}
		return con;
	}

	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	
}
