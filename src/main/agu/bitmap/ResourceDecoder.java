package agu.bitmap;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

class ResourceDecoder extends BitmapDecoder {
	private Resources res;
	private int id;
	
	private InputStream in;
	
	public ResourceDecoder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeResource(res, id, opts);
	}

	@SuppressLint("NewApi")
	@Override
	protected Bitmap decodePartial(Options opts, Rect region) {
		final AguBitmapProcessor processor = new AguResourceProcessor(opts, region);
		processor.preProcess();
		
//		if (Build.VERSION.SDK_INT >= 10) {
//			try {
//				final Bitmap bitmap = BitmapRegionDecoder.newInstance(in, false).decodeRegion(region, opts);
//				return processor.postProcess(bitmap);
//			} catch (IOException e) {
//				return null;
//			}
//		} else {
			return aguDecodePreProcessed(in, opts, region, processor);
//		}
	}
	
	class AguResourceProcessor extends AguBitmapProcessor {
		public AguResourceProcessor(Options opts, Rect region) {
			super(opts, region);
			
		}
		
		@Override
		public AguBitmapProcessor preProcess() {
			final TypedValue value = new TypedValue();
			in = res.openRawResource(id, value);
			
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
}
