package org.openpipeline.logger;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;

/**
 * Replaces the old LoggerUtil.
 */
public class LoggerSetup {
	
	public static void init(String homeDir) throws IOException {

		/*
		 * From the logback docs: If you wish, you can specify the location of
		 * the default configuration file with a system property named
		 * logback.configurationFile. The value of the this property can be a
		 * URL, a resource on the class path or a path to a file external to the
		 * application.
		 */

		String SYSPROP = "logback.configurationFile";
		String XMLFILE = "logback.xml";

		// first, check for a system property
		String prop = System.getProperty(SYSPROP);
		if (prop == null) {
			String configDir = homeDir + "/config";

			// second, check the config dir
			File file = new File(configDir, XMLFILE);
			if (!file.exists()) {
				throw new IOException("logback.xml not found in " + file.toString());
			}

			System.setProperty(SYSPROP, file.toString());
		}

	
		//initConsoleLogger();

	}
	
	
	/**
	 * NOT CURRENTLY USED. Let Jetty handle it.
	 * This method converts Logback logger into a PrintStream.
	 * Java's Console logging requires special handling because the default console log
	 * handler (System.Err(), System.out) expects a java.io.PrintStream.
	 * /
	private static void initConsoleLogger() {
		Logger logger = LoggerFactory.getLogger("root");
		if (logger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger lb = (ch.qos.logback.classic.Logger) logger;
			Iterator<Appender<ILoggingEvent>> it = lb.iteratorForAppenders();
			if (it.hasNext()) {
				Appender<ILoggingEvent> appender = it.next();
				if (appender instanceof RollingFileAppender) {
					RollingFileAppender<ILoggingEvent> fileAppender = (RollingFileAppender<ILoggingEvent>) appender;
					PrintStream serverLog = new PrintStream(
							fileAppender.getOutputStream());
					System.setErr(serverLog);
					System.setOut(serverLog);
				}
			}
		}
	}
	*/

	
	/**
	 * Closes any appenders attached to a logger. Does not close the appenders
	 * for any parent loggers.
	 * 
	 * @param logger
	 *            the logger to close
	 * /
	public static void closeLogger(Logger logger) {
		if (logger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger lb = (ch.qos.logback.classic.Logger) logger;
			lb.detachAndStopAllAppenders();
		}
	}
	*/

}
