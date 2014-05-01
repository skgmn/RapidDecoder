package agu.bitmap;

import agu.scaling.AspectRatioCalculator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import static agu.caching.ResourcePool.*;

public class LoadedBitmap extends BitmapSource {
	private Bitmap bitmap;
	private int targetWidth;
	private int targetHeight;
	private int maxWidth = Integer.MAX_VALUE;
	private int maxHeight = Integer.MAX_VALUE;
	private boolean scaleFilter;
	private Rect region;
	
	public LoadedBitmap(Bitmap bitmap) {
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
	
	@Override
	public Bitmap decode() {
		if (targetWidth == 0 || targetHeight == 0) {
			throw new IllegalArgumentException("Both width and height must be positive and non-zero.");
		}
		
		if (region != null) {
			if (targetWidth == 0 && targetHeight == 0) {
				return Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height());
			} else {
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
			}
		} else if (targetWidth != 0 && targetHeight != 0) {
			return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, scaleFilter);
		} else {
			return bitmap;
		}
	}

	@Override
	public BitmapSource scale(int width, int height, boolean scaleFilter) {
		this.targetWidth = width;
		this.targetHeight = height;
		this.scaleFilter = scaleFilter;
		
		if (targetWidth == 0 && targetHeight != 0) {
			targetWidth = AspectRatioCalculator.fitHeight(sourceWidth(), sourceHeight(), targetHeight);
		} else if (targetHeight == 0 && targetWidth != 0) {
			targetHeight = AspectRatioCalculator.fitWidth(sourceWidth(), sourceHeight(), targetWidth);
		}

		fitInMaxSize();		
		return this;
	}

	@Override
	public BitmapSource scaleBy(float widthRatio, float heightRatio,
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
	public BitmapSource region(int left, int top, int right, int bottom) {
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
	public BitmapSource maxSize(int width, int height) {
		maxWidth = width;
		maxHeight = height;
		
		fitInMaxSize();
		return this;
	}
	
	private void fitInMaxSize() {
		if ((maxWidth != Integer.MAX_VALUE || maxHeight != Integer.MAX_VALUE) &&
				(targetWidth == 0 && targetHeight == 0)) {
			
			targetWidth = sourceWidth();
			targetHeight = sourceHeight();
		}
		
		if (targetWidth != 0 || targetHeight != 0) {
			if (targetWidth > maxWidth) {
				targetHeight = AspectRatioCalculator.fitWidth(targetWidth, targetHeight, maxWidth);
				targetWidth = maxWidth;
			}
			if (targetHeight > maxHeight) {
				targetWidth = AspectRatioCalculator.fitHeight(targetWidth, targetHeight, maxHeight);
				targetHeight = maxHeight;
			}
		}
	}

	@Override
	public BitmapSource clone() {
		// TODO: Implement this
		return this;
	}
}
