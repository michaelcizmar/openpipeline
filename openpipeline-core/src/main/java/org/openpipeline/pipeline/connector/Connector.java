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


import org.openpipeline.pipeline.stage.StageList;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

/**
 * Abstract class that all Connectors extend. Connectors must have a 
 * zero-argument constructor so they can be instantiated on-demand by the 
 * scheduler. A Connector will run when execute() is called.
 */
public abstract class Connector {
	
	private Logger logger;
	private String jobName;
	private String lastMessage;
	private int errorCount;
	private int warningCount;
	private XMLConfig params;
	private StageList stageList;
	volatile private boolean interrupted;
	
	/**
	 * Report an error that terminates the processing of the current item,
	 * but does not terminate the connector.
	 * @param msg the error message
	 */
	public void error(String msg) {
		errorCount++;
		logger.error(msg);
	}
	
	/**
	 * Report an error that terminates the processing of the current item,
	 * but does not terminate the connector.
	 * @param msg the error message
	 * @param t any associated exception
	 */
	public void error(String msg, Throwable t) {
		errorCount++;
		logger.error(msg, t);
	}
	
	/**
	 * Run this Connector. Will typically initiate a crawl of some kind 
	 * or start listening passively for items on a port.
	 */
	public abstract void execute();
	
	
	/**
	 * Return a description of the Connector for display in the Admin UI, for example,
	 * "Scans my data and processes it in a special way" 
	 */
	public String getDescription() {
		return getDisplayName();
	}

	/**
	 * Return the name of the Connector to display in the Admin UI, for example,
	 * "My Connector"
	 */
	public String getDisplayName() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Returns the number of errors generated during the current run of the
	 * job. 
	 * @return the number of errors
	 */
	public int getErrorCount() {
		return errorCount;
	}
	
	/**
	 * Return true if this connector instance has received
	 * an interrupt request.
	 * @return true if interrupted
	 */
	public boolean getInterrupted() {
		return interrupted;
	}
	
	/**
	 * Return the name of the current job.
	 * @return a job name
	 */
	public String getJobName() {
		return this.jobName;
	}

	/**
	 * Returns the most recent status message from the job. This is specific 
	 * to the job; could return "Running" or "Idle", or could return some kind
	 * of progress indicator ("Completed 1 of 10...").
	 */
	public String getLastMessage() {
		return lastMessage;
	}
	
	/**
	 * Returns the default logger for the connector. To set a different default
	 * logger, call setLogger() or override the initialize() method.
	 * @return the default logger
	 */
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Return the link the admin interface should use to display log files associated
	 * with this job. For example, if this job uses the main server log, the link
	 * should be "log_viewer.jsp". If it were to create its own directory of log files
	 * under /logs, then the link would be "log_viewer.jsp?logtype=my_directory".
	 * If this job does not have a log file, returns null. This link appears on the
	 * view_jobs page and possibly elsewhere.
	 * @return a link to the log file, or null if there isn't one
	 */
	public String getLogLink() {
		return null;
	}
	
	/**
	 * Return the name of the page in the Admin UI to edit the parameters, for example,
	 * "connector_myconnector.jsp". The convention is to start the name with "connector_"
	 * for connectors.
	 * @return the name of the page, or null if there is no configuration page.
	 */
	public String getPageName() {
		return null;
	}

	/**
	 * Return the params object that this connector was given when the
	 * scheduler created it for the current run.
	 * @return the current parameters
	 */
	public XMLConfig getParams() {
		return params;
	}
	
	/**
	 * Return the name to use when creating a name for a connector instance on the Add Job
	 * page in the Admin UI, for example, "MyConnector". The Admin will take this name, append
	 * one or more digits, and display it in the "Enter name of connector:" prompt.
	 */
	public String getShortName() {
		return getDisplayName();
	}
	
	/**
	 * Returns the number of warnings generated during the current run
	 * of the connector.
	 * @return the number of warnings
	 */
	public int getWarningCount() {
		return warningCount;
	}

	/**
	 * Performs any necessary initialization. By default, this method
	 * initializes the logger and the list of stages. Override this method to
	 * do any connector-specific initialization. 
	 * @throws PipelineException 
	 */
	public void initialize() throws PipelineException {
		logger = Server.getServer().getLogger();

		stageList = new StageList();
		stageList.setConnector(this);
		stageList.createPipeline(params);
		stageList.initialize();
	}

	/**
	 * Closes the connector, releases resources.
	 * @throws PipelineException
	 */
	public void close() throws PipelineException {
		if (stageList != null) {
			stageList.close();
		}
	}
	
	/**
	 * Return the StageList for this connector, that is, 
	 * the pipeline of stages that items will be pushed through.
	 * @return a fully-configured, initialized list of stages
	 */
	public StageList getStageList() {
		return stageList;
	}
	
	/**
	 * Interrupt the execution of this connector.
	 */
	public void interrupt() {
		logger.info("Interrupt requested for " + jobName);
		lastMessage = "Interrupt requested";
		interrupted = true;
	}
	
	/**
	 * Set the name of the job for this particular Connector instance.
	 * The schedule will call this method automatically when the job starts.
	 * @param jobName name of the job
	 */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	/**
	 * Set the current status message for reporting in the Admin UI.
	 * @param lastMessage a message for the user
	 */
	public void setLastMessage(String lastMessage) {
		this.lastMessage = lastMessage;
	}
	
	/**
	 * Set the default logger for the connector.
	 * @param logger the logger to use
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	

	/**
	 * Configure this connector. The parameters will enable to find content
	 * and specify rules for processing it.
	 * @param params parameters defined in an XML file
	 */
	public void setParams(XMLConfig params) {
		this.params = params;
	}
	
	/**
	 * Report a warning which does not terminate the processing
	 * of the current item.
	 * @param msg the warning message
	 */
	public void warn(String msg) {
		warningCount++;
		logger.warn(msg);
	}

	/** 
	 * Report a warning which does not terminate the processing
	 * of the current item.
	 * @param msg the error message
	 * @param t any associated exception
	 */
	public void warn(String msg, Throwable t) {
		warningCount++;
		logger.warn(msg, t);
	}
	
	

}
