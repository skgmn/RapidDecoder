package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import static agu.caching.ResourcePool.*;

public class SimulatedDecoder implements Decoder {
	private Bitmap bitmap;
	
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
		return bitmap;
	}

	@Override
	public Decoder scale(int width, int height, boolean scaleFilter) {
		bitmap = Bitmap.createScaledBitmap(bitmap, width, height, scaleFilter);
		return this;
	}

	@Override
	public Decoder scaleBy(double widthRatio, double heightRatio,
			boolean scaleFilter) {
		
		bitmap = Bitmap.createScaledBitmap(bitmap,
				(int) (bitmap.getWidth() * widthRatio),
				(int) (bitmap.getHeight() * heightRatio),
				scaleFilter);
		return this;
	}

	@Override
	public Decoder region(int left, int top, int right, int bottom) {
		bitmap = Bitmap.createBitmap(bitmap, left, top, right, bottom);
		return this;
	}

	@Override
	public void draw(Canvas cv, Rect rectDest) {
		final Paint p = PAINT.obtain();
		try {
			p.setFilterBitmap(true);
			cv.drawBitmap(bitmap, null, rectDest, p);
		} finally {
			PAINT.recycle(p);
		}
	}
}
