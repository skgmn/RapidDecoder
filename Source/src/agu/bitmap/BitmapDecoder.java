package agu.bitmap;

import static agu.caching.ResourcePool.OPTIONS;
import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;

import java.io.FileDescriptor;
import java.io.InputStream;

import agu.bitmap.decoder.AguDecoder;
import agu.scaling.AspectRatioCalculator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

public abstract class BitmapDecoder implements BitmapSource {
	public static final int SIZE_AUTO = 0;

	Options opts;
	Rect region;
	protected boolean mutable;
	private boolean scaleFilter = true;
	private boolean useBuiltInDecoder = false;
	
	private int width;
	private int height;
	private int targetWidth;
	private int targetHeight;
	private double ratioWidth = 1;
	private double ratioHeight = 1;
	private double densityRatio;
	private double adjustedDensityRatio;
	
	protected BitmapDecoder() {
		opts = OPTIONS.obtain();
	}
	
	public void release() {
		if (opts != null) {
			OPTIONS.recycle(opts);
			opts = null;
		}
		if (region != null) {
			RECT.recycle(region);
			region = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		release();
		super.finalize();
	}
	
	protected void decodeBounds() {
		opts.inJustDecodeBounds = true;
		decode(opts);
		opts.inJustDecodeBounds = false;

		width = opts.outWidth;
		height = opts.outHeight;

		if (opts.inDensity != 0 && opts.inTargetDensity != 0) {
			densityRatio = (double) opts.inTargetDensity / opts.inDensity;
		} else {
			densityRatio = 1;
		}
	}
	
	public int sourceWidth() {
		if (width == 0) {
			decodeBounds();
		}
		return width;
	}
	
	public int sourceHeight() {
		if (height == 0) {
			decodeBounds();
		}
		return height;
	}
	
	public int width() {
		if (targetWidth != 0) {
			return targetWidth;
		} else if (region != null) {
			return (int) (region.width() * ratioWidth);
		} else {
			return (int) (sourceWidth() * getDensityRatio() * ratioWidth);
		}
	}

	public int height() {
		if (targetHeight != 0) {
			return targetHeight;
		} else if (region != null) {
			return (int) (region.height() * ratioHeight);
		} else {
			return (int) (sourceHeight() * getDensityRatio() * ratioHeight);
		}
	}
	
	@Override
	public Bitmap bitmap() {
		return decode();
	}
	
	public Bitmap decode() {
		// reset
		
		opts.mCancel = false;
		adjustedDensityRatio = 0;

		//
		
		final int targetWidth;
		final int targetHeight;
		
		// Setup target size.
		
		if (this.targetWidth != 0 && this.targetHeight != 0) {
			targetWidth = this.targetWidth;
			targetHeight = this.targetHeight;
		} else if (ratioWidth != 1 || ratioHeight != 1) {
			if (region != null) {
				targetWidth = (int) Math.round(region.width() * ratioWidth);
				targetHeight = (int) Math.round(region.height() * ratioHeight);
			} else {
				final double densityRatio = getDensityRatio();
				targetWidth = (int) Math.round(sourceWidth() * densityRatio * ratioWidth);
				targetHeight = (int) Math.round(sourceHeight() * densityRatio * ratioHeight);
			}
		} else {
			targetWidth = targetHeight = 0;
		}
		
		// Setup sample size.
		
		final boolean postScale = (targetWidth != 0 && targetHeight != 0);
		if (postScale) {
			opts.inScaled = false;
			opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
					targetWidth, targetHeight);
		} else {
			opts.inScaled = true;
			opts.inSampleSize = 1;
		}
		
		// Execute actual decoding
		
		final Bitmap bitmap = executeDecoding();
		if (bitmap == null) return null;
		
		// Scale it finally.
		
		if (postScale && (bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight)) {
			bitmap.setDensity(Bitmap.DENSITY_NONE);
			final Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, scaleFilter);
			
			bitmap.recycle();

			if (opts.inTargetDensity != 0) {
				bitmap2.setDensity(opts.inTargetDensity);
			}
			return bitmap2;
		} else {
			return bitmap;
		}
	}
	
	private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
		opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
				targetWidth, targetHeight);
		opts.inScaled = false;
		
		return executeDecoding();
	}
	
	/**
	 * Simulate native decoding.
	 * @param ctx
	 * @return
	 */
	@SuppressLint("NewApi")
	private Bitmap executeDecoding() {
		// Adjust region.
		
		final Rect region;
		final boolean recycleRegion;
		
		if (this.region != null && getDensityRatio() != 1) {
			final double densityRatio = getDensityRatio();
			
			region = RECT.obtainNotReset();
			
			region.left = (int) Math.round(this.region.left / densityRatio);
			region.top = (int) Math.round(this.region.top / densityRatio);
			region.right = (int) Math.round(this.region.right / densityRatio);
			region.bottom = (int) Math.round(this.region.bottom / densityRatio);
			
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
		
		if (bitmap == null) return null;
		
		if (opts.inScaled) {
			final int newWidth = (int) Math.round(bitmap.getWidth() * adjustedDensityRatio);
			final int newHeight = (int) Math.round(bitmap.getHeight() * adjustedDensityRatio);
			
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
	
	public BitmapDecoder scale(int width, int height) {
		return scale(width, height, true);
	}
	
	public BitmapDecoder scale(int width, int height, boolean scaleFilter) {
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("Both width and height should be positive.");
		}

		if ((width == SIZE_AUTO && height == SIZE_AUTO) ||
				(width != SIZE_AUTO && height != SIZE_AUTO)) {
			
			targetWidth = width;
			targetHeight = height;
		} else {
			if (width == SIZE_AUTO) {
				targetWidth = AspectRatioCalculator.fitHeight(sourceWidth(), sourceHeight(), height);
				targetHeight = height;
			} else {
				targetHeight = AspectRatioCalculator.fitWidth(sourceWidth(), sourceHeight(), width);
				targetWidth = width;
			}
		}
		
		ratioWidth = ratioHeight = 1;
		
		this.scaleFilter = scaleFilter;
		return this;
	}

	public BitmapDecoder scaleBy(double ratio) {
		return scaleBy(ratio, ratio, true);
	}
	
	public BitmapDecoder scaleBy(double ratio, boolean scaleFilter) {
		return scaleBy(ratio, ratio, scaleFilter);
	}

	public BitmapDecoder scaleBy(double widthRatio, double heightRatio) {
		return scaleBy(widthRatio, heightRatio, true);
	}
	
	@Override
	public BitmapDecoder scaleBy(double widthRatio, double heightRatio, boolean scaleFilter) {
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
	
	public BitmapDecoder region(int left, int top, int right, int bottom) {
		if (this.region == null) {
			this.region = RECT.obtainNotReset();
		}
		this.region.set(left, top, right, bottom);
		
		return this;
	}
	
	public BitmapDecoder mutable() {
		return mutable(true);
	}
	
	public BitmapDecoder mutable(boolean mutable) {
		this.mutable = mutable;
		return this;
	}
	
	protected abstract Bitmap decode(Options opts);
	protected abstract InputStream openInputStream();
	protected abstract BitmapRegionDecoder createBitmapRegionDecoder();
	
	protected void onDecodingStarted(boolean builtInDecoder) {
	}
	
	protected void onDecodingFinished() {
	}
	
	@SuppressLint("NewApi")
	protected Bitmap decodeRegional(Options opts, Rect region) {
		if (Build.VERSION.SDK_INT >= 10 && !useBuiltInDecoder) {
			adjustDensityRatio();
			
			final BitmapRegionDecoder d = createBitmapRegionDecoder();
			if (d == null) {
				return null;
			} else {
				return d.decodeRegion(region, opts);
			}
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
	
	public static BitmapDecoder from(byte[] data) {
		return new ByteArrayDecoder(data, 0, data.length);
	}
	
	public static BitmapDecoder from(byte[] data, int offset, int length) {
		return new ByteArrayDecoder(data, offset, length);
	}
	
	public static BitmapDecoder from(Resources res, int id) {
		return new ResourceDecoder(res, id);
	}
	
	public static BitmapDecoder from(String pathName) {
		return new FileDecoder(pathName);
	}
	
	public static BitmapDecoder from(FileDescriptor fd) {
		return new FileDescriptorDecoder(fd);
	}
	
	public static BitmapDecoder from(InputStream in) {
		return new StreamDecoder(in);
	}

	protected Bitmap aguDecode() {
		final InputStream in = openInputStream();
		if (in == null) return null;
		
		adjustDensityRatio();
		
		final AguDecoder d = new AguDecoder(in);
		d.setRegion(region);
		d.setUseFilter(scaleFilter);
		d.setSampleSize(opts.inSampleSize);
		
		final Bitmap bitmap = d.decode(opts);
		d.close();
		
		return bitmap;
	}
	
	public void cancel() {
		opts.requestCancelDecode();
	}

	public void draw(Canvas cv, int left, int top) {
		draw(cv, left, top, left + width(), top + height());
	}
	
	public void draw(Canvas cv, int left, int top, int right, int bottom) {
		final Rect bounds = RECT.obtain(left, top, right, bottom);
		draw(cv, bounds);
		RECT.recycle(bounds);
	}
	
	public void draw(Canvas cv, Rect rectDest) {
		final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());
		
		final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
		cv.drawBitmap(bitmap, null, rectDest, p);
		PAINT.recycle(p);
		
		bitmap.recycle();
	}

	public BitmapDecoder useBuiltInDecoder() {
		return useBuiltInDecoder(true);
	}
	
	public BitmapDecoder useBuiltInDecoder(boolean force) {
		this.useBuiltInDecoder = force;
		return this;
	}

	private double getDensityRatio() {
		if (densityRatio == 0) {
			decodeBounds();
		}
		return densityRatio;
	}
	
	public BitmapDecoder config(Config config) {
		opts.inPreferredConfig = config;
		return this;
	}
	
	private void adjustDensityRatio() {
		if (adjustedDensityRatio == 0) {
			adjustedDensityRatio = getDensityRatio();
			
			while (adjustedDensityRatio <= 0.5) {
				opts.inSampleSize *= 2;
				adjustedDensityRatio *= 2;
			}
		}
	}
}
