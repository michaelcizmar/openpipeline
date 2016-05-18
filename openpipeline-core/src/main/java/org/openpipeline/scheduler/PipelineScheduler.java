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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.util.XMLConfig;
import org.quartz.CronExpression;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * The main scheduler class for an OpenPipeline server. Accepts PipelineJobs
 * and executes them at the designated time. Provides methods for
 * seeing what's been scheduled and the status of each job. 
 * <p>
 * There is only one PipelineScheduler per JVM. A reference to it is stored
 * in a static variable internally. Get the reference with a call to getInstance().
 */
public class PipelineScheduler {

	// group name for all openpipeline jobs
	final static public String GROUP_NAME = "openpipeline";

	static private PipelineScheduler pipelineScheduler;

	private Scheduler quartzScheduler; // the quartz scheduler
	private QuartzJobListener quartzJobListener = new QuartzJobListener();

	/**
	 * Return the PipelineScheduler. There is only one instance of
	 * a PipelineScheduler per JVM.
	 * @return the scheduler
	 * @throws SchedulerException 
	 */
	static public PipelineScheduler getInstance() throws SchedulerException {
		if (pipelineScheduler == null) {
			pipelineScheduler = new PipelineScheduler();

			JobScanner scanner = new JobScanner();
			scanner.setUseAutoload(true);
			scanner.loadJobsIntoScheduler();
		}
		return pipelineScheduler;
	}

	/**
	 * Marked private so only getInstance() can create it.
	 */
	private PipelineScheduler() throws SchedulerException {

		/* It's not ideal to use system properties like this, because
		 * there's a potential conflict if another app in the same container
		 * is using Quartz. This is the only easy way, though, to
		 * use the default quartz.properties file embedded in the quartz
		 * jar file, and just add a couple of extra props. Otherwise
		 * there's lots of classloader complexity. See the source
		 * code for org.quartz.impl.StdSchedulerFactory
		 */

		//default properties are lost if we do this and pass props to StdSchedulerFactory.
		//Properties props = new Properties();
		//props.setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
		//props.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");
		System.setProperty("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
		System.setProperty("org.quartz.threadPool.makeThreadsDaemons", "true");
		System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

		SchedulerFactory schedFactory = new StdSchedulerFactory();
		quartzScheduler = schedFactory.getScheduler();
		quartzScheduler.addGlobalJobListener(quartzJobListener);
		quartzScheduler.start();
	}

	/**
	 * Return a List of JobInfo objects for display in the admin interface.
	 */
	public List getJobs() throws Exception {
		ArrayList list = new ArrayList();

		List currJobs = quartzScheduler.getCurrentlyExecutingJobs();

		// make an array for faster evaluation
		JobExecutionContext[] jobContexts = new JobExecutionContext[currJobs.size()];
		currJobs.toArray(jobContexts);

		String[] groupNames = quartzScheduler.getJobGroupNames();
		if (groupNames != null) {
			for (int i = 0; i < groupNames.length; i++) {
				String groupName = groupNames[i];
				String[] jobNames = quartzScheduler.getJobNames(groupName);
				if (jobNames != null) {
					for (int j = 0; j < jobNames.length; j++) {
						String jobName = jobNames[j];

						// get info on the job
						JobDetail jobDetail = quartzScheduler.getJobDetail(jobName, groupName);
						JobDataMap dataMap = jobDetail.getJobDataMap();
						XMLConfig params = (XMLConfig) dataMap.get("jobparams");

						Trigger trigger = getCurrentTrigger(jobName, groupName);
						String schedule = getSchedule(trigger);
						String nextFireTime = getNextFireTime(trigger);
						String lastMessage;
						int errorCount = 0;
						int warningCount = 0;
						boolean isRunning;

						// returns a job instance if executing, else null
						Connector pipelineJob = getCurrentJob(jobContexts, jobDetail);
						if (pipelineJob == null) {
							// job not executing
							String jobClassStr = params.getProperty("jobclass");
							Class jobClass = Class.forName(jobClassStr);
							pipelineJob = (Connector) jobClass.newInstance();

							// get the status when the job last ended
							lastMessage = dataMap.getString("lastmessage");
							String errs = dataMap.getString("errorcount");
							if (errs != null) {
								errorCount = Integer.parseInt(errs);
							}
							String warnings = dataMap.getString("warningcount");
							if (warnings != null) {
								warningCount = Integer.parseInt(warnings);
							}
							
							isRunning = false;

						} else {
							// job executing
							lastMessage = pipelineJob.getLastMessage();
							errorCount = pipelineJob.getErrorCount();
							warningCount = pipelineJob.getWarningCount();
							isRunning = true;
						}

						String pageName = pipelineJob.getPageName();
						String logLink = pipelineJob.getLogLink();

						// populate jobinfo for display
						JobInfo jobInfo = new JobInfo();
						jobInfo.setJobName(jobName);
						jobInfo.setSchedule(schedule);
						jobInfo.setNextFireTime(nextFireTime);
						jobInfo.setPageName(pageName);
						jobInfo.setLogLink(logLink);
						jobInfo.setIsRunning(isRunning);
						jobInfo.setLastMessage(lastMessage);
						jobInfo.setErrorCount(errorCount);
						jobInfo.setWarningCount(warningCount);

						list.add(jobInfo);
					}
				}
			}
		}

		Collections.sort(list);
		
		return list;
	}

	/**
	 * Roll through the currently executing jobs and see if any match the specified
	 * jobDetail. If so, return the context. This holds a reference to the
	 * actually job instance.
	 */
	private Connector getCurrentJob(JobExecutionContext[] jobContexts, JobDetail jobDetail) {

		String fullName = jobDetail.getFullName();

		for (int i = 0; i < jobContexts.length; i++) {
			JobExecutionContext context = jobContexts[i];
			// Trigger trigger = context.getTrigger(); // this is avail if we need it
			JobDetail currDetail = context.getJobDetail();
			String currName = currDetail.getFullName();
			if (currName.equals(fullName)) {
				PipelineJobWrapper wrapper = (PipelineJobWrapper) context.getJobInstance();
				Connector job = wrapper.getConnector();
				return job;
			}
		}
		return null;
	}

	/**
	 * Return the trigger that was defined in the config for this job, or if none,
	 * return null.
	 */
	private Trigger getCurrentTrigger(String jobName, String groupName) throws SchedulerException {
		/* if there are no triggers, then the job is on-demand
		 * if there are two, then the first is the one defined in the config,
		 *   and the second is an adhoc trigger.
		 * if there is only one, then it's tricker. If the trigger has no description,
		 * then assume it's an adhoc trigger, else it's the defined one.
		 */

		Trigger[] triggers = quartzScheduler.getTriggersOfJob(jobName, groupName);
		if (triggers == null || triggers.length == 0) {
			return null;

		} else if (triggers.length == 1) {
			String desc = triggers[0].getDescription();
			if (desc == null) {
				return null;
			} else {
				return triggers[0];
			}

		} else {
			return triggers[0];
		}

	}

	/**
	 * Get the next time the trigger will fire.
	 */
	private String getNextFireTime(Trigger trigger) {

		if (trigger == null) {
			return null;
		}

		Date date = trigger.getNextFireTime();
		if (date == null)
			return null;

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");

		return df.format(date);
	}

	/**
	 * Get a human-readable description of the schedule for display.
	 */
	private String getSchedule(Trigger trigger) {
		if (trigger == null) {
			return "On Demand";
		} else {
			return trigger.getDescription();
		}
	}

	/**
	 * Shut down the Quartz scheduler.
	 */
	static public void stop() throws SchedulerException {
		if (pipelineScheduler != null) {

			Scheduler sched = pipelineScheduler.quartzScheduler;
			if (sched != null) {
				sched.shutdown();
				pipelineScheduler.quartzScheduler = null;
			}
			pipelineScheduler = null;
		}
	}

	/**
	 * Return the internal Quartz scheduler.
	 */
	public Scheduler getQuartzScheduler() {
		return quartzScheduler;
	}

	/**
	 * Interrupt a job that is currently executing.
	 */
	public void interruptJob(String jobName) throws UnableToInterruptJobException {
		quartzScheduler.interrupt(jobName, GROUP_NAME);
	}

	/**
	 * Start a job now.
	 * @return true if the job was triggered, false if the job was already running
	 */
	public boolean startJob(String jobName) throws SchedulerException {

		// check to see if it's already running
		if (isJobRunning(jobName)) {
			return false;
		}

		// start the job
		quartzScheduler.triggerJob(jobName, GROUP_NAME);

		return true;
	}

	/**
	 * Return true if the specified job is running.
	 */
	public boolean isJobRunning(String jobName) throws SchedulerException {
		List list = quartzScheduler.getCurrentlyExecutingJobs();
		for (int i = 0; i < list.size(); i++) {
			JobDetail jobDetail = ((JobExecutionContext) list.get(i)).getJobDetail();
			String name = jobDetail.getName();
			String group = jobDetail.getGroup();

			if (name != null && name.equals(jobName) && group != null && group.equals(GROUP_NAME)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the job from the scheduler. Does not stop it from running.
	 */
	public void removeJob(String jobName) throws SchedulerException {
		quartzScheduler.deleteJob(jobName, GROUP_NAME);
	}

	/**
	 * Return true if the specified job has been loaded.
	 */
	public boolean isJobLoaded(String jobName) throws SchedulerException {
		return quartzScheduler.getJobDetail(jobName, GROUP_NAME) != null;
	}

	/**
	 * Check to see if the cron expression is valid, and throw an exception if not.
	 * @param cronexp a cron expression
	 * @throws SchedulerException if it's not valid
	 */
	public static void validateCronExpression(String cronexp) throws SchedulerException {
		if (cronexp != null && cronexp.length() > 0) {
			if (!CronExpression.isValidExpression(cronexp)) {
				throw new SchedulerException("Not a valid cron expression: " + cronexp);
			}
		}
	}

	/**
	 * Returns an instance of the Connector for a given job name. Otherwise, null. 
	 * @throws SchedulerException
	 */
	public Connector getJob(String jobName) throws SchedulerException {
		JobDetail jobDetail = this.quartzScheduler.getJobDetail(jobName, GROUP_NAME);
		List currJobs = this.quartzScheduler.getCurrentlyExecutingJobs();
		JobExecutionContext[] jobContexts = new JobExecutionContext[currJobs.size()];
		currJobs.toArray(jobContexts);
		return this.getCurrentJob(jobContexts, jobDetail);
	}

	public void addJobListener(JobListener listener) {
		quartzJobListener.addJobListener(listener);
	}
	
	public void removeJobListener(JobListener listener) {
		quartzJobListener.removeJobListener(listener);
	}
	
	
	class QuartzJobListener implements org.quartz.JobListener {

		private List<JobListener> listeners = Collections.synchronizedList(new ArrayList());
		
		public String getName() {
			return "quartzJobListener";
		}

		public void removeJobListener(JobListener listener) {
			// the remove method below is based on .equals(). The default
			// implementation of equals() in Object, though, is ==.
			// so this works.
			listeners.remove(listener);
		}

		public void addJobListener(JobListener listener) {
			listeners.add(listener);
		}
		
		@SuppressWarnings("unused")
		public void jobExecutionVetoed(JobExecutionContext context) {
		}

		public void jobToBeExecuted(JobExecutionContext context) {
			Connector job = ((PipelineJobWrapper)context.getJobInstance()).getConnector();
			String jobName = context.getJobDetail().getName();
			for (JobListener jl: listeners) {
				jl.jobToBeExecuted(jobName, job);
			}
		}

		@SuppressWarnings("unused")
		public void jobWasExecuted(JobExecutionContext context,
				JobExecutionException exception) {
			// exception ignored for now
			Connector job = ((PipelineJobWrapper)context.getJobInstance()).getConnector();
			String jobName = context.getJobDetail().getName();
			for (JobListener jl: listeners) {
				jl.jobToBeExecuted(jobName, job);
			}
		}
		
	}
	
}
