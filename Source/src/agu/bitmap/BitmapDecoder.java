package agu.bitmap;

import java.io.FileDescriptor;
import java.io.InputStream;

import agu.scaling.AspectRatioCalculator;
import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import static agu.ResourcePool.*;

public abstract class BitmapDecoder {
	public static final int SIZE_AUTO = 0;
	
	private Options opts;
	private Rect region;
	protected boolean mutable;
	
	private int width;
	private int height;
	private int targetWidth;
	private int targetHeight;
	private boolean scaleFilter = true;
	
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
	
	private void decodeSize() {
		opts.inJustDecodeBounds = true;
		decode(opts);
		opts.inJustDecodeBounds = false;
	}
	
	public int width() {
		if (width == 0) {
			decodeSize();
			width = opts.outWidth;
		}
		return width;
	}
	
	public int height() {
		if (height == 0) {
			decodeSize();
			height = opts.outHeight;
		}
		return height;
	}
	
	public Bitmap decode() {
		opts.mCancel = false;
		
		// Calculate sample size in advance when it should be scaled.
		
		final boolean postScale = (targetWidth != 0 && targetHeight != 0);
		
		if (postScale) {
			opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
					targetWidth, targetHeight);
			opts.inScaled = false;
		} else {
			opts.inSampleSize = 1;
			opts.inScaled = true;
		}

		// Execute actual decoding
		
		final Bitmap bitmap = executeDecoding();
		
		// Scale it finally.
		
		if (postScale &&
				(bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight)) {

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
	
	public Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
		opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
				targetWidth, targetHeight);
		opts.inScaled = false;
		
		return executeDecoding();
	}
	
	@SuppressLint("NewApi")
	private Bitmap executeDecoding() {
		final boolean useOwnDecoder =
				(mutable && Build.VERSION.SDK_INT < 11) ||
				(opts.inSampleSize > 1 && !scaleFilter);

		if (useOwnDecoder) {
			return aguDecode(openInputStream(), opts, region);
		} else {
			if (region != null &&
					!(region.left == 0 && region.top == 0 &&
					region.width() == width() && region.height() == height())) {
				
				return decodePartial(opts, region);
			} else {
				if (Build.VERSION.SDK_INT >= 11) {
					opts.inMutable = mutable;
				}
				return decode(opts);
			}
		}
	}
	
	private int regionWidth() {
		if (region == null) {
			return width();
		} else {
			return region.width();
		}
	}
	
	private int regionHeight() {
		if (region == null) {
			return height();
		} else {
			return region.height();
		}
	}
	
	public BitmapDecoder scale(int width, int height) {
		return scale(width, height, true);
	}
	
	public BitmapDecoder scale(int width, int height, boolean scaleFilter) {
		if (width == SIZE_AUTO || height == SIZE_AUTO) {
			if (width == SIZE_AUTO) {
				targetWidth = AspectRatioCalculator.fitHeight(width(), height(), height);
				targetHeight = height;
			} else {
				targetHeight = AspectRatioCalculator.fitWidth(width(), height(), width);
				targetWidth = width;
			}
		} else {
			targetWidth = width;
			targetHeight = height;
		}
		
		this.scaleFilter = scaleFilter;
		return this;
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
		left = Math.max(0, left);
		top = Math.max(0, top);
		right = Math.max(left, Math.min(right, left + width()));
		bottom = Math.max(top, Math.min(bottom, top + height()));
		
		if (this.region == null) {
			this.region = RECT.obtain();
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
	
	@SuppressLint("NewApi")
	protected Bitmap decodePartial(Options opts, Rect region) {
		final AguBitmapProcessor processor = createBitmapProcessor(opts, region);
		processor.preProcess();
		
		if (Build.VERSION.SDK_INT >= 10) {
			final BitmapRegionDecoder d = createBitmapRegionDecoder();
			if (d == null) {
				return null;
			} else {
				final Bitmap bitmap = d.decodeRegion(region, opts);
				return processor.postProcess(bitmap, scaleFilter);
			}
		} else {
			return aguDecodePreProcessed(openInputStream(), region, processor);
		}
	}
	
	protected AguBitmapProcessor createBitmapProcessor(Options opts, Rect region) {
		return new AguBitmapProcessor(opts, region);
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

	protected Bitmap aguDecode(InputStream in, Options opts, Rect region) {
		return aguDecodePreProcessed(in, region,
				createBitmapProcessor(opts, region).preProcess());
	}
	
	protected Bitmap aguDecodePreProcessed(InputStream in, Rect region,
			AguBitmapProcessor processor) {
		
		if (in == null) return null;
		
		final AguDecoder d = new AguDecoder(in);
		d.setRegion(region);
		d.setUseFilter(scaleFilter);
		d.setSampleSize(opts.inSampleSize);
		d.setConfig(opts.inPreferredConfig);
		
		final Bitmap bitmap = d.decode(opts);
		d.close();
		
		return processor.postProcess(bitmap, scaleFilter);
	}
	
	public void cancel() {
		opts.requestCancelDecode();
	}
	
	public void draw(Canvas cv, int left, int top, int right, int bottom) {
		final Rect bounds = RECT.obtain(false);
		try {
			bounds.set(left, top, right, bottom);
			draw(cv, bounds);
		} finally {
			RECT.recycle(bounds);
		}
	}
	
	public void draw(Canvas cv, Rect rectDest) {
		final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());
		
		final Paint p = PAINT.obtain();
		try {
			p.setFilterBitmap(true);
			cv.drawBitmap(bitmap, null, rectDest, p);
		} finally {
			PAINT.recycle(p);
		}
		
		bitmap.recycle();
	}
}
