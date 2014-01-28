package agu.drawable;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public final class DrawableUtils {
	public static void draw(Drawable d, Rect rectSrc, Canvas cv, int left, int top) {
		draw(d, rectSrc, cv, left, top, 0, 0);
	}
	
	public static void draw(Drawable d, Rect rectSrc, Canvas cv, int left, int top, int right, int bottom) {
		final int width = d.getIntrinsicWidth();
		final int height = d.getIntrinsicHeight();
		
		if (width == 0 || height == 0) {
			throw new IllegalArgumentException("Cannot get the size of drawable.");
		}
		
		if (right == 0) right = left + rectSrc.width();
		if (bottom == 0) bottom = top + rectSrc.height();

		if (rectSrc == null || rectSrc.contains(0, 0, width, height)) {
			d.setBounds(left, top, right, bottom);
			d.draw(cv);
			return;
		}
		
		int saveCount = -1;
		
		if (left > 0 || top > 0 || right < width || bottom < height) {
			saveCount = cv.save(Canvas.CLIP_SAVE_FLAG);
			cv.clipRect(left, top, right, bottom);
		}
		
		final float zoomH = (float) (right - left) / rectSrc.width();
		final float zoomV = (float) (bottom - top) / rectSrc.height();
		
		final int l = (int) (left - rectSrc.left * zoomH);
		final int t = (int) (top - rectSrc.top * zoomV);
		final int r = l + (int) (width * zoomH);
		final int b = t + (int) (height * zoomV);
		
		d.setBounds(l, t, r, b);
		d.draw(cv);
		
		if (saveCount != -1) {
			cv.restoreToCount(saveCount);
		}
	}
	
	public static void draw(Drawable d, Rect rectSrc, Canvas cv, Rect rectDest) {
		draw(d, rectSrc, cv, rectDest.left, rectDest.top, rectDest.right, rectDest.bottom);
	}
}
