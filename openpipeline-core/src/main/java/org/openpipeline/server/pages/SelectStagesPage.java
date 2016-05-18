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

import java.io.File;
import java.util.Iterator;
import java.util.UUID;

import javax.servlet.jsp.PageContext;

import org.openpipeline.pipeline.stage.Stage;
import org.openpipeline.pipeline.stage.StageFactory;
import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.server.Server;
import org.openpipeline.util.FastStringBuffer;
import org.openpipeline.util.XMLConfig;

public class SelectStagesPage extends AdminPage {

	/* This class adds this section to the job file:
	 * 
	 * <stages>
	 * 
	 *   <stage>
	 *     <classname>org.openpipeline.pipeline.stages.SimpleTokenizerStage</classname>
	 *     <myparam>some param</myparam>
	 *     
	 *     <!-- these are child stages -->
	 *     <stage>
	 *       <classname>org.openpipeline.pipeline.stages.AChildStage</classname>
	 *     </stage>
	 *     <stage>
	 *       <classname>org.openpipeline.pipeline.stages.AnotherChildStage</classname>
	 *     </stage>
	 *     
	 *   </stage>
	 * 	
	 *   <stage>
	 *     <classname>org.openpipeline.pipeline.stages.TheNextStageInTheList</classname>
	 *     <myparam>some param</myparam>
	 *   </stage>
	 * <stages>
	 * 
	 * The sequence of stages is important.
	 */

	private static final String STANDARD_PIPELINES_FILENAME = "config/standard-pipelines.xml";
	
	private boolean redirect = false;
	private String jobName;
	private XMLConfig jobXML;
	
	
	//private String pipelineOptions;
	//private String existingPipelineName;

	/**
	 * Process a request for the select_stages.jsp page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {

		super.processPage(pageContext);

		try {

			jobName = pageContext.getRequest().getParameter("jobname");
			
			JobScanner jobScanner = new JobScanner();
			jobXML = jobScanner.getJobFromDisk(jobName);
			
			String next = super.getParam("next");
			if (next != null && next.length() > 0) {
				// if the user hit "next>>", save and redirect.
				
				String selectedStagesParam = super.getParam("selected_stages_param", "");
				String [] selectedStages = null;
				if (selectedStagesParam.length() > 0) {
					selectedStages = selectedStagesParam.split("\\|"); 
				}
				
				// find existing stages with the name. if found, put in the
				// right sequence. if not, add.
				XMLConfig oldStagesNode = jobXML.getChild("stages");
				if (oldStagesNode == null) {
					oldStagesNode = new XMLConfig(); // just a dummy node
				}
				XMLConfig newStagesNode = new XMLConfig();
				newStagesNode.setName("stages");
				
				if (selectedStages != null) {
					for (int i = 0; i < selectedStages.length; i++) {
						
						// the format is classname + "_" + uuid
						// an existing stage should have an id. a new stage won't
						String ss = selectedStages[i];
						String className;
						String id;
						if (ss.indexOf("_") > -1) {
							String [] split = ss.split("_");
							className = split[0];
							id = split[1];
						} else {
							className = ss;
							id = UUID.randomUUID().toString();
						}
						
						XMLConfig oldStage = findStage(className, id, oldStagesNode);
						if (oldStage == null) {
							XMLConfig newStage = new XMLConfig();
							newStage.setName("stage");
							newStage.setProperty("id", id);
							newStage.setProperty("classname", className);
							newStagesNode.addChild(newStage);
							
						} else {
							// just transfer the old stage; preserves config values
							newStagesNode.addChild(oldStage);
						}
					}
				}

				jobXML.removeChild("stages");
				jobXML.addChild(newStagesNode);

				jobScanner.saveAndLoadJob(jobXML);

				redirect = true;
			}

		} catch (Exception e) {
			super.handleError("Error in SelectStagesPage: " + e.toString(), e);
		}
	}

	

	public String getStandardPipelineOptions() throws Exception {
		
		FastStringBuffer outputBuf = new FastStringBuffer();
		
		// fetch standard pipelines
		File file = new File(Server.getServer().getHomeDir(), STANDARD_PIPELINES_FILENAME);
		if (file.exists()) {
			XMLConfig standardPipelines = new XMLConfig();
			standardPipelines.load(file);
			
			for (XMLConfig child: standardPipelines.getChildren()) {
				if ("stages".equals(child.getName())) {

					String pipelineName = child.getProperty("pipeline-name");
					
					String stageOptions = getStageOptions(child);
					
					outputBuf.append("<option title=\"");
					outputBuf.appendWithXMLEncode(stageOptions);
					outputBuf.append("\">");
					outputBuf.append(pipelineName);
					outputBuf.append("</option>");
				}
			}
		}
		return outputBuf.toString();
	}
	
	
	/**
	 * Roll through a block of stages and populate < option> tags for the 
	 * selected stages box.
	 */
	private String getStageOptions(XMLConfig stages) throws Exception {
		FastStringBuffer buf = new FastStringBuffer();
		for (XMLConfig child: stages.getChildren()) {
			if (!"stage".equals(child.getName())) {
				continue;
			}
			
			String classname = child.getProperty("classname");
			String id = child.getProperty("id");
			if (id == null) {
				// could get here if we're getting options for a standard pipeline
				id = UUID.randomUUID().toString();
			}
			
			Stage stage = StageFactory.getStage(classname);
			
			String dispName = stage.getDisplayName();
			buf.append("<option value='");
			buf.append(classname);
			buf.append("_");
			buf.append(id);
			buf.append("'>");
			buf.append(dispName);
		}
		return buf.toString();
	}
	

	/**
	 * Find the stage with the specified classname and id and return it.
	 */
	private XMLConfig findStage(String className, String id, XMLConfig oldStagesNode) {
		for (XMLConfig stage:  oldStagesNode.getChildren()) {
			String classProp = stage.getProperty("classname");
			String idProp = stage.getProperty("id");
			if (className.equals(classProp) && id.equals(idProp)) {
				return stage;
			}
		}
		return null;
	}

	public String getJobName() {
		return jobName;
	}

	public boolean redirect() {
		return redirect;
	}

	public String getAvailStageOptions() {
		FastStringBuffer buf = new FastStringBuffer();
		Iterator it = StageFactory.getStages();
		while (it.hasNext()) {
			Stage stage = (Stage) it.next();
			
			String dispName = stage.getDisplayName();
			String desc = stage.getDescription();
			String className = stage.getClass().getName();
			
			buf.append("<option value=\"");
			buf.append(className);
			buf.append("\" title=\"");
			buf.append(desc);
			buf.append("\">");
			buf.append(dispName);
		}
		return buf.toString();
	}

	
	
	public String getExistingStageOptions() throws Exception {
		
		XMLConfig existingStages = jobXML.getChild("stages");
		if (existingStages == null) {
			
			// if there are no existing stages, look in standard pipelines for
			// a pipeline named "Default" and prepopulate the list

			File file = new File(Server.getServer().getHomeDir(), STANDARD_PIPELINES_FILENAME);
			if (file.exists()) {
				XMLConfig standardPipelines = new XMLConfig();
				standardPipelines.load(file);
				
				for (XMLConfig child: standardPipelines.getChildren()) {
					if ("stages".equals(child.getName())) {
						String pipelineName = child.getProperty("pipeline-name");
						if ("Default".equalsIgnoreCase(pipelineName)) {
							return getStageOptions(child);
						}
					}
				}
			}
			
			// if we get here there are no exising stages and no defaults
			existingStages = new XMLConfig(); // just a dummy node
		}
		return getStageOptions(existingStages);
	}
	

	
}
