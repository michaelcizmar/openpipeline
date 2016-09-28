package org.openpipeline.filesystem.s3;

import java.io.IOException;

import org.openpipeline.filesystem.s3.S3File;

import junit.framework.TestCase;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

public class TestS3File extends TestCase {

	public void test() throws IOException {

		System.setProperty("app.home", "/dev/enginez/trunk/enginez-test");

		/*
		 * InputStream in = new FileInputStream(
		 * "C:/Users/imatveeva/Desktop/PartsSearch.txt");
		 * 
		 * S3File file = new S3File("webapptest/testFile2.txt"); OutputStream os
		 * = file.getOutputStream(); Utils.writeStream(in, os);
		 */
		String accessKey = "AKIAJOH4KKVCP3H65FAQ";
		String secretKey = "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";

		boolean exists = false;
		boolean isDir = false;
		S3File[] files;
		S3File dir;

		System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
		
		AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(
				accessKey, secretKey));

/*		dir = new S3File("webapptest/folder1", "com.enginez.sites", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("webapptest/folder2", "com.enginez.sites", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("webapptest/logs/", "com.enginez.sites", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("webapptest/logs/index_logs/webapptest/search/",
				"com.enginez.sites", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

*/		//
		
		dir = new S3File("default2/",
				"com.enginez.accounts", s3Client);
		exists = dir.exists();
		
		dir = new S3File("default/", "com.enginez.accounts", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("default/i-0f503174/", "com.enginez.accounts",
				s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("default/i-0f503174/logs", "com.enginez.accounts",
				s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("default/i-0f503174/logs/indexlog",
				"com.enginez.accounts", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("default/i-0f503174/logs/indexlog/indexlog-2012-08-15-06.log",
				"com.enginez.accounts", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

		dir = new S3File("default/i-0f503174/logs/indexlog/indexlog-2012-08-15-06.log",
				"com.enginez.accounts", s3Client);
		exists = dir.exists();
		isDir = dir.isDirectory();
		files = dir.listS3Files();

	}
}
