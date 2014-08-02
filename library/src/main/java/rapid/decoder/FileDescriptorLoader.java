package rapid.decoder;

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
import android.support.annotation.NonNull;

class FileDescriptorLoader extends BitmapLoader {
	private FileDescriptor fd;
	
	public FileDescriptorLoader(FileDescriptor fd) {
		this.fd = fd;
	}
	
	protected FileDescriptorLoader(FileDescriptorLoader other) {
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

	@NonNull
    @Override
	public BitmapLoader mutate() {
		return new FileDescriptorLoader(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ fd.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof FileDescriptorLoader) || !super.equals(o)) return false;
		
		final FileDescriptorLoader fdd = (FileDescriptorLoader) o;
		return fd.equals(fdd.fd);
	}

	@Override
	protected boolean isMemCacheSupported() {
		return false;
	}
}
