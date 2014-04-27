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
	
	private double densityRatio;

	public ResourceDecoder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, id, opts);
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

	@Override
	protected double getDensityRatio() {
		if (densityRatio == 0) {
			decodeBounds();

			if (opts.inDensity != 0 && opts.inTargetDensity != 0) {
				densityRatio = (double) opts.inTargetDensity / opts.inDensity;
			} else {
				densityRatio = 1;
			}
		}
		return densityRatio;
	}
}
