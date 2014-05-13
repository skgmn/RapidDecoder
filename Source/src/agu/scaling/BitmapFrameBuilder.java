package agu.scaling;

import static agu.caching.ResourcePool.CANVAS;
import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;
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
	private FrameOptions options = new FrameOptions();
	
	public BitmapFrameBuilder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
		this.decoder = decoder;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
	}
	
	private BitmapFrameBuilder(BitmapFrameBuilder other) {
		decoder = other.decoder;
		frameWidth = other.frameWidth;
		frameHeight = other.frameHeight;
		options.set(other.options);
	}
	
	public BitmapFrameBuilder align(FrameAlignment align) {
		options.align = align;
		return this;
	}
	
	public BitmapFrameBuilder strategy(FrameStrategy strategy) {
		options.strategy = strategy;
		return this;
	}
	
	public BitmapFrameBuilder background(Drawable d) {
		options.background = d;
		return this;
	}
	
	public BitmapFrameBuilder setOptions(FrameOptions options) {
		this.options.set(options);
		return this;
	}

	public Bitmap build(FrameMode mode) {
		return scale(mode == FrameMode.FIT_IN);
	}
	
	private Bitmap scale(boolean fitIn) {
		final int width = decoder.sourceWidth();
		final int height = decoder.sourceHeight();
		
		if ((width == frameWidth && height == frameHeight) ||
				(options.strategy == FrameStrategy.ZOOM_OUT_ONLY && 
						((!fitIn && (width <= frameWidth || height <= frameHeight)) ||
						 ( fitIn && (width <= frameWidth && height <= frameHeight)))) ||
				(options.strategy == FrameStrategy.ZOOM_IN_ONLY &&
						((!fitIn && (width >= frameWidth && height >= frameHeight)) ||
						 ( fitIn && (width >= frameWidth || height >= frameHeight))))) {
					
			return decoder.decode();
		}
		
		final Rect bounds = RECT.obtainNotReset();
		try {
			AspectRatioCalculator.frame(width, height,
					frameWidth, frameHeight, options.align, fitIn, bounds);
			
			final int w = bounds.width();
			final int h = bounds.height();
			
			if (frameWidth == w && frameHeight == h) {
				return decoder.scale(frameWidth, frameHeight, true).decode();
			} else {
				final Bitmap bitmap = decoder.clone().scale(w, h, true).decode();
				
				final Bitmap bitmap2 = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
				final Canvas canvas = CANVAS.obtain(bitmap2);
				
				if (options.background != null) {
					canvas.save(Canvas.CLIP_SAVE_FLAG);
					canvas.clipRect(bounds, Op.DIFFERENCE);
					
					options.background.setBounds(0, 0, frameWidth, frameHeight);
					options.background.draw(canvas);
					
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

	@Override
	public BitmapFrameBuilder clone() {
		return new BitmapFrameBuilder(this);
	}
}