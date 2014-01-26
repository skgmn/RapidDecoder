package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import static agu.caching.ResourcePool.*;

public class LoadedBitmap implements BitmapSource {
	private Bitmap bitmap;
	private int targetWidth;
	private int targetHeight;
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
	public Bitmap bitmap() {
		final Bitmap regionalBitmap = (region == null ? bitmap :
			Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height()));
		final Bitmap scaledBitmap = (targetWidth == 0 && targetHeight == 0 ? regionalBitmap :
			Bitmap.createScaledBitmap(regionalBitmap, targetWidth, targetHeight, scaleFilter));
		
		return scaledBitmap;
	}

	@Override
	public BitmapSource scale(int width, int height, boolean scaleFilter) {
		this.targetWidth = width;
		this.targetHeight = height;
		this.scaleFilter = scaleFilter;
		
		return this;
	}

	@Override
	public BitmapSource scaleBy(double widthRatio, double heightRatio,
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
}
