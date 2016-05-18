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

import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * Not yet in use. See Scheduler.java
 * Defines a rule for when a job should fire. The schedule begins at the starting
 * time. The job can fire one time only at the starting time, or periodically,
 * or according to a chron expression. 
 */
public class Trigger {
	
	private long nextFireTime = -1;
	private long startTime = -1;
	private long interval;
	private String cronExp;

	/**
	 * Set the starting time, expressed in milliseconds since the epoch.
	 * @param startTime starting time in milliseconds
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Set the starting date and time. The startTime should
	 * be in the format "yyyy-mm-dd" or "yyyy-mm-dd-hh-mm-ss". The 
	 * hours/minutes/seconds are optional. The separators between the 
	 * fields are ignored, in other words, the format "yyyy mm dd hh:mm:ss" 
	 * will also work.
	 * @param startTime a starting date and time.
	 */
	public void setStartTime(String startTime) {
		GregorianCalendar cal = new GregorianCalendar();
		cal.clear();
		cal.setTimeZone(TimeZone.getDefault()); // TODO make this configurable

		// yyyy-mm-dd-hh-mm-ss
		// 0123 56 89 12 34 56
		
		int year = Integer.parseInt(startTime.substring(0, 4));
		int month = Integer.parseInt(startTime.substring(5, 2)) - 1; // month is 0-based
		int day = Integer.parseInt(startTime.substring(8, 2));
		
		if (startTime.length() > 10) {
			int hour = Integer.parseInt(startTime.substring(11, 2));
			int minute = Integer.parseInt(startTime.substring(13, 2));
			int second = Integer.parseInt(startTime.substring(15, 2));
			cal.set(year, month, day, hour, minute, second);
		} else {
			cal.set(year, month, day);
		}
		
		setStartTime(cal.getTimeInMillis());
	}
	
	public void setFireEveryXMonths(int x) {
		// TODO make this run at a fixed time every month
		// this interval scheme doesn't work very well
		// because months are of different duration
		// maybe just do a chron expression
		interval = x * 30 *  7 * 24 * 60 * 60 * 1000;
	}
	public void setFireEveryXWeeks(int x) {
		interval = x * 7 * 24 * 60 * 60 * 1000;
	}
	public void setFireEveryXDays(int x) {
		interval = x * 24 * 60 * 60 * 1000;
	}
	public void setFireEveryXHours(int x) {
		interval = x * 60 * 60 * 1000;
	}
	public void setFireEveryXMinutes(int x) {
		interval = x * 60 * 1000;
	}
	public void setFireEveryXSeconds(int x) {
		interval = x * 1000;
	}
	public void setFireEveryXMilliseconds(long x) {
		interval = x;
	}
	public void setFireOnCronExpression(String cronExp) {
		this.cronExp = cronExp;
	}
	
	/**
	 * Returns the next time the job is scheduled to fire. Always returns
	 * a time in the future.
	 * @return a time, expressed in millis, or -1 if the job is not
	 * scheduled to fire at any time in the future
	 */
	public long getNextFireTime() {
		long now = System.currentTimeMillis();
		if (nextFireTime <= now) {
			calculateNextFireTime(now);
		}
		return nextFireTime;
	}
	
	private void calculateNextFireTime(long now) {
		
		// TODO deal with chron expressions
		
		if (startTime == -1) {
			// startTime has not been set. Don't schedule anything.
			nextFireTime = -1;
			return;
		}
		if (interval <= 0) {
			nextFireTime = -1;
			return;
		}
		
		// if we haven't reached the start time yet
		if (now <= startTime) {
			nextFireTime = startTime;
			return;
		}
		
		// We start at the startTime, and then every "interval" milliseconds
		// thereafter. Find the start of the next interval which is 
		// greater than "now"
		long since = now - startTime;
		int intervalsToSkip = (int) Math.ceil((double)since / interval);
		nextFireTime = startTime + (intervalsToSkip * interval);
	}
	
}
