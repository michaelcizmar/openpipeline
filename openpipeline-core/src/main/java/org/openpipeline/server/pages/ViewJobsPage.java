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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.scheduler.PipelineScheduler;

public class ViewJobsPage extends AdminPage {
	private List jobs;

	public void processPage(PageContext pageContext) {

		super.processPage(pageContext);
		try {

			PipelineScheduler sched = PipelineScheduler.getInstance();

			ServletRequest request = pageContext.getRequest();
			String action = request.getParameter("action");
			if (action != null) {
				String jobName = request.getParameter("jobname");
				//String groupName = request.getParameter("groupname");

				if ("stop".equals(action)) {
					sched.interruptJob(jobName);//, groupName);
					super.addMessage("Interrupt request sent to " + jobName);

				} else if ("start".equals(action)) {
					if (sched.startJob(jobName)) {
						super.addMessage("Start request sent to " + jobName);
						Thread.sleep(500); // half a sec to start so the display will be current
					} else {
						super.addMessage("Job already running: " + jobName);
					}

				} else if ("remove".equals(action)) {
					sched.removeJob(jobName);//, groupName);
					
					JobScanner js = new JobScanner();
					if (js.removeJob(jobName)) {
						super.addMessage("Job removed: " + jobName);
					} else {
						super.addMessage("Job failed to delete from disk: " + jobName);
					}
				}
			}

			jobs = sched.getJobs();

		} catch (Exception e) {
			super.handleError("Error in PipelineScheduler. See log for details.", e);
		}
	}

	public List getJobs() {
		if (jobs == null) {
			// could happen if there is an exception above
			return new ArrayList();
		}
		return jobs;
	}



}
