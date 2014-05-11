package agu.bitmap;

import agu.scaling.AspectRatioCalculator;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import static agu.caching.ResourcePool.*;

public class InternalBitmapDecoder extends BitmapDecoder {
	private Bitmap bitmap;
	private boolean scaleFilter;
	private Rect region;
	private boolean mutable = false;
	private int targetWidth;
	private int targetHeight;
	private Config targetConfig;
	
	InternalBitmapDecoder(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	@Override
	public int sourceWidth() {
		return bitmap.getWidth();
	}

	@Override
	public int sourceHeight() {
		return bitmap.getHeight();
	}
	
	private Bitmap redraw(Bitmap bitmap, Rect rectSrc, int targetWidth, int targetHeight) {
		Config config = (targetConfig != null ? targetConfig : bitmap.getConfig());
		Bitmap bitmap2 = Bitmap.createBitmap(targetWidth, targetHeight, config);
		Canvas cv = CANVAS.obtain(bitmap2);
		
		Rect rectDest = RECT.obtain(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
		Paint paint = (scaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
		
		cv.drawBitmap(bitmap, rectSrc, rectDest, paint);
		
		if (paint != null) {
			PAINT.recycle(paint);
		}
		RECT.recycle(rectDest);
		
		CANVAS.recycle(cv);
		
		return (mutable ? bitmap2 : Bitmap.createBitmap(bitmap2));
	}
	
	@Override
	public Bitmap decode() {
		if (targetWidth == 0 || targetHeight == 0) {
			throw new IllegalArgumentException("Both width and height must be positive and non-zero.");
		}
		
		final boolean redraw = !((targetConfig == null || bitmap.getConfig().equals(targetConfig)) && !mutable);
		
		if (region != null) {
			if (targetWidth == 0 && targetHeight == 0) {
				if (!redraw) {
					return Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height());
				} else {
					return redraw(bitmap, region, region.width(), region.height());
				}
			} else {
				if (!redraw) {
					Matrix m = MATRIX.obtain();
					m.setScale(
							(float) targetWidth / region.width(),
							(float) targetHeight / region.height());
					
					Bitmap b = Bitmap.createBitmap(bitmap,
							region.left, region.top,
							region.width(), region.height(),
							m, scaleFilter);
					
					MATRIX.recycle(m);
					
					return b;
				} else {
					return redraw(bitmap, region, targetWidth, targetHeight);
				}
			}
		} else if (targetWidth != 0 && targetHeight != 0) {
			if (!redraw) {
				return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, scaleFilter);
			} else {
				return redraw(bitmap, null, targetWidth, targetHeight);
			}
		} else {
			if (!redraw) {
				return bitmap;
			} else {
				return redraw(bitmap, null, bitmap.getWidth(), bitmap.getHeight());
			}
		}
	}
	
	@Override
	public BitmapDecoder scale(int width, int height, boolean scaleFilter) {
		this.scaleFilter = scaleFilter;
		
		if (width == 0 && height != 0) {
			targetWidth = AspectRatioCalculator.fitHeight(sourceWidth(), sourceHeight(), height);
			targetHeight = height;
		} else if (height == 0 && width != 0) {
			targetWidth = width;
			targetHeight = AspectRatioCalculator.fitWidth(sourceWidth(), sourceHeight(), width);
		} else {
			targetWidth = width;
			targetHeight = height;
		}

		return this;
	}

	@Override
	public BitmapDecoder scaleBy(float widthRatio, float heightRatio,
			boolean scaleFilter) {
		
		if (widthRatio <= 0 || heightRatio <= 0) {
			throw new IllegalArgumentException(MESSAGE_INVALID_RATIO);
		}
		
		if (targetWidth != 0 && targetHeight != 0) {
			return scale(
					(int) (targetWidth * widthRatio),
					(int) (targetHeight * heightRatio),
					scaleFilter);
		} else {
			return scale(
					(int) (sourceWidth() * widthRatio),
					(int) (sourceHeight() * heightRatio),
					scaleFilter);
		}
	}

	@Override
	public BitmapDecoder region(int left, int top, int right, int bottom) {
		if (region == null) {
			region = RECT.obtainNotReset();
		}
		region.set(left, top, right, bottom);
		
		return this;
	}

	@Override
	public void draw(Canvas cv, Rect rectDest) {
		final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
		cv.drawBitmap(bitmap, region, rectDest, p);
		PAINT.recycle(p);
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (region != null) {
			RECT.recycle(region);
		}
		super.finalize();
	}

	@Override
	public void cancel() {
	}

	@Override
	public int width() {
		if (targetWidth != 0) {
			return targetWidth;
		} else if (region != null) {
			return region.width();
		} else {
			return sourceWidth();
		}
	}

	@Override
	public int height() {
		if (targetHeight != 0) {
			return targetHeight;
		} else if (region != null) {
			return region.height();
		} else {
			return sourceHeight();
		}
	}

	@Override
	public BitmapDecoder clone() {
		// TODO: Implement this
		return this;
	}

	@Override
	public BitmapDecoder region(Rect region) {
		if (region == null) {
			if (this.region != null) {
				RECT.recycle(region);
			}
			this.region = null;
		} else {
			region(region.left, region.top, region.right, region.bottom);
		}
		
		return this;
	}

	@Override
	public BitmapDecoder config(Config config) {
		targetConfig = config;
		return this;
	}

	@Override
	public BitmapDecoder useBuiltInDecoder(boolean force) {
		return this;
	}

	@Override
	public BitmapDecoder mutable(boolean mutable) {
		this.mutable = mutable;
		return this;
	}
	
	@Override
	public Rect region() {
		return region;
	} 
}
