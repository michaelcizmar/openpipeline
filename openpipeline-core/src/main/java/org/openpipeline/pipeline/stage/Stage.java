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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.server.pages.AdminPage;
import org.openpipeline.util.XMLConfig;

/**
 * The abstract base class for all stages. A stage is a step in a series of
 * operations on an Item. Most implementations of this class will not implement 
 * all methods below; not all classes will need to initialize(), for example.
 * The methods that must be implemented are marked abstract.
 * <p>
 * <b>Important:</b> When implementing the processItem() method, be sure to 
 * include this code at the bottom of the method:
 * <pre><code>
 * if (nextStage != null) {
 *     nextStage.processItem(item);
 * }
 * </code></pre>
 * Without it, the item will not be pushed down the pipeline.
 */
public abstract class Stage {
	protected Stage nextStage;
	protected XMLConfig params;
	protected ArrayList<Stage> childStages;
	protected StageList stageList;

	/**
	 * Configure this stage. For example, if this were an entity extraction stage,
	 * a parameter could specify which entities to extract.
	 * @param params parameters defined in an XML file
	 */
	public void setParams(XMLConfig params) {
		this.params = params;
	}

	/**
	 * Set the next stage that this stage should call after this stage has performed its task.
	 * @param stage the next stage in the list
	 */
	public void setNextStage(Stage stage) {
		this.nextStage = stage;
	}

	/**
	 * Return the next stage in the linked list of stages.
	 * @return the next stage, or null if none has been set.
	 */
	public Stage getNextStage() {
		return nextStage;
	}

	/**
	 * Add a sub-stage to this stage. This stage will push the item into child stages
	 * as appropriate, according to its own logic. Once that process is complete, this
	 * stage will push the item into the stage defined by nextStage.
	 * @param stage a child stage
	 */
	public void addChildStage(Stage stage) {
		if (!childStagesAccepted()) {
			throw new UnsupportedOperationException("This stage does not accept child stages");
		}
		if (childStages == null) {
			childStages = new ArrayList();
		}
		childStages.add(stage);
	}

	/**
	 * Returns true if this stage can process child stages. Defaults to
	 * false unless overridden.
	 * @return true if child stages are processed
	 */
	public boolean childStagesAccepted() {
		return false;
	}

	/**
	 * Returns a list of any child stages, or null if none have been added.
	 * @return a list of child stages
	 */
	public List<Stage> getChildStages() {
		if (childStages == null) {
			return Collections.EMPTY_LIST;
		}
		return childStages;
	}

	/**
	 * Get the name of the .jsp page in the admin webapp that 
	 * configures this stage.
	 * @return the file name of a page, for example, "stage_mystage.jsp", or null if no page necessary
	 */
	public String getConfigPage() {
		return null;
	}

	/**
	 * Perform any necessary initialization.
	 */
	@SuppressWarnings("unused")
	public void initialize() throws PipelineException {
	}

	/**
	 * If this stage accumulates items or sends them down a channel,
	 * this method flushes them.
	 */
	@SuppressWarnings("unused")
	public void flush() throws PipelineException {
	}

	/**
	 * Closes the stage and releases any resources. A stage may not
	 * be reopened/initialized.
	 */
	@SuppressWarnings("unused")
	public void close() throws PipelineException {
	}

	/**
	 * Return the name of the Stage to display in the Admin UI, for example,
	 * "My Stage"
	 */
	public String getDisplayName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Returns a brief description of what this class does, for use in the admin app.
	 */
	public String getDescription() {
		return getDisplayName();
	}

	/**
	 * Perform some operation on an item. This is where the stage does its work.
	 * @param item the item to process
	 * @throws PipelineException a recoverable exception caused by this stage. 
	 * Unrecoverable errors should be thrown as unchecked exceptions
	 */
	public abstract void processItem(Item item) throws PipelineException;

	/**
	 * Saves the params to an XML config object. Override this method 
	 * to implement more complex mappings on a stage config page.
	 * @param params the object that supplies the params
	 * @param xml the XML with the config info
	 */
	public void saveParamsToXML(Map<String, String[]> params, XMLConfig xml) {
		AdminPage.convertParamsToXMLConfig(params, xml);
	}

	/**
	 * Loads params from an XML config object. Override this method 
	 * to implement more complex mappings on a stage config page.
	 * @param params the object that receives the params
	 * @param xml the XML that supplies the params
	 */
	public void loadParamsFromXML(Map<String, String[]> params, XMLConfig xml) {
		AdminPage.convertXMLConfigToParams(xml, params);
	}

	/**
	 * Pushes an item into the next stage in the pipeline. Each stage's processItem()
	 * method should call <code>super.pushItemDownPipeline(item)</code> when
	 * it is finished processing the item (unless it wants processing to terminate
	 * for that item).
	 * @param item the item to be processed
	 * @throws PipelineException
	 */
	public void pushItemDownPipeline(Item item) throws PipelineException {
		if (nextStage != null) {
			nextStage.processItem(item);
		}
	}

	/**
	 * Returns the list of all the stages in the pipeline of
	 * which this stage is a part.
	 * @return the parent stage list
	 */
	public StageList getStageList() {
		return stageList;
	}
	
	/**
	 * Set the parent stage list, that is, the pipeline of
	 * which this stage is a part.
	 * @param stageList the parent stage list
	 */
	public void setStageList(StageList stageList) {
		this.stageList = stageList;
	}
	
	/**
	 * Report an error that terminates the processing of the current item,
	 * but does not terminate the connector.
	 * @param msg the error message
	 */
	public void error(String msg) {
		error(msg, null);
	}
	
	/**
	 * Report an error that terminates the processing of the current item,
	 * but does not terminate the connector.
	 * @param msg the error message
	 * @param t any associated exception
	 */
	public void error(String msg, Throwable t) {
		if (stageList != null && stageList.getConnector() != null) {
			if (t == null) {
				stageList.getConnector().error(msg);
			} else {
				stageList.getConnector().error(msg, t);
			}
		}
	}
	
	/**
	 * Report a warning which does not terminate the processing
	 * of the current item.
	 * @param msg the warning message
	 */
	public void warn(String msg) {
		if (stageList != null && stageList.getConnector() != null) {
			stageList.getConnector().warn(msg);
		}
	}
	
	/**
	 * Report a warning which does not terminate the processing
	 * of the current item.
	 * @param msg the warning message
	 * @param t any associated exception
	 */
	public void warn(String msg, Throwable t) {
		if (stageList != null && stageList.getConnector() != null) {
			if (t == null) {
				stageList.getConnector().warn(msg);
			} else {
				stageList.getConnector().warn(msg, t);
			}
		}
	}
	
	
}
