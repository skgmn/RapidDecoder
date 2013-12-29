package agu.bitmap;

import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;

public abstract class BitmapDecoder {
	public static final int SIZE_AUTO = 0;
	
	private Options opts;
	private Rect region;
	
	private int width;
	private int height;
	private int targetWidth;
	private int targetHeight;
	
	private void decodeSize() {
		ensureOptions();
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
		final boolean postScale = (targetWidth != 0 && targetHeight != 0);
		
		if (postScale) {
			ensureOptions();
			opts.inSampleSize = calculateInSampleSize(width(), height(), targetWidth, targetHeight);
			opts.inScaled = false;
		} else {
			if (opts != null) {
				opts.inScaled = true;
			}
		}
		
		Bitmap bitmap;
		if (region == null) {
			bitmap = decode(opts);
		} else {
			bitmap = decodePartial(opts, region);
		}
		
		if (postScale) {
			final Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
			bitmap.recycle();
			return bitmap2;
		} else {
			return bitmap;
		}
	}
	
	public BitmapDecoder scale(int width, int height) {
		if (width == SIZE_AUTO || height == SIZE_AUTO) {
			if (width == SIZE_AUTO) {
				targetWidth = (int) (((double) width() / height()) * height);
				targetHeight = height;
			} else {
				targetHeight = (int) (((double) height() / width()) * width);
				targetWidth = width;
			}
		} else {
			targetWidth = width;
			targetHeight = height;
		}
		
		return this;
	}
	
	public BitmapDecoder region(Rect region) {
		if (region == null) {
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
			this.region = new Rect(left, top, right, bottom);
		} else {
			this.region.set(left, top, right, bottom);
		}
		
		return this;
	}
	
	private void ensureOptions() {
		if (opts == null) {
			opts = new Options();
		}
	}
	
	protected abstract Bitmap decode(Options opts);
	protected abstract Bitmap decodePartial(Options opts, Rect region);

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

	protected static Bitmap aguDecode(InputStream in, Options opts, Rect region) {
		return aguDecodePreProcessed(in, opts, region, new AguBitmapProcessor(opts, region).preProcess());
	}
	
	protected static Bitmap aguDecodePreProcessed(InputStream in, Options opts, Rect region, AguBitmapProcessor processor) {
		final AguDecoder d = new AguDecoder(in);
		d.setRegion(region);
		d.setSampleSize(opts.inSampleSize);
		
		if (opts != null) {
			d.setConfig(opts.inPreferredConfig);
		}
		
		final Bitmap bitmap = d.decode();
		d.close();
		
		return processor.postProcess(bitmap);
	}
}
