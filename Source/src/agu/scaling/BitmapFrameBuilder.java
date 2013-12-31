package agu.scaling;

import static agu.ResourcePool.PAINT;
import static agu.ResourcePool.RECT;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;

public class BitmapFrameBuilder {
	private Bitmap bitmap;
	private int frameWidth;
	private int frameHeight;
	private ScaleAlignment align = ScaleAlignment.CENTER;
	private boolean onlyWhenOverflowed = false;
	private Drawable background;
	
	public BitmapFrameBuilder(Bitmap bitmap, int frameWidth, int frameHeight) {
		this.bitmap = bitmap;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
	}
	
	public BitmapFrameBuilder setScaleAlignment(ScaleAlignment align) {
		this.align = align;
		return this;
	}
	
	public BitmapFrameBuilder setScaleOnlyWhenOverflowed(boolean on) {
		this.onlyWhenOverflowed = on;
		return this;
	}
	
	public BitmapFrameBuilder setBackground(Drawable d) {
		this.background = d;
		return this;
	}
	
	public Bitmap fitIn() {
		return scale(true);
	}
	
	public Bitmap cutOut() {
		return scale(false);
	}
	
	private Bitmap scale(boolean fitIn) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		
		if (width == frameWidth && height == frameHeight) {
			return bitmap;
		}
		
		final Rect bounds = RECT.obtain(false);
		try {
			if (onlyWhenOverflowed && 
					((!fitIn && (width <= frameWidth || height <= frameHeight)) ||
					 (fitIn && (width <= frameWidth && height <= frameHeight)))) {
				
				// If image is smaller than frame
				
				final boolean vert = (fitIn && width > height) ||
						(!fitIn && height > width);
				
				if (vert) {
					bounds.left = (frameWidth - width) / 2;
					bounds.right = bounds.left + width;
					
					if (align == ScaleAlignment.LEFT_OR_TOP) {
						bounds.top = 0;
					} else if (align == ScaleAlignment.RIGHT_OR_BOTTOM) {
						bounds.top = frameHeight - height;
					} else {
						bounds.top = (frameHeight - height) / 2;
					}
					
					bounds.bottom = bounds.top + height;
				} else {
					bounds.top = (frameHeight - height) / 2;
					bounds.bottom = bounds.top + height;
					
					if (align == ScaleAlignment.LEFT_OR_TOP) {
						bounds.left = 0;
					} else if (align == ScaleAlignment.RIGHT_OR_BOTTOM) {
						bounds.left = frameWidth - width;
					} else {
						bounds.left = (frameWidth - width) / 2;
					}
					
					bounds.right = bounds.left + width;
				}
				
				final Bitmap bitmap2 = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
				final Canvas cv = new Canvas(bitmap2);
				
				if (background != null) {
					cv.save(Canvas.CLIP_SAVE_FLAG);
					cv.clipRect(bounds, Op.DIFFERENCE);
					
					background.setBounds(0, 0, frameWidth, frameHeight);
					background.draw(cv);
					
					cv.restore();
				}
				
				final Paint p = PAINT.obtain();
				try {
					p.setFilterBitmap(true);
					cv.drawBitmap(bitmap2, null, bounds, p);
				} finally {
					PAINT.recycle(p);
				}
				
				return bitmap2;
			} else {
				AspectRatioCalculator.frame(bitmap.getWidth(), bitmap.getHeight(),
						frameWidth, frameHeight, align, fitIn, bounds);
				
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
			}
		} finally {
			RECT.recycle(bounds);
		}
	}
}
