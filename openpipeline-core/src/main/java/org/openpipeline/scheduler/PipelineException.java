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
package org.openpipeline.scheduler;


/**
 * A checked exception that indicates a recoverable error in the pipeline; it means
 * that this particular item won't be processed, but the connector should not
 * stop running. An unchecked exception indicates a fatal error and the
 * connector should stop.
 */
public class PipelineException extends Exception {

	private static final long serialVersionUID = 1L;

	public PipelineException() {
		super();
	}

	public PipelineException(String message) {
		super(message);
	}

	public PipelineException(String message, Throwable cause) {
		super(message, cause);
	}

	public PipelineException(Throwable cause) {
		super(cause);
	}
}
