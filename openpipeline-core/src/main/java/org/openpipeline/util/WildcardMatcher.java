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
package org.openpipeline.util;

import java.util.List;
import java.util.regex.Pattern;


/**
 * Matches a String against a list of expressions with wildcards. There are include 
 * and exclude expressions. If the String matches an include, and doesn't match
 * an exclude, then it passes. Typically used for matching file paths against
 * lists of filenames and directories that should and should not be crawled.
 * <p>
 * For example, if the includes are {"*.pdf", "*.doc"} and the excludes are {"/temp/*"},
 * the isIncluded() method will return true for all file paths for pdfs and docs, but
 * exclude files in the /temp directory.
 */
public class WildcardMatcher {

	private Pattern[] includes;
	private Pattern[] excludes;
	private boolean caseSensitive;

	public WildcardMatcher(){
	}
	
	/**
	 * Set a list of patterns with wildcards that define the Strings that
	 * should be included.
	 * @param includes a List of Strings
	 */
	public void setIncludePatterns(List includes){
		this.includes = this.getPatterns(includes);
	}
	
	/**
	 * Set a list of patterns with wildcards that define the Strings that
	 * should be excluded.
	 * @param excludes a List of Strings
	 */
	public void setExcludePatterns(List excludes){
		this.excludes = this.getPatterns(excludes);
	}
	
	/**
	 * Set this true to do case-sensitive matching. Defaults to false;
	 * include and exclude filters are applied without regard to case.
	 * @param caseSensitive true if case sensitive matching enabled
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Return true if the String matches at least one of the include patterns
	 * and none of the exclude patterns. If the list of include patterns
	 * is empty then the String will not be excluded for failing to
	 * match an include pattern.
	 * @return true if the String should be included
	 */	
    public boolean isIncluded(String str) {
    	if (str == null) {
    		return false;
    	}
    	
        // test excludes first
        if (excludes != null) {
            for (int i = 0; i < excludes.length; i++) {
                if (excludes[i].matcher(str).matches()) {
                    return false;
                }
            }
        }
        
        // if include patterns exist, must match at least one of them       
        if (includes != null && includes.length > 0) {
            for (int i = 0; i < includes.length; i++) {
                if (includes[i].matcher(str).matches()) {
                    return true;
                }
            }
            return false;
        }       
       
        return true;
    }
	

	/**
	 * Compile the wildcard expressions into regular expression Patterns.
	 */
	private Pattern[] getPatterns(List list){
		if (list == null || list.size() == 0){
			return null;
		} else {
			Pattern[] patterns = new Pattern[list.size()];
			for (int i = 0; i < list.size(); i++) {
				String patStr = (String)list.get(i);
				
				int flags = 0;
				if (!caseSensitive) {
					flags = Pattern.CASE_INSENSITIVE;
				}
				
				patterns[i] = Pattern.compile(Util.wildcardToRegexp(patStr), flags);
			}
			return patterns;
		}
	}
}
