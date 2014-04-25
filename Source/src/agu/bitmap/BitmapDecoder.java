package agu.bitmap;

import static agu.caching.ResourcePool.OPTIONS;
import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;

import java.io.FileDescriptor;
import java.io.InputStream;

import agu.bitmap.decoder.AguDecoder;
import agu.scaling.AspectRatioCalculator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
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
	private int maxWidth = Integer.MAX_VALUE;
	private int maxHeight = Integer.MAX_VALUE;
	private double ratioWidth = 1;
	private double ratioHeight = 1;
	private double densityRatio;
	private double adjustedDensityRatio;
	
	protected BitmapDecoder() {
		opts = OPTIONS.obtain();
	}
	
	/**
	 * Recycle some resources. This method doesn't have to be called.
	 */
	public void recycle() {
		if (opts != null) {
			OPTIONS.recycle(opts);
			opts = null;
		}
		if (region != null) {
			RECT.recycle(region);
			region = null;
		}
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
	
	/**
	 * @return The estimated width of decoded image.
	 */
	public int width() {
		if (targetWidth != 0) {
			return targetWidth;
		} else if (region != null) {
			return (int) (region.width() * ratioWidth);
		} else {
			return (int) (sourceWidth() * getDensityRatio() * ratioWidth);
		}
	}

	/**
	 * @return The estimated height of decoded image.
	 */
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

		// Setup target size.
		
		int targetWidth;
		int targetHeight;

		if (this.targetWidth != 0 || this.targetHeight != 0) {
			targetWidth = this.targetWidth;
			targetHeight = this.targetHeight;

			if (targetWidth == 0 && targetHeight != 0) {
				targetWidth = AspectRatioCalculator.fitHeight(sourceWidth(), sourceHeight(), targetHeight);
			} else if (targetHeight == 0 && targetWidth != 0) {
				targetHeight = AspectRatioCalculator.fitWidth(sourceWidth(), sourceHeight(), targetWidth);
			}
		} else if (ratioWidth != 1 || ratioHeight != 1) {
			if (region != null) {
				targetWidth = (int) (region.width() * ratioWidth);
				targetHeight = (int) (region.height() * ratioHeight);
			} else {
				final double densityRatio = getDensityRatio();
				targetWidth = (int) (sourceWidth() * densityRatio * ratioWidth);
				targetHeight = (int) (sourceHeight() * densityRatio * ratioHeight);
			}
		} else {
			targetWidth = targetHeight = 0;
		}
		
		// Limit the size.
		
		if (maxWidth != Integer.MAX_VALUE || maxHeight != Integer.MAX_VALUE) {
			if (targetWidth == 0 || targetHeight == 0) {
				targetWidth = sourceWidth();
				targetHeight = sourceHeight();
			}
		}
		
		final boolean postScale = (targetWidth != 0 && targetHeight != 0);
		if (postScale) {
			if (targetWidth > maxWidth) {
				targetHeight = AspectRatioCalculator.fitWidth(targetWidth, targetHeight, maxWidth);
				targetWidth = maxWidth;
			}
			if (targetHeight > maxHeight) {
				targetWidth = AspectRatioCalculator.fitHeight(targetWidth, targetHeight, maxHeight);
				targetHeight = maxHeight;
			}
		}
		
		// Setup sample size.
		
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
		
		if (bitmap == null) return null;
		
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
	
	/**
	 * Equivalent to <code>scale(width, height, true)</code>.
	 */
	public BitmapDecoder scale(int width, int height) {
		return scale(width, height, true);
	}

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
	
	public BitmapDecoder limit(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Both width and height should be positive and non-zero.");
		}
		
		maxWidth = width;
		maxHeight = height;
		return this;
	}

	/**
	 * Equivalent to <code>scaleBy(ratio, ratio, true)</code>.
	 */
	public BitmapDecoder scaleBy(double ratio) {
		return scaleBy(ratio, ratio, true);
	}
	
	/**
	 * Equivalent to <code>scaleBy(ratio, ratio, scaleFilter)</code>.
	 */
	public BitmapDecoder scaleBy(double ratio, boolean scaleFilter) {
		return scaleBy(ratio, ratio, scaleFilter);
	}

	/**
	 * Equivalent to <code>scaleBy(widthRatio, heightRatio, true)</code>.
	 */
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
	
	/**
	 * Equivalent to <code>region(region.left, region.top, region.right, region.bottom)</code>.
	 */
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
	
	/**
	 * Equivalent to <code>mutable(true)</code>.
	 */
	public BitmapDecoder mutable() {
		return mutable(true);
	}

	/**
	 * <p>Tell the decoder whether decoded image should be mutable or not.</p>
	 * <p>It sets {@link BitmapFactory.Options#inMutable} to true on API level 11 or higher,
	 * otherwise it uses built-in decoder which always returns mutable bitmap.</p>
	 * @param mutable true if decoded image should be mutable.
	 */
	public BitmapDecoder mutable(boolean mutable) {
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
	
	public static BitmapDecoder from(Context context, Uri uri) {
		return new UriDecoder(context, uri);
	}

	protected Bitmap aguDecode() {
		final InputStream in = getInputStream();
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
	
	/**
	 * Equivalent to {@link #draw(Canvas, Rect)}.
	 */
	public void draw(Canvas cv, int left, int top, int right, int bottom) {
		final Rect bounds = RECT.obtain(left, top, right, bottom);
		draw(cv, bounds);
		RECT.recycle(bounds);
	}

	@Override
	public void draw(Canvas cv, Rect rectDest) {
		final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());
		
		final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
		cv.drawBitmap(bitmap, null, rectDest, p);
		PAINT.recycle(p);
		
		bitmap.recycle();
	}

	/**
	 * Equivalent to <code>useBuiltInDecoder(true)</code>.
	 */
	public BitmapDecoder useBuiltInDecoder() {
		return useBuiltInDecoder(true);
	}
	
	/**
	 * Tell the decoder to either force using built-in decoder or not.
	 * @param force true if it should always use built-in decoder.
	 */
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
	
	/**
	 * Set preferred bitmap configuration.
	 */
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
