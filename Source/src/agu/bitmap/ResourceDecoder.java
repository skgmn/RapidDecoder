package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

class ResourceDecoder extends BitmapDecoder {
	private Resources res;
	private int id;

	public ResourceDecoder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, id, opts);
	}

	class AguResourceProcessor extends AguBitmapProcessor {
		public AguResourceProcessor(Options opts, Rect region) {
			super(opts, region);
		}
		
		@Override
		public AguBitmapProcessor preProcess() {
			final TypedValue value = new TypedValue();
			res.getValue(id, value, true);
			
			if (opts != null) {
				if (opts.inDensity == 0) {
					opts.inDensity = translateDensity(value.density);
				}
				if (opts.inTargetDensity == 0) {
					opts.inTargetDensity = res.getDisplayMetrics().densityDpi;
				}
			}
			
			return super.preProcess();
		}
	}

	private static int translateDensity(int resDensity) {
		if (resDensity == TypedValue.DENSITY_DEFAULT) {
			return DisplayMetrics.DENSITY_DEFAULT;
		} else {
			return resDensity;
		}
	}

	@Override
	protected AguBitmapProcessor createBitmapProcessor(Options opts, Rect region) {
		return new AguResourceProcessor(opts, region);
	}

	@Override
	protected InputStream openInputStream() {
		return res.openRawResource(id);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(openInputStream(), false);
		} catch (IOException e) {
			return null;
		}
	}
}
