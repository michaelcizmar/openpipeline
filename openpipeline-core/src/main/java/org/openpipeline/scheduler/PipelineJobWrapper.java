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
package org.openpipeline.scheduler;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.util.XMLConfig;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * This class is a wrapper around a class that implements a PipelineJob interface. It
 * implements the Quartz InterruptableJob and StatefulJob interfaces. The purpose of 
 * it is to separate pipeline jobs from the Quartz classes, so that you don't need
 * Quartz to be able to execute a pipeline job. When the scheduler needs to
 * run a job, it creates an instance of this class, which internally loads
 * the actual job class and executes it.
 */
public class PipelineJobWrapper implements InterruptableJob, StatefulJob {

	private Connector job;

	/**
	 * Load the class specified in the params in the jobContext, and
	 * then execute it.
	 */
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {

		JobDataMap dataMap = jobContext.getJobDetail().getJobDataMap();
		XMLConfig params = (XMLConfig) dataMap.get("jobparams");

		// will equal something like "com.openpipeline.pipeline.connector.FileScanner"
		String jobClassStr = params.getProperty("jobclass");
		Class jobClass;

		try {
			jobClass = Class.forName(jobClassStr);
			job = (Connector) jobClass.newInstance();

		} catch (Exception e) {
			JobExecutionException jee = new JobExecutionException(e);
			throw jee;
		}

		String jobName = params.getProperty("jobname");
		job.setParams(params);
		job.setJobName(jobName);
		
		String errMsg = "";
		
		try {
			job.initialize();
			job.execute();
			
		} catch (Throwable t) {
			
			job.error("Error executing job: " + job.getShortName(), t);
			job.setLastMessage("Error: " + t.toString());
			
		} finally {
			
			try {
				job.close();
			} catch (PipelineException e) {
				job.error("Could not close job", e);
			}
			
			dataMap.put("lastmessage", errMsg + job.getLastMessage());
			dataMap.put("errorcount", job.getErrorCount() + "");
			dataMap.put("warningcount", job.getWarningCount() + "");
		}
	}

	/**
	 * Interrupt an executing job.
	 */
	public void interrupt() {
		if (job != null) {
			job.interrupt();
		}
	}

	/**
	 * Return the PipelineJob that this object wraps.
	 */
	public Connector getConnector() {
		return job;
	}

}
