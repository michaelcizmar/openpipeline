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
package org.openpipeline.pipeline.item;

/**
 * This class documents some common attribute names for use in items. These
 * name are not enforced -- there is no rule that says a title must be named
 * "title" -- but adopting these names as a convention is helpful.
 */
public class StandardAttributeNames {
	public static final String ITEM_ID = "item_id";
	public static final String DOCTYPE = "doctype";
	public static final String TITLE = "title";
	public static final String TEXT = "text";
	public static final String URL = "url";
	public static final String LAST_UPDATE = "lastupdate"; // should be in millis
	public static final String FILE_SIZE = "filesize"; // should be in bytes or chars
	public static final String ENCODING = "encoding";
}
