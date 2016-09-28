package org.openpipeline.filesystem.s3;

import java.io.File;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class S3TransferManager {

	private AWSCredentials awsCredentials;

	public S3TransferManager(AWSCredentials awsCredentials) {
		this.awsCredentials = awsCredentials;
	}

	public void upload(String bucket, String s3Path, String file) {

		TransferManager tm = new TransferManager(awsCredentials);
		PutObjectRequest request = new PutObjectRequest(bucket, s3Path,
				new File(file));

		request.setProgressListener(new ProgressListener() {
			public void progressChanged(ProgressEvent event) {
				System.out.println("Transferred bytes: "
						+ event.getBytesTransfered());
			}
		});

		// TransferManager processes all transfers asynchronously,
		// so this call will return immediately.
		Upload upload = tm.upload(request);

		try {
			// You can block and wait for the upload to finish
			upload.waitForCompletion();
		} catch (AmazonClientException amazonClientException) {
			System.out.println("Unable to upload file, upload aborted.");
			amazonClientException.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("InterruptedException during s3 upload.");
			e.printStackTrace();
		}

	}
}
