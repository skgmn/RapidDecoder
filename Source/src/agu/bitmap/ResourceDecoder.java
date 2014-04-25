package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class ResourceDecoder extends BitmapDecoder {
	Resources res;
	int id;

	public ResourceDecoder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, id, opts);
	}
	
	@Override
	public Bitmap decode() {
		// Ensure that the native decoder fills in inDensity and inTargetDensity.
		decodeBounds();
		return super.decode();
	}

	@Override
	protected InputStream getInputStream() {
		return res.openRawResource(id);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(getInputStream(), false);
		} catch (IOException e) {
			return null;
		}
	}
}
