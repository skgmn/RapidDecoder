package agu.scaling;

import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;
import static agu.caching.ResourcePool.CANVAS;
import agu.bitmap.BitmapDecoder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;

public class BitmapFrameBuilder {
	private BitmapDecoder decoder;
	private int frameWidth;
	private int frameHeight;
	private FrameAlignment align = FrameAlignment.CENTER;
	private FrameStrategy strategy = FrameStrategy.FIT;
	private Drawable background;
	
	public BitmapFrameBuilder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
		this.decoder = decoder;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
	}
	
	public BitmapFrameBuilder align(FrameAlignment align) {
		this.align = align;
		return this;
	}
	
	public BitmapFrameBuilder strategy(FrameStrategy strategy) {
		this.strategy = strategy;
		return this;
	}
	
	public BitmapFrameBuilder background(Drawable d) {
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
		final int width = decoder.sourceWidth();
		final int height = decoder.sourceHeight();
		
		if ((width == frameWidth && height == frameHeight) ||
				(strategy == FrameStrategy.ZOOM_OUT && 
						((!fitIn && (width <= frameWidth || height <= frameHeight)) ||
						 ( fitIn && (width <= frameWidth && height <= frameHeight)))) ||
				(strategy == FrameStrategy.ZOOM_IN &&
						((!fitIn && (width >= frameWidth && height >= frameHeight)) ||
						 ( fitIn && (width >= frameWidth || height >= frameHeight))))) {
					
			return decoder.decode();
		}
		
		final Rect bounds = RECT.obtainNotReset();
		try {
			AspectRatioCalculator.frame(width, height,
					frameWidth, frameHeight, align, fitIn, bounds);
			
			final int w = bounds.width();
			final int h = bounds.height();
			
			if (frameWidth == w && frameHeight == h) {
				return decoder.scale(frameWidth, frameHeight, true).decode();
			} else {
				final Bitmap bitmap = decoder.clone().scale(w, h, true).decode();
				
				final Bitmap bitmap2 = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
				final Canvas canvas = CANVAS.obtain(bitmap2);
				
				if (background != null) {
					canvas.save(Canvas.CLIP_SAVE_FLAG);
					canvas.clipRect(bounds, Op.DIFFERENCE);
					
					background.setBounds(0, 0, frameWidth, frameHeight);
					background.draw(canvas);
					
					canvas.restore();
				}
				
				final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
				canvas.drawBitmap(bitmap, bounds.left, bounds.top, p);
				PAINT.recycle(p);
				
				CANVAS.recycle(canvas);
				
				return bitmap2;
			}
		} finally {
			RECT.recycle(bounds);
		}
	}
}