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

/**
 * Constants that define lists of annotations that are attached to Nodes in an Item.
 */
public interface AnnotationConstants {
	public final static String TOKEN_ANNOTATIONS = "token";
	public final static String SENTENCE_ANNOTATIONS = "sentence";
	public final static String SKIP_ANNOTATIONS = "skip";
	public final static String SPECIALTERM_ANNOTATIONS = "special";
	public final static String ATTACHMENTS_ANNOTATION = "attachments";
	public final static String CLEANTEXT = "cleantext"; // text cleaned of html
	public final static String HTML_ANALYZER = "analyzedhtml";
	
	
}
