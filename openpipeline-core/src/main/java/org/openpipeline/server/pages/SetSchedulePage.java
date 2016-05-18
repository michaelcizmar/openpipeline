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
package org.openpipeline.server.pages;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.scheduler.PipelineScheduler;
import org.openpipeline.util.XMLConfig;
import org.quartz.SchedulerException;

/**
 * This page populates the "trigger" part of a job.
 */
public class SetSchedulePage extends AdminPage {
	/*
	 * The format in the job of a trigger:
	 *     <trigger>
	 *       <starttime>yyyy-mm-ddThh:mm:ss</starttime>
	 *       <schedtype>periodic</schedtype>
	 *       <period>daily</period>
	 *       <periodinterval>2</periodinterval>
	 *       <cronexp>some expression</cronexp>
	 *     </trigger>
	 *     
	 * The page params:
	 *     startdate
	 *     starthour
	 *     startminute
	 *     ampm
	 *     schedtype
	 *     period-interval
	 *     cronexp
	 *     
	 */

	private boolean redirect;
	private String jobName;
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
	
	@Override
	public void processPage(PageContext pageContext) {

		redirect = false;
		super.processPage(pageContext);

		try {
			JobScanner jobScanner = new JobScanner();
			jobName = pageContext.getRequest().getParameter("jobname");
			XMLConfig conf = jobScanner.getJobFromDisk(jobName);

			String next = super.getParam("next");
			if (next == null || next.length() == 0) {
				// we're hitting the page for the first time. populate it
				if (conf != null) {
					XMLConfig trig = conf.getChild("trigger");
					if (trig != null) {
						popParamsFromXML(trig);
					}

					// set some defaults here
					if (super.getParam("schedtype") == null) {
						super.setParam("schedtype", "ondemand");
					}

				}

			} else {
				// the user hit "next>>". save and redirect.

				try {
					saveJob(conf);
					redirect = true;

				} catch (ParseException e1) {
					super.handleError("Bad date format: " + e1.toString(), e1);
				} catch (Exception e) {
					super.handleError("Error (see log for details): " + e.toString(), e);
				}
			}

		} catch (Exception e) {
			super.handleError("Error on set schedule page", e);
		}
	}

	/**
	 * Fetch the params that define the schedule, add them to the config
	 * object and then write to disk.
	 * @throws ClassNotFoundException 
	 * @throws SchedulerException 
	 */
	private void saveJob(XMLConfig conf) throws IOException, ParseException, SchedulerException {
		conf.removeChild("trigger");
		XMLConfig trig = populateXMLFromParams();
		conf.addChild(trig);

		JobScanner jobScanner = new JobScanner();
		jobScanner.saveAndLoadJob(conf);
	}

	/**
	 * Get params out of the config and populate the page params, so the
	 * controls on the page are filled in with the previous values.
	 * @param conf
	 */
	private void popParamsFromXML(XMLConfig conf) {
		String fullDate = conf.getProperty("starttime",df.format(new Date()));
		if (fullDate != null) {
			String startDate = fullDate.substring(0, 10);
			String startHour = fullDate.substring(11, 13);
			String startMinute = fullDate.substring(14, 16);

			String ampm;
			int startHourInt = Integer.parseInt(startHour);
			if (startHourInt > 12) {
				ampm = "pm";
				startHourInt = startHourInt - 12;
				startHour = startHourInt + "";
				if (startHour.length() < 2)
					startHour = "0" + startHour;
			} else {
				ampm = "am";
			}

			super.setParam("startdate", startDate);
			super.setParam("starthour", startHour);
			super.setParam("startminute", startMinute);
			super.setParam("ampm", ampm);
		}
		
		super.setParam("period", conf.getProperty("period"));
		super.setParam("period-interval", conf.getProperty("period-interval"));
		super.setParam("schedtype", conf.getProperty("schedtype"));
		super.setParam("cronexp", conf.getProperty("cronexp"));
	}

	/**
	 * Extract the params from the page and populate a config trigger.
	 * @return a new config object
	 * @throws ParseException if the date format is bad
	 * @throws SchedulerException 
	 */
	private XMLConfig populateXMLFromParams() throws ParseException, SchedulerException {
		
		String startDate = super.getParam("startdate", "");
		String startHour = super.getParam("starthour", "00");
		String startMinute = super.getParam("startminute", "00");
		String ampm = super.getParam("ampm", "am");

		if ("pm".equals(ampm)) {
			int intStartHour = Integer.parseInt(startHour);
			startHour = (intStartHour + 12) + "";
		}

		String fullDate = df.format(new Date());
		if (startDate.length() > 0) {
			fullDate = startDate + " " + startHour + ":" + startMinute + ":00 " + ampm;
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
			df.parse(fullDate); // just validates the date format
		}

		XMLConfig trig = new XMLConfig();
		
		String cronexp = super.getParam("cronexp");
		PipelineScheduler.validateCronExpression(cronexp);
		
		addPropertyIfNotNull("period", super.getParam("period"), trig);
		addPropertyIfNotNull("period-interval", super.getParam("period-interval"), trig);
		addPropertyIfNotNull("schedtype", super.getParam("schedtype"), trig);
		addPropertyIfNotNull("cronexp", cronexp, trig);
		addPropertyIfNotNull("starttime", fullDate, trig);

		trig.setName("trigger");

		return trig;
	}


	/**
	 * Add property if not null.
	 */
	private void addPropertyIfNotNull(String name, String value, XMLConfig conf) {
		if (value != null && value.length() > 0) {
			conf.addProperty(name, value);
		}
	}
	

	public boolean redirect() {
		return redirect;
	}

	public String getJobName() {
		return jobName;
	}

}
