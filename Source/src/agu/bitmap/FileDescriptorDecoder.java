package agu.bitmap;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class FileDescriptorDecoder extends BitmapDecoder {
	private FileDescriptor fd;
	
	public FileDescriptorDecoder(FileDescriptor fd) {
		this.fd = fd;
	}
	
	protected FileDescriptorDecoder(FileDescriptorDecoder other) {
		super(other);
		fd = other.fd;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeFileDescriptor(fd, null, opts);
	}

	@Override
	protected InputStream getInputStream() {
		return new FileInputStream(fd);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(fd, false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public BitmapDecoder clone() {
		return new FileDescriptorDecoder(this);
	}
}
