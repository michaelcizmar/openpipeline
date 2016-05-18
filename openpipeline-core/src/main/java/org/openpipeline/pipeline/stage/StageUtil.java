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
package org.openpipeline.pipeline.stage;

import java.io.IOException;

import javax.servlet.ServletRequest;

import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.util.XMLConfig;

public class StageUtil {
	
	/**
	 * Extract the "jobname" parameter from the request and fetch the job file from
	 * disk.
	 * @param request
	 * @return the job file as an XMLConfig object
	 * @throws IOException
	 */
	public static XMLConfig getJobFromRequest(ServletRequest request) throws IOException {
		JobScanner jobScanner = new JobScanner();
		String jobName = request.getParameter("jobname");
		return jobScanner.getJobFromDisk(jobName);
	}

}
