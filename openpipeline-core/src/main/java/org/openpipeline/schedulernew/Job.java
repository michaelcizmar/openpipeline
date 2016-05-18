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
package org.openpipeline.schedulernew;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;


/**
 * Not yet in use. See Scheduler.java.
 * 
 * Only one instance of a job runs at a time.
 * 
 */
public class Job implements Callable {
	
	// holds the current connector instance. 
	// a new instance gets created when the job runs.
	// after a job stops, this connector is still here so we can
	// get status information from it, like the number of errors
	private Connector connector;
	
	private XMLConfig params;
	private String jobName;
	private Trigger trigger;
	private String connectorClassName; // fully-qualified: com.mydomain.MyClass
	private volatile boolean isRunning;

	
	public Object call() throws Exception {
		
		Class clazz = Class.forName(connectorClassName);
		connector = (Connector) clazz.newInstance();
		
		connector.setParams(params);
		connector.setJobName(jobName);

		try {
		
			isRunning = true;
			connector.initialize();
			connector.execute();
			
			if (!connector.getInterrupted()) {
				Scheduler.getScheduler().scheduleNextExecution(this);
			}
			
		} catch (Throwable t) {
			
			// TODO deal with fatal errors vs exceptions
			
			connector.error("Error executing job: " + connector.getShortName(), t);
			connector.setLastMessage("Error: " + t.toString());
			
		} finally {
			try {
				connector.close();
			} catch (PipelineException e) {
				connector.error("Could not close connector", e);
			} finally {
				isRunning = false;
			}
		}
		
		return null;
	}
	
	public void interruptJob() {
		// making a copy of the reference eliminates threading issues
		Connector con = connector;
		if (con != null) {
			con.interrupt();
		}
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public void loadFromDisk(String jobName) throws IOException {
		this.jobName = jobName;
		String filename = Server.getServer().getHomeDir() + "/config/jobs/" + jobName + ".xml";
		File file = new File(filename);
		if (file.exists()) {
			params = new XMLConfig();
			params.load(file);
			
		} else {
			throw new FileNotFoundException("File not found: " + filename);
		}
		
		connectorClassName = params.getProperty("jobclass");
		loadTrigger(params);
	}
	

	private void loadTrigger(XMLConfig params) {
		// TODO Auto-generated method stub
		
	}
	
	public Trigger getTrigger() {
		return trigger;
	}

	public String getJobName() {
		return jobName;
	}




	
	
	/**
	 * Create a trigger from the params in the xml.
	 * @param group 
	 * @param jobname 
	 * /
	private Trigger getTrigger(XMLConfig jobXML, String jobname) throws SchedulerException, ParseException {
		
		XMLConfig triggerXML = jobXML.getChild("trigger");
		if (triggerXML == null)
			return null;

		String schedType = triggerXML.getProperty("schedtype");
		String period = triggerXML.getProperty("period");
		// period interval is the number of minutes, hours, etc. 1..60
		int periodInterval = triggerXML.getIntProperty("period-interval");
		String cronexp = triggerXML.getProperty("cronexp");
		String startTime = triggerXML.getProperty("starttime");
		Date startTimeDate = null;
		
		// parse and set start time
		if (startTime != null) {
	        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
	        startTimeDate = (Date)formatter.parse(startTime);
			if (startTimeDate == null) {
				throw new SchedulerException("Bad start time format: " + startTime);
			}
		}
		
		Trigger trigger;

		if (schedType == null) {
			schedType = "ondemand";
		}
		
		if ("ondemand".equals(schedType)) {
			trigger = null;

		} else if ("onetime".equals(schedType)) {
			trigger = new SimpleTrigger(jobname, PipelineScheduler.GROUP_NAME, startTimeDate);
			trigger.setDescription("One Time Only");
			
			// this says that if the date is in the past, do nothing
			trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);

		} else if ("periodic".equals(schedType)) {
			
			String description = "error(periodic)";
			if ("months".equals(period)) {
				// @todo to do this correctly,
				// need start time to get day of month, fire
				// that day at the specified start time
				// use cronTrigger
				long repeatInterval = 30L * 24 * 60 * 60 * 1000 * periodInterval; // every 30 days exactly
				trigger = new SimpleTrigger(jobname, PipelineScheduler.GROUP_NAME, startTimeDate, null, SimpleTrigger.REPEAT_INDEFINITELY, repeatInterval);
				description = getPeriodDesc("month", periodInterval);
			
			} else {
				// else it's a simple periodic interval
				long repeatInterval = -1; // millisec
				if ("minutes".equals(period)) {
					repeatInterval = 60 * 1000 * periodInterval;
					description = getPeriodDesc("minute", periodInterval);
					
				} else if ("hours".equals(period)) {
					repeatInterval = 60 * 60 * 1000 * periodInterval;
					description = getPeriodDesc("hour", periodInterval);
					
				} else if ("days".equals(period)) {
					repeatInterval = 24L * 60 * 60 * 1000 * periodInterval;
					description = getPeriodDesc("day", periodInterval);
					
				} else if ("weeks".equals(period)) {
					repeatInterval = 7L * 24 * 60 * 60 * 1000 * periodInterval;
					description = getPeriodDesc("week", periodInterval);
				}
				trigger = new SimpleTrigger(jobname, PipelineScheduler.GROUP_NAME, startTimeDate, null, SimpleTrigger.REPEAT_INDEFINITELY, repeatInterval);
			}
			trigger.setDescription(description);

		} else if ("cronexp".equals(schedType)) {
			trigger = new CronTrigger(jobname, PipelineScheduler.GROUP_NAME, jobname, PipelineScheduler.GROUP_NAME, startTimeDate, null, cronexp); 
			trigger.setDescription("Cron: " + cronexp);
			trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);

		} else {
			throw new SchedulerException("Bad schedType: " + schedType);
		}

		return trigger;
	}
	*/

	
	
	
	

}
