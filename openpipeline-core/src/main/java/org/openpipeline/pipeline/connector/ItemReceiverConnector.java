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
package org.openpipeline.pipeline.connector;

import java.util.concurrent.CountDownLatch;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;

/**
 *	This connector, pushes item received from one or more remote machines down a pipeline. 
 *	Items can be processed either in single-threaded or multi-threaded mode.
 *	@see org.openpipeline.pipeline.stage.ItemSender
 */
public class ItemReceiverConnector extends Connector {
	/*
	 * TODO: Once we figure out how to get a reference to  
	 * a non-executing job in PipelineScheduler, we
	 * can eliminate the code( CountDownLatch) 
	 * which requires this connector
	 * to be live for processing incoming items.
	 * 
	 * TODO: once pipeline pools are implemented, make
	 * sure this connector uses one. will improve concurrency.
	 * 
	 * See:
	 * http://stackoverflow.com/questions/1625666/publish-jax-ws-endpoint-with-embedded-jetty-7
	 * 
	 */
	
	private CountDownLatch interruptSignal = new CountDownLatch(1);
	
	public void execute() {
		super.setLastMessage("Service Started");

		//	Wait for the service to stop.
		try {
			this.interruptSignal.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		super.setLastMessage("Service Stopped");
	}

	/**
	 * Pushes item down a pipeline.
	 * @param item
	 * @throws PipelineException
	 */
	public void processItem(Item item) throws PipelineException {
		synchronized (this) {
			// pipelines are single-threaded
			// later, fix this when pipeline pools are implemented
			super.getStageList().processItem(item);
		}
	}

	public String getDescription() {
		return "Receives one or more items from remote machines.";
	}

	public String getDisplayName() {
		return "Item Receiver";
	}

	public String getLogLink() {
		return "log_viewer.jsp";
	}

	public String getPageName() {
		return "connector_item_receiver.jsp";
	}

	public String getShortName() {
		return "ItemReceiver";
	}

	public void interrupt() {
		super.interrupt();
		this.interruptSignal.countDown();
	}



}
