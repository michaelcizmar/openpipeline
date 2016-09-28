package org.openpipeline.pipeline.connector;

import java.util.List;

import org.openpipeline.filesystem.s3.S3File;
import org.openpipeline.filesystem.s3.S3FileSystem;
import org.openpipeline.pipeline.connector.Connector;
import org.openpipeline.pipeline.connector.GenericScanner;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueue;
import org.openpipeline.pipeline.connector.linkqueue.LinkQueueFactory;
import org.openpipeline.server.Server;
import org.openpipeline.util.WildcardMatcher;
import org.openpipeline.util.XMLConfig;
import org.slf4j.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * A connector that scans an S3 file system and processes the files it finds.
 */
public class S3Crawler extends Connector {

	private Logger logger;

	// parameters
	public static final long MAX_FILE_SIZE_DEFAULT = 100; // 100mb

	private String bucketName;
	private AmazonS3Client s3Client;

	private GenericScanner scanner;
	private String linkQueueName;
	private List<String> fileRoots;
	private List<String> includes;
	private List<String> excludes;
	private boolean scanSubDirs;
	private boolean scanCompressedFiles;
	private int docLoggingCount = 1;
	private long maxFileSize;


	@Override
	public void execute() {
		try {
			
			logger = super.getLogger();
			logger.info("Starting " + super.getJobName() + "...");
			super.setLastMessage("Running");
			
			if (super.getParams() == null) {
				String msg = "setParams() was not called for S3Crawler.";
				throw new IllegalStateException(msg);
			}

			extractParams();

			WildcardMatcher wildcardMatcher = new WildcardMatcher();
			wildcardMatcher.setIncludePatterns(includes);
			wildcardMatcher.setExcludePatterns(excludes);

			LinkQueue linkQueue = LinkQueueFactory
					.getLinkQueueByName(linkQueueName);
			if (linkQueue != null) {
				linkQueue.setParams(super.getParams()); // inits the queue,
														// could throw error
			}

			boolean debug = Server.getServer().getDebug();

			scanner = new GenericScanner();
			scanner.setParentConnector(this);
			scanner.setStartOfCrawl(System.currentTimeMillis());
			scanner.setDebug(debug);
			scanner.setDocLoggingCount(docLoggingCount);
			scanner.setLinkQueue(linkQueue);
			scanner.setLogger(logger);
			scanner.setStageList(super.getStageList());
			scanner.setScanSubDirs(scanSubDirs);
			scanner.setScanCompressedFiles(scanCompressedFiles);
			scanner.setWildcardMatcher(wildcardMatcher);
			scanner.setMaxFileSize(maxFileSize);

			// start scanning here
			for (int i = 0; i < fileRoots.size(); i++) {
				String filename = (String) fileRoots.get(i);
				S3File file = new S3File(filename, bucketName,s3Client);
				S3FileSystem fileSystem = new S3FileSystem(file);

				if (file.exists()) {
					scanner.scan(fileSystem);
				} else {
					logger.warn("File or directory does not exist:"
							+ file.toString());
				}
			}

			// the crawl is complete. Now roll through the linkqueue and find
			// deleted items
			scanner.lookForDeletes();

			super.setLastMessage("Ended");

		} catch (Throwable e) {
			super.error("Error executing S3FileScanner", e);
			super.setLastMessage("Error: " + e.toString());
		}
	}

	@Override
	public String getDisplayName() {
		return "S3 Crawler";
	}

	@Override
	public String getDescription() {
		return "Scans user data stored on Amazon S3";
	}

	@Override
	public String getPageName() {
		return "connector_s3crawler.jsp";
	}

	@Override
	public String getShortName() {
		return "S3Crawler";
	}

	@Override
	public String getLogLink() {
		return "log_viewer.jsp";
	}

	public void extractParams() {
		XMLConfig params = super.getParams();
		if (params != null) {

			bucketName = params.getProperty("bucket");
			
			String accessKey = params.getProperty("access-key");
			String secretKey = params.getProperty("secret-key");

			s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));

			scanSubDirs = params.getBooleanProperty("subdirs", true);
			scanCompressedFiles = params.getBooleanProperty("compressed-files",
					true);
			fileRoots = params.getValues("fileroots");
			includes = params.getValues("include-patterns");
			excludes = params.getValues("exclude-patterns");
			docLoggingCount = params.getIntProperty("doc-logging-count", 1);
			linkQueueName = params.getProperty("linkqueue-name");
			maxFileSize = params.getLongProperty("max-file-size",
					MAX_FILE_SIZE_DEFAULT);

			
			
		}
	}

}
