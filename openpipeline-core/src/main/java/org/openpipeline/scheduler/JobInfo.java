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


/**
 * Contains information on a job in the scheduler. Intended for displaying
 * job information on the view_jobs page in the Admin interface.
 */
public class JobInfo implements Comparable<JobInfo>{
	private String jobName;
	private String schedule;
	private String pageName;
	private String nextFireTime;
	private String logLink;
	private String lastMessage;
	private boolean isRunning;
	private int errorCount;
	private int warningCount;

	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	
	/**
	 * Get a human-readable description of the schedule for display.
	 */
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	public void setLastMessage(String lastMessage) {
		if (lastMessage == null) {
			this.lastMessage = "";
		} else {
			this.lastMessage = lastMessage;
		}
	}
	public String getLastMessage() {
		return lastMessage;
	}
	
	public int getWarningCount() {
		return warningCount;
	}
	public void setWarningCount(int warningCount) {
		this.warningCount = warningCount;
	}
	
	public int getErrorCount() {
		return errorCount;
	}
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}
	public void setIsRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}
	public boolean getIsRunning() {
		return isRunning;
	}
	

	public String getRemoveDesc() {
		// do not show a link if the job is running
		if (isRunning) {
			return "";
		} else {
			return "x";
		}
	}
	
	public String getRemoveLink() {
		return "action=remove&jobname=" + jobName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}
	
	public String getPageName() {
		return pageName;
	}
	
	public void setNextFireTime(String nextFireTime) {
		this.nextFireTime = nextFireTime;
	}
	
	public String getNextFireTime() {
		if (nextFireTime == null)
			return "";
		return nextFireTime;
	}
	
	public void setLogLink(String logLink) {
		this.logLink = logLink;
	}
	
	public String getLogLink() {
		return logLink;
	}
	public int compareTo(JobInfo o) {
		return jobName.compareToIgnoreCase(o.jobName);
	}

}
