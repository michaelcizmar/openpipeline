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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import org.openpipeline.pipeline.stage.Stage;
import org.openpipeline.pipeline.stage.StageFactory;
import org.openpipeline.pipeline.stage.StageUtil;
import org.openpipeline.scheduler.JobScanner;
import org.openpipeline.util.XMLConfig;

public class ConfigureStagesPage extends AdminPage {
	
	/*
	 * Each subpage runs independently. This page just assembles the
	 * list of subpages to show.
	 */

	private boolean redirect = false;
	private String jobName;
	private XMLConfig jobXML;
	private List<String> stageLinks = new ArrayList();
	private String currStageId;
	private String currConfigPage;

	/**
	 * Process a request for the configure_stages.jsp page in the admin interface.
	 */
	public void processPage(PageContext pageContext) {
		
		super.processPage(pageContext);

		try {
			
			jobName = pageContext.getRequest().getParameter("jobname");
			jobXML = StageUtil.getJobFromRequest(pageContext.getRequest());
			
			String oldStageId = super.getParam("currstageid");
			String newStageId = super.getParam("newstageid");
			String redirectStr = super.getParam("redirect", "false");
			
			saveOldPage(oldStageId);
			loadNewPage(newStageId);
			
			populateStageLinks(newStageId);

			// if the user hit Next >>
			if (redirectStr.equals("true")) {
				redirect = true;
			}

		} catch (Exception e) {
			super.handleError("Error in configure stages page " + e.toString(), e);
		}
	}
	
	/**
	 * Return a reference to a stage in the jobxml. Can be modified
	 * and saved.
	 * @param stageNum
	 * @return
	 * @throws Exception 
	 */
	private XMLConfig getStageXML(String stageId) throws Exception {
		XMLConfig stagesNode = jobXML.getChild("stages");
		for (XMLConfig child: stagesNode.getChildren()) {
			String id = child.getProperty("id");
			if (id.equals(stageId)) {
				return child;
			}
		}
		throw new Exception("StageId not found: " + stageId);
	}
	
	/**
	 * Get the child nodes of the stages tag.
	 * @return
	 */
	private List getStagesChildren() {
		XMLConfig stagesNode = jobXML.getChild("stages");
		if (stagesNode == null) {
			return Collections.EMPTY_LIST;
		}
		return stagesNode.getChildren();
	}
	

	/**
	 * Load a new config page: populate params, set currStageId, currConfigPage.
	 * @param newStageId
	 * @throws Exception
	 */
	private void loadNewPage(String newStageId) throws Exception {
		currStageId = newStageId;
		if (newStageId == null || newStageId.trim().length() == 0) {
			currConfigPage = null;
			return;
		}
		
		XMLConfig newXML = getStageXML(newStageId);
		String classname = newXML.getProperty("classname");
		Stage stage = StageFactory.getStage(classname);
		Map params = super.getParamMap();
		
		params.clear();
		stage.loadParamsFromXML(params, newXML);

		currConfigPage = stage.getConfigPage(); 
	}

	/**
	 * Save the params from the config page into the job.
	 * @param oldStageId
	 * @throws Exception
	 */
	private void saveOldPage(String oldStageId) throws Exception {
		
		if (oldStageId == null || oldStageId.trim().length() == 0) {
			return;
		}
		
		XMLConfig oldXML = getStageXML(oldStageId);
		String classname = oldXML.getProperty("classname");
		Stage stage = StageFactory.getStage(classname);
		
		Map<String, String[]> params = super.getParamMap();
		params.remove("jobname");
		params.remove("currstageid");
		params.remove("newstageid");
		params.remove("redirect");

		stage.saveParamsToXML(params, oldXML);
		
		JobScanner jobScanner = new JobScanner();
		jobScanner.saveAndLoadJob(jobXML);
	}

	/**
	 * Create the array of StageInfo objects. Also set the value of currConfigPage.
	 */
	private void populateStageLinks(String newStageId) throws Exception {
		
		List<XMLConfig> children = getStagesChildren();
		for (XMLConfig child: children) {
			if (!"stage".equals(child.getName())) {
				continue;
			}

			String classname = child.getProperty("classname");
			String id = child.getProperty("id");
			
			Stage stage = StageFactory.getStage(classname);

			String link = stage.getDisplayName();

			if (id.equals(newStageId)) {
				link = "<b>" + link + "</b>";
			}
			
			String configPage = stage.getConfigPage();
			if (configPage != null) {
				link += " <a style='font-size: 9px;' href='javascript:refresh(\"" + id + "\", false)'>(config)</a>";
			}

			stageLinks.add(link);
		}
	}

	public List getStageLinks() {
		return stageLinks;
	}
	
	public String getCurrStageId() {
		if (currStageId == null) {
			return "";
		}
		return currStageId;
	}
	
	public String getCurrConfigPage() {
		return currConfigPage;
	}
	
	public String getJobName() {
		return jobName;
	}

	public boolean redirect() {
		return redirect;
	}


}
