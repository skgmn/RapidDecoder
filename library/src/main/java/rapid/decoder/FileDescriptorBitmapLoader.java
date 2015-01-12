package rapid.decoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class FileDescriptorBitmapLoader extends BitmapLoader {
	private FileDescriptor fd;
	
	public FileDescriptorBitmapLoader(FileDescriptor fd) {
		this.fd = fd;
	}
	
	protected FileDescriptorBitmapLoader(FileDescriptorBitmapLoader other) {
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
	public BitmapLoader fork() {
		return new FileDescriptorBitmapLoader(this);
	}
	
	@Override
	public int hashCode() {
		if (mHashCode == 0) {
            mHashCode = super.hashCode() + 31 * fd.hashCode();
        }
        return mHashCode;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof FileDescriptorBitmapLoader) || !super.equals(o)) return false;
		
		final FileDescriptorBitmapLoader fdd = (FileDescriptorBitmapLoader) o;
		return fd.equals(fdd.fd);
	}
}
