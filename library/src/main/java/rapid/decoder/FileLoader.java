package rapid.decoder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

class FileLoader extends BitmapLoader {
	private String pathName;
	
	public FileLoader(String pathName) {
		if (pathName == null) {
			throw new NullPointerException();
		}
		this.pathName = pathName;
	}
	
	protected FileLoader(FileLoader other) {
		super(other);
		pathName = other.pathName;
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeFile(pathName, opts);
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return new FileInputStream(pathName);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(pathName, false);
		} catch (IOException e) {
			return null;
		}
	}

	@NonNull
    @Override
	public BitmapLoader mutate() {
		return new FileLoader(this);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ pathName.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof FileLoader) || !super.equals(o)) return false;
		
		final FileLoader d = (FileLoader) o;
		return pathName.equals(d.pathName);
	}

	@Override
	protected boolean isMemCacheSupported() {
		return true;
	}
}
