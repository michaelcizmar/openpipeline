package org.openpipeline.server.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openpipeline.server.Server;
import org.openpipeline.util.FileUtil;


/**
 * Launches an OpenPipeline server.
 */
public class OpenPipelineLauncher {
	
	/* This class is separate from JettyLauncher in case
	 * we want to run OP in a different app server.
	 */

	private Start start = new Start();

	/**
	 * Creates a new instance of this class and calls launch().
	 */
	public static void main(String[] args) throws Exception {
		OpenPipelineLauncher launcher = new OpenPipelineLauncher();
		launcher.launch(args);
	}

	
	/**
	 * Loads default config files and launches Jetty;
	 */
	public void launch(String[] args) throws Exception {
		loadDefaultConfigFiles();
		launchJetty(args);
	}
	
	/**
	 * Copies missing config files into the /config directory. Loads them from the .jar.
	 * @throws IOException 
	 */
	public void loadDefaultConfigFiles() throws IOException {
		loadDefaultConfigFile("/openpipeline_default_config_files", "logback.xml");
		//loadDefaultConfigFile("/openpipeline_default_config_files", "logback-access.xml");
		loadDefaultConfigFile("/openpipeline_default_config_files", "standard-pipelines.xml");
		loadDefaultConfigFile("/openpipeline_default_config_files", "webdefault.xml");
	}
	
	/**
	 * Using the current classloader, looks for the specified file 
	 * in the /config dir, and if it doesn't exists, tries to copy
	 * it there from the /resources/default_config_files dir.
	 * @param filename
	 * @throws IOException 
	 */
	public void loadDefaultConfigFile(String resourceDir, String filename) throws IOException {
		// get the home directory without initializing the server
		String homeDir = Server.findHomeDir();
		File file = new File(homeDir, "config/" + filename);
		if (!file.exists()) {
			InputStream in = this.getClass().getResourceAsStream(resourceDir + "/" + filename);
			FileUtil.writeStreamToFile(in, file);
		}
	}

	/**
	 * Just launches Jetty.
	 */
	public void launchJetty(String[] args) throws Exception {
		start.populateWebappsList(args);
		start.start();
	}
	
	public Start getStart() {
		return start;
	}
	

}
