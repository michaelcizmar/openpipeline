package org.openpipeline.filesystem.s3;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * S3File implementation for Amazon S3.
 */
public class S3File {

	/*
	 * On folders: S3 doesn't support them directly. So we adopt a convention:
	 * if the filename ends with /, it's a folder. It should be a file of 0
	 * length.
	 */

	private int NOT_FOUND = 404;
	private String delimiter = "/";

	protected AmazonS3Client s3Client;
	protected String bucket;
	protected String fullPath;

	private ObjectMetadata metadata;
	private boolean exists;
	private boolean connected;

	// Used for listing the files and metadata
	private ListObjectsRequest request;

	public S3File(String fullPath, String bucket, AmazonS3Client s3Client) {

		if (".".equals(fullPath) || "./".equals(fullPath)
				|| "/".equals(fullPath)) {
			fullPath = delimiter;
		} else if (fullPath.startsWith(delimiter)) {
			// Should not start with a delimiter
			fullPath = fullPath.substring(1);
		}

		fullPath = fullPath.replace("\\", delimiter);
		fullPath = fullPath.replace("//", delimiter);

		this.fullPath = fullPath;
		this.bucket = bucket;
		this.s3Client = s3Client;
	}

	private void connect() throws IOException {

		if (connected) {
			return;
		}

		connected = true;

		// Since S3 handles files and directories differently, this is the
		// easiest way to see if the object (file or directory) exists.
		// If it does, try to get the metadata, which is available only for
		// files.
		request = new ListObjectsRequest().withBucketName(bucket)
				.withDelimiter(delimiter).withPrefix(fullPath);

		ObjectListing list = s3Client.listObjects(request);

		if (list.getObjectSummaries().size() > 0
				|| list.getCommonPrefixes().size() > 0) {
			exists = true;
		} else {
			exists = false;
			return;
		}

		try {
			metadata = s3Client.getObjectMetadata(bucket, fullPath);
		} catch (AmazonServiceException e) {
			if (e.getStatusCode() == NOT_FOUND) {
				// Do nothing, this is not a file
				if (!fullPath.endsWith(delimiter)) {
					fullPath += delimiter;
				}
				connected = false;
			} else if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				throw new IOException(e);
			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	/**
	 * Determines if the current object is a file or a folder. S3 does not have
	 * the concept of a folder but common file name prefixes can be treated as
	 * folders.
	 * 
	 * @return true if it is a file, false otherwise
	 * @throws IOException
	 *             if a service side error occurs
	 * @throws RuntimeException
	 *             on any other S3 error / private boolean determineIsFile()
	 *             throws IOException {
	 * 
	 *             // Get the parent String parent = getParent(); String
	 *             parentDir = filePrefix; if (parent != null) { parentDir =
	 *             filePrefix + parent; }
	 * 
	 *             try { // List the objects in the parent directory
	 *             ListObjectsRequest request = new ListObjectsRequest()
	 *             .withBucketName(bucket).withDelimiter(delimiter)
	 *             .withPrefix(parentDir);
	 * 
	 *             // If this is listed as a prefix, it is a folder
	 *             ObjectListing list = s3Client.listObjects(request);
	 *             Iterator<String> iter = list.getCommonPrefixes().iterator();
	 * 
	 *             list.getCommonPrefixes()
	 * 
	 * 
	 *             while (iter.hasNext()) { if (iter.next().equals(filePrefix +
	 *             fileName)) { return false; } } return true;
	 * 
	 *             } catch (AmazonServiceException e) { if
	 *             (e.getErrorType().equals(
	 *             AmazonServiceException.ErrorType.Service)) { // request is
	 *             valid, service side error, can retry throw new
	 *             IOException(e); } else { // client side error: invalid access
	 *             key or parameter, throw // RuntimeException throw e; } } }
	 */

	public String getName() {

		if (fullPath.endsWith("/")) {
			// return the folder name

			// in S3 folder names must end with a slash, remove the last slash
			// first
			String temp = fullPath.substring(0, fullPath.length() - 1);

			// return whatever appears after the last slash in the temp string
			int lastSlash = temp.lastIndexOf('/');
			return temp.substring(lastSlash + 1) + "/";

		}

		// return whatever appears after the last slash
		int lastSlash = fullPath.lastIndexOf('/');
		return fullPath.substring(lastSlash + 1);
	}

	public String getParent() {
		int pos = fullPath.lastIndexOf('/');
		return fullPath.substring(0, pos + 1);
	}

	public boolean exists() throws IOException {
		connected = false;
		connect();
		return exists;
	}

	public boolean isDirectory() throws IOException {
		connect();
		return fullPath.endsWith("/");
	}

	public boolean isFile() throws IOException {
		connect();
		return !isDirectory();
	}

	public long lastModified() throws IOException {
		if (exists() && metadata != null) {
			return metadata.getLastModified().getTime();
		}
		return 0;
	}

	public long length() throws IOException {
		if (exists() && isFile()) {
			return metadata.getContentLength();
		}
		return 0;
	}

	/**
	 * Deletes the S3File. If the S3File is a folder, deletes all
	 * sub-directories and files first.
	 * 
	 * @return true if the file was deleted successfully, false if a service
	 *         side error occurred.
	 * 
	 * @throws IOException
	 *             if file does not exist
	 * @throws RuntimeException
	 *             on any S3 error other than AmazonServiceException of the
	 *             Service type
	 */

	public boolean delete() throws IOException {

		if (!exists()) {
			throw new FileNotFoundException();
		}

		if (isDirectory()) {
			S3File[] list = null;
			try {
				list = listS3Files();
			} catch (IOException e) {
				return false;
			}

			if (list.length == 0) {
				return this.deleteFile();
			}

			boolean result = true;
			for (int i = 0; i < list.length; i++) {
				S3File nextFile = (S3File) list[i];
				if (!nextFile.delete()) {
					result = false;
				}
			}

			if (result) {
				// all subdirs and files deleted successfully
				return this.deleteFile();
			}
			return false;

		} else {
			return this.deleteFile();
		}

	}

	/**
	 * Deletes an S3File.
	 * 
	 * @return true if the file was deleted successfully, false if a service
	 *         side error occurred.
	 * 
	 * @throws RuntimeException
	 *             on any S3 error other than AmazonServiceException of the
	 *             Service type
	 */
	private boolean deleteFile() {

		try {
			s3Client.deleteObject(bucket, fullPath);
			exists = false;
			connected = false;
			return true;

		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				return false;
			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	/**
	 * Lists the files and sub-directories in this S3File.
	 * 
	 * @return S3File[] containing sub-directories and files, null if this is
	 *         not a directory
	 * 
	 * @throws IOException
	 *             if a service side error occurs
	 * @throws RuntimeException
	 *             on any other S3 error
	 */

	public S3File[] listS3Files() throws IOException {

		if (!exists()) {
			throw new FileNotFoundException();
		}

		if (isDirectory()) {
			try {

				List<S3File> buffer = new ArrayList<S3File>();

				ObjectListing list = null;
				while (true) {

					// Retrieve a list of objects with the name prefix root/path
					if (list == null) {
						request = new ListObjectsRequest()
								.withBucketName(bucket)
								.withDelimiter(delimiter).withPrefix(fullPath);

						list = s3Client.listObjects(request);

					} else {
						// continue if the previous list was truncate
						list = s3Client.listNextBatchOfObjects(list);
					}

					// Collect the results
					List<S3ObjectSummary> summaries = list.getObjectSummaries();
					Iterator<S3ObjectSummary> iter = summaries.iterator();

					while (iter.hasNext()) {

						S3ObjectSummary obj = iter.next();
						String name = obj.getKey();

						if (name.startsWith(fullPath)) {
							name = name.substring(fullPath.length());
						}

						if (name.trim().length() > 0 && !name.equals("/")) {

							S3File newFile = new S3File(fullPath + name,
									bucket, s3Client);
							buffer.add(newFile);
						}
					}

					// Get the list of subdirs, in s3 subdirs are just filename
					// prefixes
					List<String> prefixes = list.getCommonPrefixes();
					Iterator<String> i2 = prefixes.iterator();
					while (i2.hasNext()) {
						String name = i2.next();
						if (name.startsWith(fullPath)) {
							name = name.substring(fullPath.length());
						}
						if (name.trim().length() > 0 && !name.equals("/")) {

							S3File newFile = new S3File(fullPath + name,
									bucket, s3Client);
							buffer.add(newFile);
						}
					}

					if (!list.isTruncated()) {
						break;
					}
				}

				int sz = buffer.size();
				S3File[] remoteFiles = buffer.toArray(new S3File[sz]);
				return remoteFiles;

			} catch (AmazonServiceException e) {
				if (e.getErrorType().equals(
						AmazonServiceException.ErrorType.Service)) {
					// request is valid, service side error, can retry
					throw new IOException(e);
				} else {
					// client side error: invalid access key or parameter, throw
					// RuntimeException
					throw e;
				}
			}
		} else {
			return null;
		}
	}

	public S3FilesPageIterator getPageIterator() {
		return new S3FilesPageIterator(this.getFullPath(), s3Client, bucket,
				delimiter);
	}

	/**
	 * Renames the file.
	 * 
	 * @param dest
	 *            containing the destination file
	 * @return true if the file was deleted successfully, false if a service
	 *         side error occurred.
	 * 
	 * @throws UnsupportedOperationException
	 *             if this S3File is a directory
	 * @throws RuntimeException
	 *             on any S3 error other than AmazonServiceException of the
	 *             Service type
	 */

	public boolean renameTo(S3File dest) throws IOException {

		if (isDirectory()) {
			throw new UnsupportedOperationException();
		}
		return renameFileTo(dest);
	}

	private boolean renameFileTo(S3File dest) throws IOException {
		try {

			String destPath = dest.toString();

			if (dest.exists()) {
				// An object with the desired name already exists
				return false;
			}

			AccessControlList acl = s3Client.getObjectAcl(bucket, fullPath);

			CopyObjectRequest copyRequest = new CopyObjectRequest(bucket,
					fullPath, bucket, destPath).withNewObjectMetadata(metadata);
			s3Client.copyObject(copyRequest);

			s3Client.setObjectAcl(bucket, destPath, acl);

		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				return false;

			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}

		this.delete();

		return true;
	}

	public int compareTo(S3File otherFile) {
		return this.fullPath.compareTo(otherFile.fullPath);
	}

	public String toString() {
		return fullPath;
	}

	public int hashCode() {
		return fullPath.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof S3File) {
			S3File other = (S3File) obj;
			return this.fullPath.equals(other.fullPath);
		}
		return false;
	}

	/**
	 * Returns the input stream.
	 * 
	 * @throws IOException
	 *             if a service side error occurs
	 * @throws RuntimeException
	 *             on any other S3 error
	 */

	public InputStream getInputStream() throws IOException {

		try {
			S3Object object = s3Client.getObject(bucket, fullPath);
			InputStream is = object.getObjectContent();
			return is;

		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				throw new IOException(e);

			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	public String getContentType() throws IOException {
		if (exists() && metadata != null) {
			return metadata.getContentType();
		}
		return null;
	}

	public int compareTo(Object obj) {
		return compareTo((S3File) obj);
	}

	/**
	 * Creates the directory named by the fileName. In S3 it is not needed, so
	 * only the parent directory is created.
	 */

	public void mkdirs() throws IOException {

		if (!fullPath.endsWith("/")) {
			fullPath += "/";
		}

		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		try {
			s3Client.putObject(bucket, fullPath, input, metadata);
		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				throw new IOException(e);

			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	public boolean createNewFile() throws IOException {

		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		try {
			s3Client.putObject(bucket, fullPath, input, metadata);
			exists = true;
			return true;
		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				throw new IOException(e);

			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	/**
	 * Returns the outputstream.
	 * 
	 * @return OutputStream
	 * @throws IOException
	 */

	public OutputStream getOutputStream() throws IOException {
		// s3 will only upload complete files. So we have
		// to stream the output to disk, and then when the
		// stream closes it gets uploaded.
		return new S3OutputStream(this);
	}

	/**
	 * Uploads the temporary file on disk to this file in S3, deletes the temp
	 * file.
	 * 
	 * @param s3os
	 * @throws IOException
	 */
	public void upload(File tmpFile) throws IOException {

		if (tmpFile == null) {
			return;
		}

		try {

			s3Client.putObject(bucket, fullPath, tmpFile);
			tmpFile.delete();
		} catch (AmazonServiceException e) {
			if (e.getErrorType().equals(
					AmazonServiceException.ErrorType.Service)) {
				// request is valid, service side error, can retry
				throw new IOException(e);

			} else {
				// client side error: invalid access key or parameter, throw
				// RuntimeException
				throw e;
			}
		}
	}

	public String getFullPath() {
		return fullPath;
	}

	/**
	 * Auxiliary class for file upload.
	 */
	private class S3OutputStream extends OutputStream {

		private File tmpFile;
		private S3File s3file;
		private BufferedOutputStream bos;

		/**
		 * Creates a temporary file and returns its outputstream.
		 * 
		 * @param tmpFile
		 *            containing the temporary file name
		 * @param s3file
		 *            containing the target S3 file
		 * @throws IOException
		 */
		public S3OutputStream(S3File s3file) throws IOException {

			tmpFile = File.createTempFile("s3" + System.currentTimeMillis(),
					null);

			// If the virtual machine ever terminates gracefully, the temporary
			// files will be deleted.
			tmpFile.deleteOnExit();

			this.bos = new BufferedOutputStream(new FileOutputStream(tmpFile),
					8192);
			this.s3file = s3file;
		}

		public void write(byte[] array) throws IOException {
			bos.write(array);
		}

		public void write(int b) throws IOException {
			bos.write(b);
		}

		/**
		 * Closes the stream and uploads it to S3.
		 * 
		 * @throws IOException
		 */
		public void close() throws IOException {
			bos.close();
			s3file.upload(this.tmpFile);
			tmpFile.delete();
		}
	}

	public ObjectMetadata getMetadata() throws IOException {
		connect();
		return metadata;
	}
}
