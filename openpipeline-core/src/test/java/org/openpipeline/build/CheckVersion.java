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
package org.openpipeline.build;


/**
 * OBSOLETE
 * Make sure that the pom and the Server versions match.
 * Don't overwrite existing version. Force a manual delete first.
 * Return the version number.
 * Return the svn rev number.
 */
public class CheckVersion { //extends Task {
	/*
	 * not used any more
	 * depends on:
	 * 
	 * 
			<dependency>
			<groupId>org.apache.ant</groupId>
			<artifactId>ant</artifactId>
			<version>1.7.1</version>
			<scope>test</scope>
		</dependency>
			<dependency>
			<groupId>org.tmatesoft.svnkit</groupId>
			<artifactId>svnkit</artifactId>
			<version>1.3.4</version>
			<scope>test</scope>
		</dependency>
	
	
	
	private String pomVersion;
	private String versionSource; 	// optional
	private String artifactId;

	
	public void setPomVersion(String pomVersion) {
		this.pomVersion = pomVersion;
	}

	public void setVersionSource(String versionSource) {
		this.versionSource = versionSource;
	}
	
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	public void execute() throws BuildException {
		
		try {
			
			File baseDir = getProject().getBaseDir();
			
			// compare version numbers
			// strip SNAPSHOT
			int pos = pomVersion.indexOf("-SNAPSHOT");
			if (pos > -1) {
				pomVersion = pomVersion.substring(0, pos);
			}
			
			String vers = getVersionFromSource();
			if (vers != null && !vers.equals(pomVersion)) {
				throw new BuildException(versionSource + ".VERSION does not match POM version. ("
						+ vers + "!=" + pomVersion + ")");
			}
			
			getProject().setProperty("vers", pomVersion);
			
			// prevent overwrites
			if (artifactId == null) {
				throw new BuildException("artifactId not set");
			}
			String appName = artifactId + "-" + vers;
			File releaseDir = new File(baseDir.getParentFile().getParentFile(), "releases/" + appName);
			if (releaseDir.exists()) {
				throw new BuildException("Release dir already exists. Please delete manually. dir=" + releaseDir);
			}
			
			SVNClientManager cm = SVNClientManager.newInstance();
			SVNStatus status = cm.getStatusClient().doStatus(baseDir, false);
			SVNRevision rev = status.getCommittedRevision();
			String svnrev = rev.getNumber() + "";
			
			getProject().setProperty("svnrev", svnrev);

			
		} catch (Exception e) {
			
			throw new BuildException(e);
		}
	}

	private String getVersionFromSource() throws Exception {
		if (versionSource == null) {
			return null;
		}
		
		File baseDir = getProject().getBaseDir();
		
		File file = new File(baseDir, versionSource);
		FileReader r = new FileReader(file);
		BufferedReader br = new BufferedReader(r);
		while (true) {
			String line = br.readLine();
			if (line == null) {
				break;
			}
			
			int pos = line.indexOf("VERSION");
			if (pos > -1) {
				int start = line.indexOf('"') + 1;
				int end = line.lastIndexOf('"');
				return line.substring(start, end);
			}
		}
		
		throw new Exception("version not found in " + file.toString());
	}
*/
}
