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

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is a start on a new Scheduling system that will replace Quartz. It's
 * not yet in use. 
 */
public class Scheduler {
	/* Still to do:
	 * 
	 * Figure out how to do passive connectors: connectors that
	 * aren't running but get called from the rest connector.
	 * Must initialize, shutdown properly. Also show the status
	 * in the UI.
	 * 
	 * Separate a job definition from a running job. This allows
	 * the same job def to spawn multiple, simultaneous tasks.
	 * Separate the lists. Separate the display in the UI.
	 * 
	 * Deal with fatal errors vs exceptions in Job. Any exception, checked or
	 * not, stops scheduler for that job.
	 * 
	 * Do job groups.
	 */
	
	
    private static Scheduler scheduler = new Scheduler();
	private TreeMap<String, Job> jobs = new TreeMap(); // treemap keeps it sorted
    private ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);

    // don't allow instantiation
    private Scheduler() {
    }
	
    public static Scheduler getScheduler() {
    	return scheduler;
    }
	
	/**
	 * Reload jobs from disk.
	 */
	public void reloadJobs() {
	}

	public void startRunningJobs() {
		for (Job job: jobs.values()) {
			scheduleNextExecution(job);
		}
	}
	
	// package-private
	void scheduleNextExecution(Job job) {
		
		long nextFireTime = job.getTrigger().getNextFireTime();
		long delay = nextFireTime - System.currentTimeMillis();
		
		// if delay happens to be <= 0, which could happen, the task is
		// executed immediately
		execService.schedule(job, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * Stops the scheduler, prevents the execution of any jobs that
	 * have not yet started. Attempts to interrupt jobs that are currently
	 * executing, but does not forcibly shut them down.
	 */
	public void stop() {
		execService.shutdown();
		for (Job job: jobs.values()) {
			job.interruptJob();
		}
	}


	/**
	 * Returns the job with the given name.
	 * @param jobName the job to return
	 * @return the job, or null if no job with that name exists
	 */
	public Job getJob(String jobName) {
		return jobs.get(jobName);
	}
	
	public void addJob(Job job) {
		jobs.put(job.getJobName(), job);
	}
	
	public void removeJob(String jobName) {
		jobs.remove(jobName);
	}

	public SortedMap<String, Job> getJobs() {
		return jobs;
	}
	
	

}
