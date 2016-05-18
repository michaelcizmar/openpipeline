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

import java.util.List;

import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;
import org.openpipeline.util.XMLConfig;

/**
 * A wrapper around the item processing pipeline. Handles the creation, initialization, flushing,
 * and closing of the stages in the pipeline. Handles pushing each item down the pipeline.
 */
public class StageList {

	// the first stage in the linked list
	private Stage head;
	private Connector connector;

	/**
	 * Creates and assembles a pipeline of stages using the specified
	 * configuration.
	 * @param jobParams config object that contains information necessary to create and configure the pipeline
	 * @throws PipelineException 
	 */
	public void createPipeline(XMLConfig jobParams) throws PipelineException {

		// see SelectStagesPage for the layout of a <stages> section.

		head = null;
		Stage current = null;

		try {
		XMLConfig stages = jobParams.getChild("stages");
		if (stages != null) {
			for (XMLConfig stageConf: stages.getChildren()) { 
				if (!"stage".equals(stageConf.getName())) {
					continue;
				}

				String className = stageConf.getProperty("classname");
				Stage stage = StageFactory.getStage(className);
				stage.setParams(stageConf);
				stage.setStageList(this);

				if (current == null) {
					head = stage;
					current = stage;
				} else {
					current.setNextStage(stage);
					current = stage;
				}
				// TODO implement child stages

			}
		}
		
		} catch (Exception e) {
			throw new PipelineException(e);
		}
	}

	/**
	 * Call the initialize() method on each stage.
	 */
	public  void initialize() throws PipelineException {
		initializeInternal(head);
	}
	
	private void initializeInternal(Stage stage) throws PipelineException {
		if (stage == null) {
			return;
		}
		stage.initialize();
		List list = stage.getChildStages();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Stage child = (Stage) list.get(i);
				initializeInternal(child);
			}
		}
		initializeInternal(stage.getNextStage());
	}

	/**
	 * Call the flush() method on each stage in sequence.
	 * @throws PipelineException 
	 */
	public void flush() throws PipelineException {
		flushInternal(head);
	}
	
	public void flushInternal(Stage stage) throws PipelineException {
		if (stage == null) {
			return;
		}
		stage.flush();
		List list = stage.getChildStages();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Stage child = (Stage) list.get(i);
				flushInternal(child);
			}
		}
		flushInternal(stage.getNextStage());
	}

	/**
	 * Call the close() method on each stage in sequence.
	 * @throws PipelineException 
	 */
	public void close() throws PipelineException {
		closeInternal(head);
	}
	
	private void closeInternal(Stage stage) throws PipelineException {
		if (stage == null) {
			return;
		}
		stage.close();
		List list = stage.getChildStages();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Stage child = (Stage) list.get(i);
				closeInternal(child);
			}
		}
		closeInternal(stage.getNextStage());
	}

	/**
	 * Process an item by pushing it into the first stage in the list. 
	 * @param item the item to process
	 * @throws PipelineException
	 */
	public void processItem(Item item) throws PipelineException {
		if (head != null) {
			head.processItem(item);
		}
	}
	
	/**
	 * Return true if this pipeline is empty.
	 * @return true if no stages have been defined
	 */
	public boolean isEmpty() {
		return head == null;
	}

	/**
	 * Set the parent connector for this list of stages.
	 * @param connector a connector
	 */
	public void setConnector(Connector connector) {
		this.connector = connector;
	}

	/**
	 * Get the parent connector for this list of stages.
	 * @return a connector
	 */
	public Connector getConnector() {
		return connector;
	}
}
