package org.openpipeline.pipeline.stage;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;

/**
 * Use this stage to generate synthetic data.
 */
public class ItemReplicator extends Stage {
	private int count = 30;

	public void initialize() {
		if (params != null) {
			count = params.getIntProperty("replication-cound", 30);
		}
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		for (int i = 0; i < count; i++) {
			
			if (nextStage != null) {
				/*
				 * It is the responsibility of the subsequent stages
				 * to assign an item id.
				 */
				item.setItemId(null);
				nextStage.processItem(item);
			}
		}
	}

	@Override
	public String getDescription() {
		return "Use this stage to generate synthetic data by replicate the same item multiple times.";
	}

	@Override
	public String getDisplayName() {
		return "Item Replicator";
	}
	
	@Override
	public String getConfigPage() {
		return "stage_item_replicator.jsp";
	}
}
