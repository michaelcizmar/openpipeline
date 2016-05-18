package com.openpipeline.filesystem.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3FilesPageIterator {

	private String fullPath;
	private AmazonS3Client s3Client;
	private String bucket;
	private boolean isTruncated = true;

	// Used for listing the files and pagination
	private ListObjectsRequest request;

	// markerKey - Indicates where in the bucket to begin listing. The
	// list will only include keys that occur lexicographically after marker.
	// For pagination: to get the next page of results use the last key of the
	// current page as the marker. Can be null.
	private String markerKey;
	private String currentMarkerKey;

	public S3FilesPageIterator(String fullPath, AmazonS3Client s3Client,
			String bucket, String delimiter) {

		this.s3Client = s3Client;
		this.bucket = bucket;
		this.fullPath = fullPath;
	}

	/**
	 * Lists a page of files in this S3File. Directories are not included.
	 * 
	 * @param maxKeys
	 *            Specifies the maximum number of objects to return. Valid
	 *            Values: Integers from 1 to 1000, inclusive.
	 * @param requestedMarker
	 *            Indicates where in the bucket to begin listing. The list will
	 *            only include keys that occur lexicographically after marker.
	 *            To get the next page of results use the last key of the
	 *            current page as the marker. Can be null.
	 * 
	 * @return An array of files, null of there are no more files to list
	 * @throws IOException
	 * @throws AmazonClientException
	 */
	public S3File[] getNextPage(int maxKeys, String requestedMarker)
			throws AmazonClientException, IOException {

		try {

			if (!isTruncated) {
				return null;
			}

			List<S3File> buffer = new ArrayList<S3File>();

			ObjectListing list = null;

			// Retrieve the first page of files
			if (requestedMarker == null) {
				request = new ListObjectsRequest().withBucketName(bucket)
						.withMarker(fullPath).withMaxKeys(maxKeys)
						.withPrefix(fullPath);
				list = s3Client.listObjects(request);

			} else {
				// continue from the requested marker key
				request = new ListObjectsRequest().withBucketName(bucket)
						.withMarker(requestedMarker).withMaxKeys(maxKeys)
						.withPrefix(fullPath);
				list = s3Client.listObjects(request);
			}

			// set the marker for the next page
			markerKey = list.getNextMarker();
 
			// set the marker for the current page, will serve for "previous" page link
			currentMarkerKey = list.getMarker();
			
			// set the truncated property for the next page
			// if it is false, there are no more files to list, last page
			isTruncated = list.isTruncated();

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

					S3File newFile = new S3File(fullPath + name, bucket,
							s3Client);
					buffer.add(newFile);
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
	}

	public String getMarker() {
		return this.markerKey;
	}

	public boolean isTruncated() {
		return this.isTruncated;
	}

}
