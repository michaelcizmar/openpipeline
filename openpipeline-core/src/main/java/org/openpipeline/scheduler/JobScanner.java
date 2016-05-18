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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openpipeline.server.Server;
import org.openpipeline.util.XMLConfig;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;


/**
 * Scans all files in /config/jobs and loads them into the scheduler. Each job
 * exists in its own file. 
 */
public class JobScanner {
	/*
	 * Job file format:
	 * 
	 *   <job>
	 *     <group>index</group>
	 *     <jobname>FileScanner0</jobname>
	 *     <jobclass>com.openpipeline.pipeline.connector.FileScanner</jobclass>
	 *     
	 *     <index-name>myindex</index-name>
	 *     other job-specific params here
	 *     
	 *     <trigger>
	 *       <starttime>yyyy-mm-dd-hh-mm-ss</starttime>
	 *       <schedtype>periodic</schedtype>
	 *       <period>daily</period>
	 *       <periodinterval>2</periodinterval>
	 *       <cronexp>some expression</cronexp>
	 *     </trigger>
	 *   </job>
	 *   
	 *   Another example:
	 *   <job>
	 *     <group>recommender</group>
	 *     <name>buildmodel</name>
	 *     <jobclass>ModelBuilderJob</jobclass>
	 *     etc.
	 *   </job>
	 * 
	 */

	private boolean useAutoload = false;
	
	/**
	 * Load all xml config files from the /jobs directory and put them
	 * into the scheduler.
	 */
	public void loadJobsIntoScheduler() {
		Server server = Server.getServer();
		String homeDir = server.getHomeDir();
		
		File jobsDir = new File(homeDir, "config/jobs");
		File [] jobFiles = jobsDir.listFiles();
		if (jobFiles != null) {
			for (int i = 0; i < jobFiles.length; i++) {
				File jobFile = jobFiles[i];
				String filename = jobFile.toString().toLowerCase();
				if (filename.endsWith(".xml")) {

					try {
						Logger logger = Server.getServer().getLogger();
						logger.info("Loading job file:" + jobFile.toString());

						loadJobIntoScheduler(jobFile);
						
					} catch (Exception e) {
						Logger logger = Server.getServer().getLogger();
						logger.error("Error loading job file:" + jobFile.toString(), e);
					}
					
				}
			}
		}
	}
	
	/**
	 * Load the job in the XMLConfig file into the scheduler.
	 */
	public void loadJobIntoScheduler(File file) throws SchedulerException, ParseException, IOException {
		XMLConfig jobXML = new XMLConfig();
		jobXML.load(file);
		
		// if autoload is set to false, then do not load the job on startup
		if (useAutoload) {
			boolean autoload = jobXML.getBooleanProperty("autoload", true);
			if (!autoload) {
				return;
			}
		}
		
		String jobname = jobXML.getProperty("jobname");
		
		Trigger trigger = getTrigger(jobXML, jobname);
		JobDetail jobDetail = getJobDetail(jobname);
		
		JobDataMap dataMap = jobDetail.getJobDataMap();
		dataMap.put("jobparams", jobXML);
		
		PipelineScheduler pipelineScheduler = PipelineScheduler.getInstance();
		Scheduler scheduler = pipelineScheduler.getQuartzScheduler();
		
		// if the job is already loaded, remove it
		scheduler.deleteJob(jobname, PipelineScheduler.GROUP_NAME); 
		
		if (trigger == null) {
			// we get here if schedType = ondemand
			scheduler.addJob(jobDetail, false);
		} else {
			scheduler.scheduleJob(jobDetail, trigger);
		}

	}
	

	/**
	 * Create a trigger from the params in the xml.
	 * @param group 
	 * @param jobname 
	 */
	private Trigger getTrigger(XMLConfig jobXML, String jobname) throws SchedulerException, ParseException {
		
		XMLConfig triggerXML = jobXML.getChild("trigger");
		if (triggerXML == null)
			return null;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");

        
		String schedType = triggerXML.getProperty("schedtype");
		String period = triggerXML.getProperty("period");
		// period interval is the number of minutes, hours, etc. 1..60
		int periodInterval = triggerXML.getIntProperty("period-interval");
		String cronexp = triggerXML.getProperty("cronexp");
		String startTime = triggerXML.getProperty("starttime", formatter.format(new Date()));
		Date startTimeDate = null;
		
		// parse and set start time
		if (startTime != null) {
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

	private String getPeriodDesc(String period, int interval) {
		if (interval == 1) {
			return "Every " + period;
		} else {
			return "Every " + interval + " " + period + "s";
		}
	}
	

	/**
	 * Create a JobDetail object from the XML.
	 * @param group 
	 * @param jobname 
	 */
	private JobDetail getJobDetail(String jobname) {
		
		Class jobClass = PipelineJobWrapper.class;
		JobDetail jobDetail = new JobDetail(jobname, PipelineScheduler.GROUP_NAME, jobClass);
		jobDetail.setDurability(true);
		
		return jobDetail;
	}
	
	/**
	 * Save the job to disk.
	 * @param jobXML contains the job parameters
	 * @return a File object that points to the xml on disk
	 */
	public File saveJob(XMLConfig jobXML) throws IOException {
		String jobName = jobXML.getProperty("jobname");
		File file = getJobFile(jobName);
		jobXML.save(file);
		return file;
	}


	/**
	 * Save the job to disk then load it into the scheduler.
	 * @param jobXML contains the job parameters
	 */
	public void saveAndLoadJob(XMLConfig jobXML) throws IOException, SchedulerException, ParseException {
		File file = saveJob(jobXML);
		loadJobIntoScheduler(file);
	}
	
	/**
	 * Delete the job from disk.
	 * @param jobName name of the job.
	 */
	public boolean removeJob(String jobName) {
		// remove not only the job file, but also the timestamp file

		File file = getJobFile(jobName);
		String filename = file.getName();
		
		// strip the "xml" extension, leave the "."
		String prefix = filename.substring(0, filename.length() - 3);
		
		File [] files = file.getParentFile().listFiles();
		for (int i = 0; i < files.length; i++) {
			File subFile = files[i];
			if (subFile.getName().startsWith(prefix)) {
				if (!subFile.delete()) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Read the job file from disk and populate an XMLConfig object.
	 * @param jobName name of job, not including the .xml extension
	 * @return a populated XMLConfig file, or an empty one if the file
	 * does not exist
	 */
	public XMLConfig getJobFromDisk(String jobName) throws IOException {
		XMLConfig conf = new XMLConfig();
		File file = getJobFile(jobName);
		if (file.exists()) {
			conf.load(file);
		}
		return conf;
	}
	
	/**
	 * Return a file pointing to the job with this jobName
	 * @param jobName name of the job
	 * @return a File. May or may not exist.
	 */
	public File getJobFile(String jobName) {
		Server server = Server.getServer();
		File file = new File(server.getHomeDir(), "config/jobs/" + jobName + ".xml");
		return file;
	}

	/**
	 * Normally false, useAutoload should be set true when loading classes
	 * on startup so that jobs that have autoload=N won't be loaded.
	 * Sometimes it's useful to ignore certain jobs on startup.
	 * @param useAutoload indicates whether the system should use, or ignore,
	 * the autoload parameter in a job config file.
	 */
	public void setUseAutoload(boolean useAutoload) {
		this.useAutoload = useAutoload;
	}
	
}



