package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import static agu.caching.ResourcePool.*;

public class SimulatedDecoder implements Decoder {
	private Bitmap bitmap;
	private int targetWidth;
	private int targetHeight;
	private boolean scaleFilter;
	private Rect region;
	
	public SimulatedDecoder(Bitmap bitmap) {
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
		final Bitmap regionalBitmap = (region == null ? bitmap :
			Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height()));
		final Bitmap scaledBitmap = (targetWidth == 0 && targetHeight == 0 ? regionalBitmap :
			Bitmap.createScaledBitmap(regionalBitmap, targetWidth, targetHeight, scaleFilter));
		
		return scaledBitmap;
	}

	@Override
	public Decoder scale(int width, int height, boolean scaleFilter) {
		this.targetWidth = width;
		this.targetHeight = height;
		this.scaleFilter = scaleFilter;
		
		return this;
	}

	@Override
	public Decoder scaleBy(double widthRatio, double heightRatio,
			boolean scaleFilter) {
		
		this.targetWidth = (int) (bitmap.getWidth() * widthRatio);
		this.targetHeight = (int) (bitmap.getHeight() * heightRatio);
		this.scaleFilter = scaleFilter;
		
		return this;
	}

	@Override
	public Decoder region(int left, int top, int right, int bottom) {
		if (region == null) {
			region = RECT.obtain(false);
		}
		region.set(left, top, right, bottom);
		
		return this;
	}

	@Override
	public void draw(Canvas cv, Rect rectDest) {
		final Paint p = PAINT.obtain();
		try {
			p.setFilterBitmap(true);
			cv.drawBitmap(bitmap, region, rectDest, p);
		} finally {
			PAINT.recycle(p);
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (region != null) {
			RECT.recycle(region);
		}
		super.finalize();
	}
}
