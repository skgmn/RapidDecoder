package rapid.decoder;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory.Options;
import android.os.Build;

import static rapid.decoder.cache.ResourcePool.*;

public final class Cloner {
	@SuppressLint("NewApi")
	public static Options clone(Options other) {
		final Options opts = OPTIONS.obtainNotReset();
		
		opts.inDensity = other.inDensity;
		opts.inDither = other.inDither;
		opts.inInputShareable = other.inInputShareable;
		opts.inJustDecodeBounds = other.inJustDecodeBounds;
		opts.inPreferredConfig = other.inPreferredConfig;
		opts.inPurgeable = other.inPurgeable;
		opts.inSampleSize = other.inSampleSize;
		opts.inScaled = other.inScaled;
		opts.inScreenDensity = other.inScreenDensity;
		opts.inTargetDensity = other.inTargetDensity;
		opts.inTempStorage = other.inTempStorage;
		opts.mCancel = other.mCancel;
		opts.outHeight = other.outHeight;
		opts.outMimeType = other.outMimeType;
		opts.outWidth = other.outWidth;

		final int ver = Build.VERSION.SDK_INT;
		if (ver >= 10) {
			opts.inPreferQualityOverSpeed = other.inPreferQualityOverSpeed;
			
			if (ver >= 11) {
				opts.inBitmap = other.inBitmap;
				opts.inMutable = other.inMutable;
				
				if (ver >= 19) {
					opts.inPremultiplied = other.inPremultiplied;
				}
			}
		}
		
		return opts;
	}
}
