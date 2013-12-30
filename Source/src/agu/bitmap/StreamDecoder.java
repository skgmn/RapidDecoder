package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class StreamDecoder extends BitmapDecoder {
	private InputStream is;
	
	public StreamDecoder(InputStream is) {
		this.is = is;
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeStream(is);
	}

	@Override
	protected InputStream openInputStream() {
		return is;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(is, false);
		} catch (IOException e) {
			return null;
		}
	}

}
