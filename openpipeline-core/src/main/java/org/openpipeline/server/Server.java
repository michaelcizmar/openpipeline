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
package org.openpipeline.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.openpipeline.logger.LoggerSetup;
import org.openpipeline.scheduler.PipelineScheduler;
import org.openpipeline.util.Util;
import org.openpipeline.util.XMLConfig;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The OpenPipeline server. Maintains configuration information for
 * the server. 
 * <p>
 * For this class to operate as expected, the "app.home" system property
 * should be set, and it should contain a path to the installation directory.
 * It can be set on the Java command line:
 * <p>
 * java -Dapp.home=/mydirectory [classpath] org.openpipeline.server.Server
 */
public class Server {

	public static final String SERVER_CONFIG_FILE = "config/serverconfig.xml";

	private volatile static Server server;
	
	private ExecutorService threadPool = createThreadPool();
	private String homeDir;
	private XMLConfig config;
	//private static JettyInvoker jettyInvoker;
	//private static ServerGUIApp guiApp;
	private Logger logger;

	private long startTime;
	
	// cached for speed
	private boolean debug; 

	private Server() {
	}

	/**
	 * Return a reference to the server, creating it if necessary. The server is a singleton.
	 */
	public static Server getServer() {
		if (server == null) {			
			try {
				synchronized (Server.class) {
					/* yes, this is double-checked locking, which
					 * isn't supposed to work, but does because
					 * server is marked volatile.
					 */
					if (server == null) {
						server = new Server();
						server.init();
					}
				}
				
			} catch (Throwable t) {
				server = null;
				throw new RuntimeException(t);
			}
		}
		return server;
	}
	
	
	
	
	/**
	 * Initialize some server variables, start the scheduler.
	 * @throws IOException 
	 */
	private void init() throws IOException {
		
		homeDir = findHomeDir(); // inits the homeDir variable
		
		LoggerSetup.init(homeDir);
		
		logger = LoggerFactory.getLogger("server");
		
		startTime = System.currentTimeMillis();
		config = new XMLConfig();
		
		File file = new File(homeDir, SERVER_CONFIG_FILE);
		if (file.exists()) {
			try {
				config.load(file);
			} catch (IOException e) {
				logger.error("Error loading server config file:" + file.toString(), e);
				throw new RuntimeException(e);
			}
		} else {
			// config file doesn't exist. make one.
			config.addBooleanProperty("debug", false);
			
			try {
				config.save(file);
			} catch (IOException e) {
				logger.error("Error creating server config file:" + file.toString(), e);
				throw new RuntimeException(e);
			}
		} 

		// starts scheduler, loads jobs
		try {
			PipelineScheduler.getInstance();

		} catch (SchedulerException e) {
			logger.error("Error loading scheduler", e);
			try {
				PipelineScheduler.stop();
			} catch (SchedulerException e1) {
				logger.error("Error shutting down scheduler", e);
				throw new RuntimeException(e1);
			}
			throw new RuntimeException(e);
		}
		
		logger.info("Server initialized successfully. Version=" + getVersion() + " homeDir=" + homeDir);
	}



	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	
	/**
	 * Return the home directory for the app. 
	 */
	public String getHomeDir() {
		return homeDir;
	}

	/**
	 * Return the time the server has been running.
	 */
	public long getUptime() {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * Return the time the server has been running, formatted.
	 */
	public String getUptimeString() {
		float elapsed = getUptime();
		return Util.getFormattedElapsedTime(elapsed);
	}

	/**
	 * Return a property in the server config file.
	 * @param name the name of the property
	 * @return the value of the property, or null if name not found
	 */
	public String getProperty(String name) {
		return config.getProperty(name);
	}
	
	/**
	 * Calls getProperty(String name) and converts the value found from a String 
	 * to a boolean. 
	 * @param name the name of the value to return
	 * @param defaultValue the value to return if the name is not found
	 * @return the value as a boolean, or false
	 */
	public boolean getBooleanProperty(String name, boolean defaultValue) {
		String value = getProperty(name);
		return Util.toBoolean(value, defaultValue);
	}
	
	/**
	 * Return a property in the server config file.
	 * @param name the name of the property
	 * @param defaultValue the value to return if the property is not found
	 * @return the value of the property
	 */
	public String getProperty(String name, String defaultValue) {
		return config.getProperty(name, defaultValue);
	}

	/**
	 * Set a property in the server. May require a restart for the property to take effect.
	 * @param name name of the property
	 * @param value the value of the property
	 */
	public void setProperty(String name, String value) {
		config.setProperty(name, value);
		
		// must make sure cached values set
		debug = config.getBooleanProperty("debug", false);
	}

	public boolean getDebug() {
		return debug;
	}
	
	/**
	 * Save any changed properties to disk.
	 */
	public void saveProperties() throws IOException {
		config.save(homeDir + SERVER_CONFIG_FILE);
	}

	/**
	 * Return a general logger for the server. This is different
	 * from the Jetty logger, which is specific to the Jetty container.
	 * @return a server logger
	 */
	public Logger getLogger() {
		return logger;
	}
	

	/**
	 * Returns the internal thread pool for the server.
	 * @return a global thread pool
	 */
	public ExecutorService getThreadPool() {
		return threadPool;
	}
	
	private static ExecutorService createThreadPool() {
		ConfigurableThreadFactory factory = new ConfigurableThreadFactory();
		factory.setDaemon(true);
		//return Executors.newCachedThreadPool(factory);
		return Executors.newFixedThreadPool(10, factory);
	}
	
	
    /**
     * Identical to the DefaultThreadFactory inside the java.util.concurrent.Executors class,
     * except that daemon status is configurable. Could make priority configurable in the
     * future.
     */
    static class ConfigurableThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
        
        private boolean isDaemon = true;

        ConfigurableThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = "OpenPipeline ThreadPool-" + 
                          poolNumber.getAndIncrement() + 
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, 
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            
            t.setDaemon(isDaemon);

            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
        
        public void setDaemon(boolean isDaemon) {
        	this.isDaemon = isDaemon;
        }
    }

	/**
	 * Find the home directory for the app. The home directory should already
	 * be set in the system property "app.home". 
	 */
	public static String findHomeDir() {

		String homeDir = System.getProperty("diesel.home"); // for backward compatibility
		if (homeDir == null) {
			homeDir = System.getProperty("app.home");

			if (homeDir == null) {
				throw new RuntimeException(
					"Error -- home directory not set. You must set the \"app.home\" " +
					"java system property to an absolute path to the home directory for this app.");
			}
		}

		// standardize it
		File file = new File(homeDir);
		try {
			homeDir = file.getCanonicalPath() + File.separator;
		} catch (IOException e) {
			throw new RuntimeException("Could not standardized homeDir: " + file.toString() + " " + e.toString());
		}

		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new RuntimeException("Could not create home directory: " + file.toString());
			}
		}

		System.setProperty("app.home", homeDir);
		
		return homeDir;
	}
  

}
