package agu.bitmap;

import agu.scaling.AspectRatioCalculator;
import agu.scaling.ScaleAlignment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public abstract class BitmapDecoder {
	private static final int BUFFER_LENGTH = 16384 + 1;
	
	public static final int SIZE_AUTO = 0;
	
	protected Options opts;
	
	private Rect bounds;
	private int frameWidth;
	private int frameHeight;
	
	private int width;
	private int height;
	
	private void decodeSize() {
		ensureOptions();
		opts.inJustDecodeBounds = true;
		decodeImpl();
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
		return decodeImpl();
	}
	
	public BitmapDecoder scale(int width, int height) {
		if (width == SIZE_AUTO || height == SIZE_AUTO) {
			if (width == SIZE_AUTO) {
				this.width = (int) (((double) width() / height()) * height);
				this.height = height;
			} else {
				this.height = (int) (((double) height() / width()) * width);
				this.width = width;
			}
		} else {
			this.width = width;
			this.height = height;
		}
		
		return this;
	}
	
	public BitmapDecoder fitIn(int width, int height) {
		return fitIn(width, height, ScaleAlignment.CENTER, null);
	}
	
	public BitmapDecoder fitIn(int width, int height, Drawable fill) {
		return fitIn(width, height, ScaleAlignment.CENTER, fill);
	}
	
	public BitmapDecoder fitIn(int width, int height, ScaleAlignment align, Drawable fill) {
		ensureBounds();
		AspectRatioCalculator.scale(width(), height(), width, height, align, true, bounds);
		
		frameWidth = width;
		frameHeight = height;
		
		return this;
	}
	
	private void ensureOptions() {
		if (opts == null) {
			opts = new Options();
		}
	}
	
	private void ensureBounds() {
		if (bounds != null) {
			bounds = new Rect();
		}
	}
	
	protected abstract Bitmap decodeImpl();

	private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
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
}
