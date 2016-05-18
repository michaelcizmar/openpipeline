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
package org.openpipeline.pipeline.docfilter;

import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;

/**
 * Implementation of the {@link DocFilter} class for files that should
 * not be parsed, but only have their filenames and other metadata stored.
 */
public class MetadataOnlyFilter extends DocFilter {

	@Override
	public void processItem(Item item) throws PipelineException {
		// do nothing; the DocFilterStage adds the metadata
		super.pushItemDownPipeline(item);
	}
	
	@Override
	public String[] getDefaultExtensions() {
		String [] exts = { };
		return exts;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		String [] mimeTypes = { };
		return mimeTypes;
	}

	@Override
	public String getDescription() {
		return "Does not parse the file; designed for passing on filenames and metadata only.";
	}

	@Override
	public String getDisplayName() {
		return "MetadataOnlyFilter";
	}
	
	@Override
	public String getDocType() {
		return "meta";
	}

	
}
