package agu.bitmap;

import static agu.caching.ResourcePool.MATRIX;
import static agu.caching.ResourcePool.OPTIONS;
import static agu.caching.ResourcePool.PAINT;

import java.io.InputStream;

import agu.bitmap.decoder.AguDecoder;
import agu.util.Cloner;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

public abstract class ExternalBitmapDecoder extends BitmapDecoder {
	public static final int SIZE_AUTO = 0;

	protected Options opts;
	protected boolean mutable;
	private boolean scaleFilter = true;
	private boolean useBuiltInDecoder = false;
	
	private int width;
	private int height;
	
	private MemCacheEnabler<?> memCacheEnabler;
	
	// Temporary variables
	private float adjustedDensityRatio;
	private float adjustedWidthRatio;
	private float adjustedHeightRatio;
	
	protected ExternalBitmapDecoder() {
		super();
		opts = OPTIONS.obtain();
		opts.inScaled = false;
	}
	
	protected ExternalBitmapDecoder(ExternalBitmapDecoder other) {
		super(other);
		
		opts = Cloner.clone(other.opts);
		
		mutable = other.mutable;
		scaleFilter = other.scaleFilter;
		useBuiltInDecoder = other.useBuiltInDecoder;
		
		width = other.width;
		height = other.height;
		
		if (other.memCacheEnabler != null) {
			setMemCacheEnabler(other.memCacheEnabler.clone());
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			OPTIONS.recycle(opts);
		} finally {
			super.finalize();
		}
	}
	
	protected void decodeBounds() {
		opts.inJustDecodeBounds = true;
		decode(opts);
		opts.inJustDecodeBounds = false;

		width = opts.outWidth;
		height = opts.outHeight;
	}

	@Override
	public int sourceWidth() {
		if (width == 0) {
			decodeBounds();
		}
		return width;
	}
	
	@Override
	public int sourceHeight() {
		if (height == 0) {
			decodeBounds();
		}
		return height;
	}
	
	void setMemCacheEnabler(MemCacheEnabler<?> enabler) {
		enabler.setBitmapDecoder(this);
		memCacheEnabler = enabler;
	}
	
	private Object getCacheKey() {
		return memCacheEnabler != null ? memCacheEnabler : this;
	}
	
	public Bitmap getCachedBitmap() {
		synchronized (sMemCacheLock) {
			if (sMemCache == null) return null;
			return sMemCache.get(getCacheKey());
		}
	}
	
	@Override
	public Bitmap decode() {
		final boolean memCacheSupported = (memCacheEnabler != null || isMemCacheSupported());
		if (memCacheSupported) {
			final Bitmap cachedBitmap = getCachedBitmap();
			if (cachedBitmap != null) {
				return cachedBitmap;
			}
		}
		
		// reset
		
		opts.mCancel = false;
		adjustedDensityRatio = 0;
		
		//
		
		resolveQueries();

		// Setup sample size.
		
		final boolean postScaleBy = (ratioWidth != 1 || ratioHeight != 1);

		if (postScaleBy) {
			opts.inSampleSize = calculateInSampleSizeByRatio();
		} else {
			opts.inSampleSize = 1;
		}
		
		// Execute actual decoding
		
		if (opts.mCancel) return null;
		
		Bitmap bitmap = executeDecoding();
		if (bitmap == null) return null;
		
		// Scale it finally.
		
		if (postScaleBy) {
			bitmap.setDensity(Bitmap.DENSITY_NONE);
			Bitmap bitmap2;
			
			Matrix m = MATRIX.obtain();
			m.setScale(adjustedWidthRatio, adjustedHeightRatio);
			bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, scaleFilter);
			MATRIX.recycle(m);
			
			if (bitmap != bitmap2) {
				bitmap.recycle();
			}

			bitmap2.setDensity(opts.inTargetDensity);
			bitmap = bitmap2;
		}
		
		if (memCacheSupported) {
			synchronized (sMemCacheLock) {
				if (sMemCache != null) {
					sMemCache.put(getCacheKey(), bitmap);
				}
			}
		}
		
		return bitmap;
	}
	
	private int calculateInSampleSizeByRatio() {
		adjustedWidthRatio = ratioWidth;
		adjustedHeightRatio = ratioHeight;
		
		int sampleSize = 1;
		while (adjustedWidthRatio <= 0.5f && adjustedHeightRatio <= 0.5f) {
			sampleSize *= 2;
			adjustedWidthRatio *= 2f;
			adjustedHeightRatio *= 2f;
		}
		
		return sampleSize;
	}

	private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
		resolveQueries();
		opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
				targetWidth, targetHeight);
		
		return executeDecoding();
	}
	
	/**
	 * Simulate native decoding.
	 * @return
	 */
	@SuppressLint("NewApi")
	protected Bitmap executeDecoding() {
		final boolean regional = region != null &&
				!(region.left == 0 && region.top == 0 &&
				region.width() == sourceWidth() && region.height() == sourceHeight());
		final boolean useBuiltInDecoder =
				this.useBuiltInDecoder ||
				(mutable && (Build.VERSION.SDK_INT < 11 || regional)) ||
				(opts.inSampleSize > 1 && !scaleFilter);
		
		onDecodingStarted(useBuiltInDecoder);
		
		try {
			if (useBuiltInDecoder) {
				return aguDecode(region);
			} else {
				if (regional) {
					return decodeRegional(opts, region);
				} else {
					return decode(opts);
				}
			}
		} finally {
			onDecodingFinished();
		}
	}
	
	private int regionWidth() {
		if (region != null) {
			return region.width();
		} else {
			return sourceWidth();
		}
	}
	
	private int regionHeight() {
		if (region != null) {
			return region.height();
		} else {
			return sourceHeight();
		}
	}
	
	@SuppressLint("NewApi")
	@Override
	public ExternalBitmapDecoder mutable(boolean mutable) {
		this.mutable = mutable;
		if (Build.VERSION.SDK_INT >= 11) {
			opts.inMutable = mutable;
		}
		return this;
	}
	
	protected abstract Bitmap decode(Options opts);
	protected abstract InputStream getInputStream();
	protected abstract BitmapRegionDecoder createBitmapRegionDecoder();
	protected abstract boolean isMemCacheSupported();
	
	protected void onDecodingStarted(boolean builtInDecoder) {
	}
	
	protected void onDecodingFinished() {
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	protected Bitmap decodeRegional(Options opts, Rect region) {
		adjustDensityRatio(false);
		
		final BitmapRegionDecoder d = createBitmapRegionDecoder();
		return (d == null ? null : d.decodeRegion(region, opts));
	}
	
	private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }
	
	    return inSampleSize;
	}

	protected Bitmap aguDecode(Rect region) {
		final InputStream in = getInputStream();
		if (in == null) return null;
		
		adjustDensityRatio(true);
		
		final AguDecoder d = new AguDecoder(in);
		d.setRegion(region);
		d.setUseFilter(scaleFilter);
		d.setSampleSize(opts.inSampleSize);
		
		final Bitmap bitmap = d.decode(opts);
		d.close();
		
		return mutable ? bitmap : Bitmap.createBitmap(bitmap);
	}
	
	@Override
	public void cancel() {
		opts.requestCancelDecode();
	}

	@Override
	public void draw(Canvas cv, Rect rectDest) {
		final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());
		
		final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
		cv.drawBitmap(bitmap, null, rectDest, p);
		PAINT.recycle(p);
		
		bitmap.recycle();
	}

	@Override
	public BitmapDecoder useBuiltInDecoder(boolean force) {
		this.useBuiltInDecoder = force;
		return this;
	}

	@Override
	public BitmapDecoder config(Config config) {
		opts.inPreferredConfig = config;
		return this;
	}
	
	private void adjustDensityRatio(boolean checkRatio) {
		if (adjustedDensityRatio == 0) {
			if (checkRatio && (ratioWidth != 1 || ratioHeight != 1)) {
				adjustedDensityRatio = 1;
			} else {
				adjustedDensityRatio = getDensityRatio();
				
				while (adjustedDensityRatio <= 0.5) {
					opts.inSampleSize *= 2;
					adjustedDensityRatio *= 2;
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		final int hashRegion = (region == null ? HASHCODE_NULL_REGION : region.hashCode());
		final int hashOptions = (mutable ? 0x55555555 : 0) | (scaleFilter ? 0xAAAAAAAA : 0);
		final int hashConfig = (opts.inPreferredConfig == null ? HASHCODE_NULL_BITMAP_OPTIONS : opts.inPreferredConfig.hashCode());
		
		return hashRegion ^ hashOptions ^ hashConfig ^ queriesHash();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExternalBitmapDecoder)) return false;
		
		final ExternalBitmapDecoder d = (ExternalBitmapDecoder) o;
		
		final Config config1 = opts.inPreferredConfig;
		final Config config2 = d.opts.inPreferredConfig;
		
		return (region == null ? d.region == null : region.equals(d.region)) &&
				(config1 == null ? config2 == null : config1.equals(config2)) &&
				mutable == d.mutable &&
				scaleFilter == d.scaleFilter &&
				queriesEquals(d);
	}
	
	@Override
	public BitmapDecoder filterBitmap(boolean filter) {
		scaleFilter = filter;
		return this;
	}
}
