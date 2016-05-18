package com.openpipeline.filesystem.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

public class S3RandomAccessFile extends S3File {

	private RandomAccessFile randFile;
	private File tempFile;

	// S3TransferManager is a high level class to handle s3 uploads. In
	// particular, it does multi-part uploads for large files.
	private S3TransferManager tm;

	// true if changes to this file need to be written to S3
	private boolean isDirty = false;

	public S3RandomAccessFile(String fullPath, String bucket,
			AmazonS3Client s3Client) {
		super(fullPath, bucket, s3Client);
	}

	public OutputStream createOutputStream(long offset) throws IOException {
		/*
		 * This is used when the client wants to write to S3. We first write to
		 * a local file, if it doesn't exist, and then return an outputstream
		 * that starts at the right offset.
		 */
		if (randFile == null) {
			// we get here if this is the first call to createOutputStream()
			randFile = new RandomAccessFile(getTempFile(), "rw");
			randFile.setLength(0);
			// isDirty = true;
		}

		return new RandomFileOutputStream(randFile, offset);
	}

	public InputStream createInputStream(long offset) throws IOException {
		/*
		 * This is used when the client wants to read from S3. Unfortunately,
		 * when this method is first called we have to download the entire file
		 * from S3 to a local file, and then return an input stream starting at
		 * the right offset.
		 */
		if (randFile == null) {
			readFileFromS3(); // creates randFile
		}

		return new RandomFileInputStream(randFile, offset);
	}

	private File getTempFile() throws IOException {

		if (tempFile != null) {
			return tempFile;
		}

		String tmpDir = System.getProperty("java.io.tmpdir");
		String fileName = System.currentTimeMillis() + "/" + fullPath;

		tempFile = new File(tmpDir, fileName);
		File parent = tempFile.getParentFile();
		parent.mkdirs();

		boolean created = tempFile.createNewFile();
		for (int i = 0; i < 3; i++) {
			// A file with this name exists. Retry. Rare in practice
			if (created) {
				break;
			}
			fileName = System.currentTimeMillis() + "/" + fullPath;
			created = tempFile.createNewFile();
		}
		if (!created) {
			// Should not happen in practice
			throw new RuntimeException("Temp file cannot be created: " + tmpDir
					+ "/" + fileName);
		}

		return tempFile;
	}

	public void handleClose() throws IOException {
		/*
		 * This gets called when the client is finished with "any" of the
		 * actions
		 */

		if (randFile == null) {
			// do nothing
			return;
		}

		if (isDirty) {
			// the local file needs to be written to S3
			writeFileToS3();

			isDirty = false;
			randFile.close();
			randFile = null;
			return;
		}

		// else this class was used for reading, just close the file
		randFile.close();
		randFile = null;
	}

	private void writeFileToS3() throws IOException {
		try {
			if (tm != null) {
				tm.upload(bucket, fullPath, getTempFile().getAbsolutePath());
			} else {
				s3Client.putObject(bucket, fullPath, getTempFile());
			}
		} catch (AmazonClientException e) {
			String msg = "error writing file: " + fullPath;
			handleError(msg, e);
		}
	}

	/**
	 * Write the s3 object to a local temp file.
	 * 
	 * @throws IOException
	 */
	private void readFileFromS3() throws IOException {

		if (randFile != null) {
			randFile.close();
		}

		randFile = new RandomAccessFile(getTempFile(), "rw");
		randFile.setLength(0);

		S3Object object = s3Client.getObject(bucket, fullPath);
		InputStream in = object.getObjectContent();

		byte[] buffer = new byte[64 * 1024];
		int len = in.read(buffer);
		while (len != -1) {
			randFile.write(buffer, 0, len);
			len = in.read(buffer);
		}

		in.close();
	}

	private void handleError(String msg, Exception e) throws IOException {
		throw new IOException(msg, e);
	}

	/**
	 * OutputStream wrapper around a RandomAccessFile that starts at an offset.
	 */
	class RandomFileOutputStream extends OutputStream {

		private RandomAccessFile randFile;

		public RandomFileOutputStream(RandomAccessFile randFile, long offset)
				throws IOException {
			randFile.seek(offset);
			this.randFile = randFile;
		}

		@Override
		public void write(int b) throws IOException {
			randFile.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			randFile.write(b, off, len);
		}

		@Override
		public void close() {
			isDirty = true;
		}
	}

	class RandomFileInputStream extends InputStream {

		private RandomAccessFile randFile;

		public RandomFileInputStream(RandomAccessFile randFile, long offset)
				throws IOException {
			randFile.seek(offset);
			this.randFile = randFile;
		}

		@Override
		public int read() throws IOException {
			return randFile.read();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return randFile.read(b, off, len);
		}
	}

	public boolean isExecutable() {
		return false;
	}

	public boolean create() throws IOException {
		try {
			randFile = new RandomAccessFile(getTempFile(), "rw");
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	public void truncate() throws IOException {
		if (randFile == null) {
			randFile = new RandomAccessFile(getTempFile(), "rw");
		}
		randFile.setLength(0);
		// isDirty = true;
		handleClose();
	}

	public void setS3TransferManager(S3TransferManager tm) {
		this.tm = tm;
	}
}
