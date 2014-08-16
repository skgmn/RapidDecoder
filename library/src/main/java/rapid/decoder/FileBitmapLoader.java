package rapid.decoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class FileBitmapLoader extends BitmapLoader {
	public FileBitmapLoader(String pathName) {
		if (pathName == null) {
			throw new NullPointerException();
		}
        mId = pathName;
	}
	
	protected FileBitmapLoader(FileBitmapLoader other) {
		super(other);
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeFile((String) mId, opts);
	}

    @Override
	protected InputStream getInputStream() {
		try {
			return new FileInputStream((String) mId);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance((String) mId, false);
		} catch (IOException e) {
			return null;
		}
	}

	@NonNull
    @Override
	public BitmapLoader mutate() {
		return new FileBitmapLoader(this);
	}
	
	@Override
	public boolean equals(Object o) {
        return o == this || o instanceof FileBitmapLoader && super.equals(o);
    }
}
