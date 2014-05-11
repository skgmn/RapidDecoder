package agu.bitmap;

import static agu.caching.ResourcePool.MATRIX;
import static agu.caching.ResourcePool.OPTIONS;
import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;

import java.io.InputStream;

import agu.bitmap.decoder.AguDecoder;
import agu.scaling.AspectRatioCalculator;
import agu.util.Cloner;
import android.annotation.SuppressLint;
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
	protected Rect region;
	protected boolean mutable;
	private boolean scaleFilter = true;
	private boolean useBuiltInDecoder = false;
	
	private int width;
	private int height;
	private int targetWidth;
	private int targetHeight;
	private float ratioWidth = 1;
	private float ratioHeight = 1;
	
	// Temporary variables
	private float adjustedDensityRatio;
	private float adjustedWidthRatio;
	private float adjustedHeightRatio;
	
	protected ExternalBitmapDecoder() {
		opts = OPTIONS.obtain();
	}
	
	protected ExternalBitmapDecoder(ExternalBitmapDecoder other) {
		opts = Cloner.clone(other.opts);
		
		region = new Rect(other.region);
		mutable = other.mutable;
		scaleFilter = other.scaleFilter;
		useBuiltInDecoder = other.useBuiltInDecoder;
		
		width = other.width;
		height = other.height;
		targetWidth = other.targetWidth;
		targetHeight = other.targetHeight;
		ratioWidth = other.ratioWidth;
		ratioHeight = other.ratioHeight;
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			OPTIONS.recycle(opts);
			RECT.recycle(region);
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
	
	@Override
	public int width() {
		if (targetWidth != 0) {
			return targetWidth;
		} else if (region != null) {
			return (int) Math.ceil(region.width() * ratioWidth);
		} else {
			return (int) Math.ceil(sourceWidth() * getDensityRatio() * ratioWidth);
		}
	}

	@Override
	public int height() {
		if (targetHeight != 0) {
			return targetHeight;
		} else if (region != null) {
			return (int) Math.ceil(region.height() * ratioHeight);
		} else {
			return (int) Math.ceil(sourceHeight() * getDensityRatio() * ratioHeight);
		}
	}
	
	@Override
	public Bitmap decode() {
		// reset
		
		opts.mCancel = false;
		adjustedDensityRatio = 0;

		// Setup target size.
		
		int finalWidth = 0;
		int finalHeight = 0;

		if (this.targetWidth != 0 || this.targetHeight != 0) {
			if (targetWidth == 0) {
				finalWidth = AspectRatioCalculator.fitHeight(sourceWidth(), sourceHeight(), targetHeight);
				finalHeight = targetHeight;
			} else if (finalHeight == 0) {
				finalWidth = targetWidth;
				finalHeight = AspectRatioCalculator.fitWidth(sourceWidth(), sourceHeight(), targetWidth);
			} else {
				finalWidth = this.targetWidth;
				finalHeight = this.targetHeight;
			}
		} else if (region != null && (ratioWidth != 1 || ratioHeight != 1)) {
			finalWidth = (int) Math.ceil(region.width() * ratioWidth);
			finalHeight = (int) Math.ceil(region.height() * ratioHeight);
		}
		
		// Setup sample size.
		
		final boolean postScaleTo = (finalWidth != 0 && finalHeight != 0);
		final boolean postScaleBy = (region == null && (ratioWidth != 1 || ratioHeight != 1));

		if (postScaleTo) {
			opts.inScaled = false;
			opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
					finalWidth, finalHeight);
		} else if (postScaleBy) {
			opts.inScaled = false;
			opts.inSampleSize = calculateInSampleSizeByRatio();
		} else {
			opts.inScaled = true;
			opts.inSampleSize = 1;
		}
		
		// Execute actual decoding
		
		if (opts.mCancel) return null;
		
		final Bitmap bitmap = executeDecoding();
		if (bitmap == null) return null;
		
		// Scale it finally.
		
		if (postScaleTo || postScaleBy) {
			bitmap.setDensity(Bitmap.DENSITY_NONE);
			Bitmap bitmap2;
			
			if (postScaleTo) {
				bitmap2 = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, scaleFilter);
			} else {
				Matrix m = MATRIX.obtain();
				m.setScale(adjustedWidthRatio, adjustedHeightRatio);
				bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, scaleFilter);
				MATRIX.recycle(m);
			}
			
			if (bitmap != bitmap2) {
				bitmap.recycle();
			}

			bitmap2.setDensity(opts.inTargetDensity);
			return bitmap2;
		} else {
			return bitmap;
		}
	}
	
	private int calculateInSampleSizeByRatio() {
		final float densityRatio = getDensityRatio();
		
		adjustedWidthRatio = densityRatio * ratioWidth;
		adjustedHeightRatio = densityRatio * ratioHeight;
		
		int sampleSize = 1;
		while (adjustedWidthRatio <= 0.5 && adjustedHeightRatio <= 0.5) {
			sampleSize *= 2;
			adjustedWidthRatio *= 2;
			adjustedHeightRatio *= 2;
		}
		
		return sampleSize;
	}

	private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
		opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
				targetWidth, targetHeight);
		opts.inScaled = false;
		
		return executeDecoding();
	}
	
	/**
	 * Simulate native decoding.
	 * @return
	 */
	@SuppressLint("NewApi")
	protected Bitmap executeDecoding() {
		// Adjust region.
		
		final Rect region;
		final boolean recycleRegion;
		
		if (this.region != null && getDensityRatio() != 1) {
			final float densityRatio = getDensityRatio();
			
			region = RECT.obtainNotReset();
			
			region.left = (int) (this.region.left / densityRatio);
			region.top = (int) (this.region.top / densityRatio);
			region.right = (int) (this.region.right / densityRatio);
			region.bottom = (int) (this.region.bottom / densityRatio);
			
			recycleRegion = true;
		} else {
			region = this.region;
			recycleRegion = false;
		}
		
		//
		
		final boolean useBuiltInDecoder =
				this.useBuiltInDecoder ||
				(mutable && Build.VERSION.SDK_INT < 11) ||
				(opts.inSampleSize > 1 && !scaleFilter);
		
		onDecodingStarted(useBuiltInDecoder);
		
		final Bitmap bitmap;
		try {
			if (useBuiltInDecoder) {
				bitmap = aguDecode();
			} else {
				if (region != null &&
						!(region.left == 0 && region.top == 0 &&
						region.width() == sourceWidth() && region.height() == sourceHeight())) {
					
					bitmap = decodeRegional(opts, region);
				} else {
					if (Build.VERSION.SDK_INT >= 11) {
						opts.inMutable = mutable;
					}
					return decode(opts);
				}
			}
		} finally {
			if (recycleRegion) {
				RECT.recycle(region);
			}
			onDecodingFinished();
		}
		
		// Scale corresponds to the desired density.
		
		if (bitmap == null || opts.mCancel) return null;
		
		if (opts.inScaled) {
			final int newWidth;
			final int newHeight;
			
			if (this.region != null) {
				newWidth = this.region.width();
				newHeight = this.region.height();
			} else {
				newWidth = (int) (bitmap.getWidth() * adjustedDensityRatio);
				newHeight = (int) (bitmap.getHeight() * adjustedDensityRatio);
			}
			
			bitmap.setDensity(Bitmap.DENSITY_NONE);
			final Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, scaleFilter);
			
			bitmap2.setDensity(opts.inTargetDensity);
			
			if (bitmap != bitmap2) {
				bitmap.recycle();
			}
			return bitmap2;
		} else {
			return bitmap;
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
	
	@Override
	public BitmapDecoder scale(int width, int height, boolean scaleFilter) {
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("Both width and height should be positive.");
		}

		targetWidth = width;
		targetHeight = height;
		ratioWidth = ratioHeight = 1;
		
		this.scaleFilter = scaleFilter;
		return this;
	}
	
	@Override
	public BitmapDecoder scaleBy(float widthRatio, float heightRatio, boolean scaleFilter) {
		if (widthRatio <= 0 || heightRatio <= 0) {
			throw new IllegalArgumentException(MESSAGE_INVALID_RATIO);
		}
		
		if (targetWidth != 0 && targetHeight != 0) {
			return scale(
					(int) (targetWidth * widthRatio),
					(int) (targetHeight * heightRatio),
					scaleFilter);
		} else {
			this.ratioWidth = widthRatio;
			this.ratioHeight = heightRatio;
			this.scaleFilter = scaleFilter;
			return this;
		}
	}

	@Override
	public BitmapDecoder region(Rect region) {
		if (region == null) {
			if (this.region != null) {
				RECT.recycle(this.region);
			}
			this.region = null;
			return this;
		} else {
			return region(region.left, region.top, region.right, region.bottom);
		}
	}
	
	@Override
	public BitmapDecoder region(int left, int top, int right, int bottom) {
		if (this.region == null) {
			this.region = RECT.obtainNotReset();
		}
		this.region.set(left, top, right, bottom);
		
		return this;
	}
	
	@Override
	public ExternalBitmapDecoder mutable(boolean mutable) {
		this.mutable = mutable;
		return this;
	}
	
	protected abstract Bitmap decode(Options opts);
	protected abstract InputStream getInputStream();
	protected abstract BitmapRegionDecoder createBitmapRegionDecoder();
	
	protected void onDecodingStarted(boolean builtInDecoder) {
	}
	
	protected void onDecodingFinished() {
	}
	
	@SuppressLint("NewApi")
	protected Bitmap decodeRegional(Options opts, Rect region) {
		if (Build.VERSION.SDK_INT >= 10 && !useBuiltInDecoder) {
			adjustDensityRatio(false);
			
			final BitmapRegionDecoder d = createBitmapRegionDecoder();
			return (d == null ? null : d.decodeRegion(region, opts));
		} else {
			return aguDecode();
		}
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

	protected Bitmap aguDecode() {
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

	protected float getDensityRatio() {
		return 1f;
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
}
