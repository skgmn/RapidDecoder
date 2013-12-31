package agu.bitmap;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;

public class AguBitmapProcessor {
	protected Options opts;
	protected Rect region;
	
	private double scaleFactor;
	private InputStream in;
	
	public AguBitmapProcessor(Options opts, Rect region) {
		this.opts = opts;
		this.region = region;
		this.scaleFactor = 1;
	}
	
	public AguBitmapProcessor preProcess() {
		if (opts.inDensity != 0 && opts.inTargetDensity != 0 &&
				opts.inDensity != opts.inTargetDensity) {
			
			double scaleFactor = (double) opts.inTargetDensity / opts.inDensity;
			
			if (region != null) {
				region.left = (int) (region.left / scaleFactor);
				region.top = (int) (region.top / scaleFactor);
				region.right = (int) (region.right / scaleFactor);
				region.bottom = (int) (region.bottom / scaleFactor);
			}

			if (opts.inScaled) {
				if (scaleFactor <= 0.5) {
					if (opts.inSampleSize < 1) {
						opts.inSampleSize = 1;
					}
					while (scaleFactor <= 0.5) {
						opts.inSampleSize *= 2;
						scaleFactor *= 2;
					}
				}
				this.scaleFactor = scaleFactor;
			}
		}
		
		return this;
	}
	
	public Bitmap postProcess(Bitmap bitmap, boolean filter) {
		if (bitmap == null) return null;
		
		if (scaleFactor != 1) {
			if (opts.inDensity != 0) {
				bitmap.setDensity(opts.inDensity);
			}

			final int newWidth = (int) (bitmap.getWidth() * scaleFactor);
			final int newHeight = (int) (bitmap.getHeight() * scaleFactor);
			
			final Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, filter);
			
			bitmap2.setDensity(opts.inTargetDensity);
			bitmap.recycle();
			
			return bitmap2;
		} else {
			if (opts.inTargetDensity != 0) {
				bitmap.setDensity(opts.inTargetDensity);
			}
			return bitmap;
		}
	}

	public double getScaleFactor() {
		return scaleFactor;
	}
	
	public InputStream getInputStream() {
		return in;
	}
}
