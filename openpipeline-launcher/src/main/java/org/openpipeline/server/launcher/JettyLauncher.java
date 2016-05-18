package org.openpipeline.server.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;





/**
 * Launches the Jetty Server.
 */
public class JettyLauncher {
	/*
	 * Production directory structure expected:
	 * 
	 * app.home
	 *  /config
	 *         /logback.xml
	 *         /webdefault.xml
	 * 
	 * Discussion on Jetty temporary directories:
	 * 
	 * http://docs.codehaus.org/display/JETTY/Temporary+Directories
	 */
	private Map<String, ArrayList<String>> contexts = new HashMap<String, ArrayList<String>>();
	 

	/**
	 * Explicitly add one or more webapp contexts.
	 * 
	 * @param contextPath
	 * @param resourceBase
	 */
	public void addContext(String contextPath, String resourceBase) {
		// aggregate resources by context
		ArrayList<String> list = contexts.get(contextPath);
		if (list == null) {
			list = new ArrayList<String>();
			contexts.put(contextPath, list);
		}
		list.add(resourceBase);
	}

	/**
	 * Returns a list of webapps
	 * 
	 * @param homeDir
	 * @return
	 * @throws IOException
	 */
	private ArrayList<WebAppContext> getWebapps(String homeDir)
			throws IOException {
		ArrayList<WebAppContext> webapps = new ArrayList<WebAppContext>();
		Iterator<Entry<String, ArrayList<String>>> it = contexts.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<String, ArrayList<String>> entry = (Entry<String, ArrayList<String>>) it
					.next();
			String contextPath = entry.getKey();
			ArrayList<String> baseDirs = entry.getValue();

			for (int i = 0; i < baseDirs.size(); i++) {
				// make dir relative to homeDir
				String dir = (new File(homeDir, baseDirs.get(i)))
						.getCanonicalPath();
				baseDirs.set(i, dir);
			}

			String[] dirs = new String[baseDirs.size()];
			baseDirs.toArray(dirs);
			ResourceCollection coll = new ResourceCollection(dirs);

			WebAppContext webapp = new WebAppContext();
			
			webapp.setBaseResource(coll);
			webapp.setContextPath(contextPath);

			webapp.setDefaultsDescriptor(homeDir + "/config/webdefault.xml");

			webapp.setParentLoaderPriority(true);
			webapps.add(webapp);
		}
		return webapps;
	}

	private boolean exists(String path) {
		File f = new File(path);
		return f.exists();
	}

	/**
	 * Launches a new jetty server.
	 * 
	 * @throws Exception
	 */
	public void execute() throws Exception {
		
		if (1==1)
		throw new RuntimeException("This does not work. See notes in openpipeline/JettyLauncher.execute()");
		/*
		 *  I never finished upgrading op to jetty 9 and jdk 1.7
		 *  It required a few changes in this class, and now it doesn't respond
		 *  to requests. It just hangs. There's some kind of connector configuration
		 *  problem. Must debug later.
		 *  A better solution would be to have op depend on Rhubarb/Start.
		 *  Or migrate diesel to rhubarb, and copy over only the op classes we really need.
		 * 
		 */
		
		
		String homeDir = System.getProperty("app.home");

		// Check if app.home are set or not.
		// Setting Jetty home is absolutely essential at startup.
		if (homeDir == null || homeDir.length() == 0) {
			throw new Exception(
					"app.home not set. Please set -Dapp.home=\"...\" on the java command line.");
		}

		//Create jetty temporary work directory.
		File tempDir = new File(homeDir, "work");
		tempDir.mkdirs();
		
		// Default Jetty port
		String jettyPort = System.getProperty("jetty.port", "8080");
		System.setProperty("jetty.port", jettyPort);

		// Secure Port
		String sslPort = System.getProperty("jetty.ssl.port", "8443");
		System.setProperty("jetty.ssl.port", sslPort);

		// Default location for logback.xml ${JETTY_HOME}/etc/logback.xml
		String logbackConfigurationFile = System.getProperty(
				"logback.configurationFile", homeDir + "/config/logback.xml");
		System.setProperty("logback.configurationFile",
				logbackConfigurationFile);

		System.out.println("Starting the servers...");
		System.out.println(
				"app.home=" + System.getProperty("app.home") +
				" Jetty port=" + jettyPort);

		// Initialize OpenPipeline Server.
		org.openpipeline.server.Server.getServer();

		// Uncomment these methods for debugging purposes only.
		// server.setDumpAfterStart(true);
		// server.setDumpBeforeStop(true);

		// Setup Threadpool
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(10);
		
		// Initialize Jetty Server
		Server server = new Server(threadPool);
		
		// Setup Connectors
		/*
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(Integer.parseInt(jettyPort));
		connector.setMaxIdleTime(30000);
		connector.setConfidentialPort(Integer.parseInt(sslPort));
		connector.setStatsOn(true);
		server.setConnectors(new Connector[] { connector });
		*/
		
		
		// the code below duplicates jetty-http.xml
		HttpConfiguration httpConf = new HttpConfiguration();
		httpConf.setSecureScheme("https");
		httpConf.setSecurePort(getInt(System.getProperty("jetty.tls.port"), 8443));
		httpConf.setOutputBufferSize(32768);
		httpConf.setRequestHeaderSize(8192);
		httpConf.setResponseHeaderSize(8192);
		
		HttpConnectionFactory httpConFactory = new HttpConnectionFactory(httpConf);
		ConnectionFactory [] factories = {httpConFactory};
		ServerConnector serverCon = new ServerConnector(server, factories);
		serverCon.setHost(System.getProperty("jetty.host"));
		serverCon.setPort(getInt(System.getProperty("jetty.port"), 8080));
		serverCon.setIdleTimeout(30000);
		server.addConnector(serverCon);
		// end jetty-http.xml

		// TODO: Add SSL support. See LikeJettyXxml.java in the jetty project
		// under "example-jetty-embedded" module.

		RequestLogHandler requestLogHandler = new RequestLogHandler();
		NCSARequestLog requestLog = new NCSARequestLog();

		// the ignore paths code only works with trailing * or
		// *.extension. It's not generalized wildcard matching
		String [] ignorePaths = {"/images/*", "/admin/images/*", "*.css", 
                "*.jpg", "*.JPG", "*.gif", "*.GIF", "*.ico", "*.ICO", "*.js"};
		requestLog.setIgnorePaths(ignorePaths);
		requestLogHandler.setRequestLog(requestLog);

		
		// Assembly all the contexts.
		HandlerCollection handlers = new HandlerCollection();
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		ArrayList<WebAppContext> webapps = getWebapps(homeDir);
		if (webapps.size() > 0) {
			ContextHandler[] contextHandlers = new ContextHandler[webapps
					.size()];
			webapps.toArray(contextHandlers);
			contexts.setHandlers(contextHandlers);
		}

		// Create a DefaultHandler
		DefaultHandler defaultHandler = new DefaultHandler();

		// Dont serve Jetty's favicon.ico.
		defaultHandler.setServeIcon(false);

		handlers.setHandlers(new Handler[] { contexts, new DefaultHandler(),
				requestLogHandler });

		// Routes all the default handlers via the StatisticsHandler.
		// TODO: Explorer this in detail. We can use this class for getting
		// detailed statistics to keep a check on the health of the server.

		StatisticsHandler stats = new StatisticsHandler();
		stats.setHandler(handlers);
		server.setHandler(stats);

		// Setup deployers
		DeploymentManager deployer = new DeploymentManager();
		deployer.setContexts(contexts);
		server.addBean(deployer);

		/*
		String contextDir = homeDir + "/config/contexts";
		ContextProvider contextProvider = new ContextProvider();
		contextProvider.setMonitoredDirName(contextDir);
		contextProvider.setScanInterval(2);
		deployer.addAppProvider(contextProvider);
		*/

		WebAppProvider webappProvider = new WebAppProvider();
		webappProvider.setMonitoredDirName(homeDir + "/webapps");
		webappProvider.setParentLoaderPriority(false);
		webappProvider.setExtractWars(true);
		webappProvider.setScanInterval(2);
		webappProvider.setDefaultsDescriptor(homeDir + "/config/webdefault.xml");
		//webappProvider.setContextXmlDir(contextDir);

		deployer.addAppProvider(webappProvider);

		// Optionally, initialize Test Realm. This is useful, in case you need
		// to secure webapps like diesel-admin
		String realmProperties = homeDir + "/config/realm.properties";
		if (exists(realmProperties)) {
			HashLoginService login = new HashLoginService();
			login.setName("Test Realm");

			login.setConfig(realmProperties);
			server.addBean(login);
		}

		server.setStopAtShutdown(true);
		// server.setSendServerVersion(true);

		server.start();

		server.join();
	}

	public static void main(String[] args) throws Exception {
		(new JettyLauncher()).execute();
	}
	
	private int getInt(String val, int defaultVal) {
		if (val != null) {
			return Integer.parseInt(val);
		}
		return defaultVal;
	}
}
