package agu.scaling;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import static agu.ResourcePool.*;

public class BitmapFrame {
	public static Bitmap fitIn(Bitmap bitmap, int frameWidth, int frameHeight, Drawable background) {
		return fitIn(bitmap, frameWidth, frameHeight, ScaleAlignment.CENTER, background);
	}
	
	public static Bitmap fitIn(Bitmap bitmap, int frameWidth, int frameHeight, ScaleAlignment align,
			Drawable background) {
		
		final Rect bounds = RECT.obtain(false);
		try {
			AspectRatioCalculator.frame(bitmap.getWidth(), bitmap.getHeight(),
					frameWidth, frameHeight, align, true, bounds);
			
			final int w = bounds.width();
			final int h = bounds.height();
			
			if (bitmap.getWidth() == w && bitmap.getHeight() == h) {
				return bitmap;
			} else {
				final Bitmap bitmap2 = Bitmap.createBitmap(w, h, bitmap.getConfig());
				final Canvas canvas = new Canvas(bitmap2);
				
				if (background != null) {
					canvas.save(Canvas.CLIP_SAVE_FLAG);
					canvas.clipRect(bounds, Op.DIFFERENCE);
					
					background.setBounds(0, 0, w, h);
					background.draw(canvas);
					
					canvas.restore();
				}
				
				final Paint p = PAINT.obtain();
				try {
					p.setFilterBitmap(true);
					canvas.drawBitmap(bitmap, null, bounds, p);
				} finally {
					PAINT.recycle(p);
				}

				return bitmap2;
			}
		} finally {
			RECT.recycle(bounds);
		}
	}
}
