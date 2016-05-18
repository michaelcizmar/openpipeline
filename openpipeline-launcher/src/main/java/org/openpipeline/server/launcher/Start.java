package org.openpipeline.server.launcher;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
/**
 * Simplified startup class for jetty. Mostly mimics the default jetty-*.xml
 * startup scripts.
 */
public class Start {

	private Server server;
	private Map<String, ArrayList<String>> contexts = new HashMap<String, ArrayList<String>>();
	private String configDir = "/config/";
	
	private static int stopPort;
	private static String stopKey;
	
	private StopMonitor stopMonitor;



	public void populateWebappsList(String[] args) {
		if (args == null) {
			return;
		}

		for (String arg : args) {
			if (arg.startsWith("-a")) {
				int pos = arg.indexOf('=');
				if (pos == -1) {
					throw new IllegalArgumentException("bad -a argument");
				}
				String context = arg.substring(2, pos);
				String path = arg.substring(pos + 1);
				addContext(context, path);
			}
		}
	}

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
	 * Set the configuration directory relative to the home directory. Defaults
	 * to "/etc/".
	 */
	public void setConfigDir(String configDir) {
		this.configDir = configDir;
	}

	/**
	 * Start the server. Usage:
	 * <p>
	 * -a/context=path/to/app, ... repeat
	 * </p>
	 * Use the -a option to add one or more webapps.
	 * <p>
	 * "context" is the path at which the webapp will appear. "foo" would put
	 * the app at "www.domain.com/foo".
	 * </p>
	 * <p>
	 * "/path/to/app" is a path on disk to the webapp app directory or a .war
	 * file. It's relative to the main application directory. For example,
	 * "/webapps/foo.war" or "/webapps/foo" for an exploded directory
	 * </p>
	 * 
	 * @param args
	 *            command line arguments or null
	 * @throws Exception
	 */
	public void start() throws Exception {

		// setUpClassLoader();

		// rhubarb.home is the home directory where the application is started,
		// not the directory where the rhubarb binaries are located.
		String appHome = System.getProperty("app.home", ".");
		appHome = new File(appHome).getCanonicalPath();
		System.setProperty("app.home", appHome);

		stopPort = Integer.parseInt(System.getProperty("server.stopPort", "8081"));
		stopKey = System.getProperty("server.stopKey", "openpipeline");
		
		
		// set up logback
		if (System.getProperty("logback.configurationFile") == null) {
			File logbackFile = new File(appHome, configDir + "logback.xml");
			if (logbackFile.exists()) {
				System.setProperty("logback.configurationFile",
						logbackFile.toString());
			} else {
				// this will search the classpath
				System.setProperty("logback.configurationFile", configDir
						+ "logback.xml");
			}
		}

		// if webdefaults.xml exists in /etc, use it, else pull it from the
		// classpath
		File wd = new File(appHome, configDir + "webdefault.xml");
		String webDefaults;
		if (wd.exists()) {
			webDefaults = wd.toString();
		} else {
			webDefaults = configDir + "webdefault.xml"; // should pull from the
			// classpath
		}

		// the code below duplicates jetty.xml
		QueuedThreadPool pool = new QueuedThreadPool();
		pool.setMinThreads(10);
		pool.setMaxThreads(200);
		pool.setDetailedDump(false);

		server = new Server(pool);
		server.setStopAtShutdown(true);
		/*
		 * server.setSendServerVersion(true); server.setSendDateHeader(true);
		 */
		server.setStopTimeout(5000);
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);

		ArrayList<Handler> handlerList = new ArrayList();

		// The default jetty request logger writes directly to disk.
		// Instead, use one that uses slf4j.
		// the ignore paths code only works with trailing * or
		// *.extension. It's not generalized wildcard matching
		String[] ignorePaths = { "/images/*", "/img/*", "*.css", "*.jpg",
				"*.JPG", "*.gif", "*.GIF", "*.ico", "*.ICO", "*.js" };
		RequestLogHandler requestLogHandler = new RequestLogHandler();
		NCSARequestLog requestLog = new NCSARequestLog();
		requestLog.setIgnorePaths(ignorePaths);
		requestLogHandler.setRequestLog(requestLog);
		handlerList.add(requestLogHandler);

		ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
		// ContextHandlerCollection must know about the server.
		contextHandlers.setServer(server);
		handlerList.add(contextHandlers);

		// add configured webapps
		
		// Create temporary webapp directory.
		
		File tmpDir = new File(appHome + "/work/");
		
		for (Map.Entry<String, ArrayList<String>> entry : contexts.entrySet()) {

			String context = entry.getKey();
			ArrayList<String> paths = entry.getValue();

			WebAppContext r = new WebAppContext();

			r.setCopyWebInf(true);
			r.setContextPath(context);
			r.setDefaultsDescriptor(webDefaults);
			r.setAttribute("org.eclipse.jetty.webapp.basetempdir", tmpDir.getCanonicalPath());
			r.setPersistTempDirectory(false);
			
			if (paths.size() == 1) {
				r.setWar(paths.get(0));
			} else {
				// multiple resources bases
				String[] p = new String[paths.size()];
				paths.toArray(p);
				ResourceCollection coll = new ResourceCollection(p);
				r.setBaseResource(coll);
			}

			//r.setClassLoader(new WebAppClassLoader(getClass().getClassLoader(), r));

			contextHandlers.addHandler(r);
		}

		// for local development. do this last, because it matches / and
		// will catch requests not handled elsewhere
		File localAppFile = new File(appHome, "src/main/webapp");
		if (localAppFile.exists()) {
			WebAppContext localApp = new WebAppContext();
			localApp.setContextPath("/");
			// localApp.setWar(localAppFile.toString());
			localApp.setDefaultsDescriptor(webDefaults);

			/*
			 * Based on this url:
			 * http://musingsofaprogrammingaddict.blogspot.com
			 * /2009/12/running-jsf -2-on-embedded-jetty.html
			 */
			File localTargetDir = new File(appHome, "/target");
			localApp.setBaseResource(new ResourceCollection(new String[] {
					localAppFile.toString(), localTargetDir.toString() }));

			contextHandlers.addHandler(localApp);
		} else {
			// TODO: Default handler overrides the root context that may be
			// required in another project. There
			// must be a switch to suppress this optionally.

			// handlerList.add(new RhubarbDefaultHandler());
		}

		Handler[] handlers = handlerList
				.toArray(new Handler[handlerList.size()]);

		HandlerCollection col = new HandlerCollection();
		col.setHandlers(handlers);
		server.setHandler(col);
		// end jetty.xml

		// the code below duplicates jetty-http.xml
		HttpConfiguration httpConf = new HttpConfiguration();
		httpConf.setSecureScheme("https");
		httpConf.setSecurePort(getInt(System.getProperty("jetty.tls.port"),
				8443));
		httpConf.setOutputBufferSize(32768);
		httpConf.setRequestHeaderSize(8192);
		httpConf.setResponseHeaderSize(8192);
		httpConf.setSendServerVersion(false);
		
		HttpConnectionFactory httpConFactory = new HttpConnectionFactory(
				httpConf);
		ConnectionFactory[] factories = { httpConFactory };
		ServerConnector serverCon = new ServerConnector(server, factories);
		serverCon.setHost(System.getProperty("jetty.host"));
		serverCon.setPort(getInt(System.getProperty("jetty.port"), 8080));
		serverCon.setIdleTimeout(30000);
		server.addConnector(serverCon);
		// end jetty-http.xml

		// jetty-deploy.xml
		DeploymentManager deployMgr = new DeploymentManager();
		deployMgr.setContexts(contextHandlers);

		// Note: For some reason this does not work.
		// deployMgr.setContextAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
		// ".*/servlet-api-[^/]*\\.jar$");

		// monitors the application webapps dir
		WebAppProvider webapps = new WebAppProvider();
		// Webapp Provider must know about the Deployment Manager.
		webapps.setDeploymentManager(deployMgr);
		
		deployMgr.addAppProvider(webapps);

		webapps.setScanInterval(5); // 5 secs
		webapps.setExtractWars(true);

		webapps.setDefaultsDescriptor(webDefaults);

		// monitor two directories: one is a webapps resource directory within
		// the rhubarb jar itself, and the other is the /webapps dir in the
		// application
		ArrayList dirs = new ArrayList();
		// dirs.add(this.getClass().getResource("/webapps").toExternalForm());
		dirs.add(appHome + File.separator + "webapps");
		webapps.setMonitoredDirectories(dirs);

		deployMgr.addAppProvider(webapps);

		/*
		 * Enable "web-fragment.xml" processing within the Eclipse environment.
		 */

		// enableWebFragmentProcessing(webapps, deployMgr, contextHandlers);

		server.addBean(deployMgr);

		// end jetty-deploy.xml

		System.out.println("Starting Rhubarb Server in " + appHome + "...");

		// Optionally, start a new thread to listen to rhubarb:stop maven
		// goal.
		configureStopMonitor();
		
		this.server.start();
		
		server.join();
	}

	/**
	 * Creates the parent classloader for all webapps, and loads all of the
	 * modules in Config. / private void setUpClassLoader() throws
	 * MalformedURLException {
	 * 
	 * // set the parent classloader for all webapps ModuleClassLoader fancy =
	 * new ModuleClassLoader(new URL[0]);
	 * Thread.currentThread().setContextClassLoader(fancy);
	 * 
	 * List<String> modules = Config.getList("modules"); for (String module:
	 * modules) { fancy.addModule(module); } }
	 */

	private int getInt(String val, int defaultVal) {
		if (val != null) {
			return Integer.parseInt(val);
		}
		return defaultVal;
	}

	private void configureStopMonitor() throws UnknownHostException,
			IOException {
		stopMonitor = new StopMonitor(stopPort, stopKey, this);
		stopMonitor.start();
	}

	/**
	 * Hang until the server terminates.
	 */
	public void join() throws InterruptedException {
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
	}
}
