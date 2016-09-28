package org.openpipeline.filesystem.s3;

import java.io.IOException;
import java.io.InputStream;

import org.openpipeline.filesystem.s3.S3File;
import org.openpipeline.filesystem.s3.S3FileSystem;
import org.openpipeline.pipeline.connector.filesystem.FileIterator;
import org.openpipeline.pipeline.connector.filesystem.FileSystem;
import org.openpipeline.pipeline.item.Item;

/**
 * Abstract base class for different file systems. Maps to java.util.File. Also
 * serves as a factory class.
 */
public class S3FileSystem implements FileSystem {

	private S3File file;

	public S3FileSystem(S3File file) {
		this.file = file;
	}

	@Override
	public S3FileIterator getIterator() throws IOException {
		return new S3FileIterator(file);
	}

	class S3FileIterator implements FileIterator {
		private S3File[] files;
		private int next;

		public S3FileIterator(S3File file) throws IOException {
			this.files = file.listS3Files();
		}

		public boolean hasNext() {
			return next < files.length;
		}

		public S3FileSystem next() {
			S3File nextFile = files[next];
			next++;
			return new S3FileSystem(nextFile);
		}
	}

	/**
	 * Gets the full path for an S3 file.
	 * 
	 * @return the full path for an S3 file, null otherwise.
	 */
	@Override
	public String getFullName() {
		if (file instanceof org.openpipeline.filesystem.s3.S3File) {
			return ((org.openpipeline.filesystem.s3.S3File) file).getFullPath();
		}
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return file.getInputStream();
	}

	@Override
	public boolean getItem(Item item) throws IOException {
		return false;
	}

	@Override
	public long getLastUpdate() {
		try {
			return file.lastModified();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getSignature() {
		return getLastUpdate();
	}

	@Override
	public boolean isDirectory() {
		try {
			return file.isDirectory();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isFile() {
		try {
			return file.isFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long getSize() {
		try {
			return file.length();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public FileSystem fetch(String fullname) throws IOException {

		S3File nextFile = new S3File(fullname, this.file.bucket,
				this.file.s3Client);
		if (nextFile.exists()) {
			return new S3FileSystem(nextFile);
		}
		return null;
	}
}
