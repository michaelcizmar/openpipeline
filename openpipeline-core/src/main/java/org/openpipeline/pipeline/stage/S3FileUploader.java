package org.openpipeline.pipeline.stage;

import java.io.File;

import org.openpipeline.pipeline.item.DocBinary;
import org.openpipeline.pipeline.item.Item;
import org.openpipeline.scheduler.PipelineException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
/**
 * 
 *
 */
public class S3FileUploader extends Stage {
	private String bucketName;
	private String accessKey;
	private String secretKey;
	private String rootDirectory;
	private AmazonS3Client s3Client;

	@Override
	public void initialize() {
		if (params != null) {
			accessKey = params.getProperty("access-key");
			secretKey = params.getProperty("secret-key");
			bucketName = params.getProperty("bucket");
			rootDirectory = params.getProperty("root-dir");
			AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey,
					secretKey);
			s3Client = new AmazonS3Client(awsCredentials);
		}
	}

	@Override
	public void processItem(Item item) throws PipelineException {
		upload(item);
		if (nextStage != null) {
			nextStage.processItem(item);
		}
	}

	private void upload(Item item) throws PipelineException {
		DocBinary bin = item.getDocBinary();
		if (bin != null) {
			try {
				File file = new File(bin.getName());
				String key = rootDirectory + convertDirToKey(file) + file.getName();
				PutObjectResult res = s3Client.putObject(bucketName, key, file);
				System.out.println(file.getName() +"\t" + res.getETag());
			} catch (AmazonServiceException e) {
				throw new PipelineException(e);
			}
		}
	}

	/**
	 * Converts file path to S3 compatible key format.
	 * @param file
	 * @return
	 */
	private String convertDirToKey(File file) {
		File parent = file.getParentFile();
		if (parent == null) {
			return "";
		}

		String prefix = convertDirToKey(parent);
		return prefix + parent.getName() + "/";
	}

	@Override
	public String getDescription() {
		return "Uploads one or more files to Amazon S3 Storage.";
	}

	@Override
	public String getDisplayName() {
		return "S3 File Uploader";
	}

	@Override
	public String getConfigPage() {
		return "stage_s3fileuploader.jsp";
	}
}
