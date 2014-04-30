package agu.bitmap;

import static agu.caching.ResourcePool.CANVAS;
import static agu.caching.ResourcePool.RECT;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;

public class TextBitmap {
	public static Bitmap createTextBitmap(String text, Paint p, Rect outTextBounds) {
		final Rect textBounds = (outTextBounds != null ? outTextBounds : RECT.obtainNotReset());
    	p.getTextBounds(text, 0, text.length(), textBounds);
    	
    	Bitmap bitmap = Bitmap.createBitmap(textBounds.width(), textBounds.height(), Config.ARGB_4444);
    	final Canvas cv = CANVAS.obtain(bitmap);
    	
    	final int left;
    	final Align align = p.getTextAlign();
    	if (align.equals(Align.CENTER)) {
    		left = bitmap.getWidth() / 2;
    	} else if (align.equals(Align.RIGHT)) {
    		left = bitmap.getWidth();
    	} else {
    		left = 0;
    	}
    	
    	cv.drawText(text, left - textBounds.left, -textBounds.top, p);
    	CANVAS.recycle(cv);

    	if (outTextBounds == null) {
    		RECT.recycle(textBounds);
    	}
		
		return bitmap;
	}
	
	public static int getLeft(int left, Rect textBounds) {
		return left + textBounds.left;
	}
	
	public static int getTop(int top, Rect textBounds) {
		return top + textBounds.top;
	}
	
	public static void getCoord(int left, int top, Rect textBounds, Point out) {
		out.x = left + textBounds.left;
		out.y = top + textBounds.top;
	}
}
