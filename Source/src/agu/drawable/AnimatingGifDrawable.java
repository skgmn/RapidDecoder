package agu.drawable;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import static agu.caching.ResourcePool.*;

public class AnimatingGifDrawable extends Drawable {
	private Movie movie;
	private long movieStart = 0;
	private float scaleHorizontal = 1;
	private float scaleVertical = 1;
	private boolean animating = true;
	private Paint paint;
	private int alpha = 0xff;
	
	public AnimatingGifDrawable(Resources res, int id) {
		this(res.openRawResource(id), true);
	}

	public AnimatingGifDrawable(InputStream in) {
		this(in, false);
	}
	
	public AnimatingGifDrawable(InputStream in, boolean close) {
		try {
			movie = Movie.decodeStream(in);
		} finally {
			if (close) {
				try { in.close(); } catch (IOException e) {}
			}
		}
			
		if (movie == null) {
			throw new IllegalArgumentException("Not an animated gif file.");
		}
		
		movie.setTime(0);
	}
	
	@Override
	public void draw(Canvas cv) {
		if (animating) {
			final long now = SystemClock.uptimeMillis();
			if (movieStart == 0) {
				movieStart = now;
			}

			final int elapsed = (int) ((now - movieStart) % movie.duration());
			movie.setTime(elapsed);
		}
		
		final Rect bounds = getBounds();
		
		cv.drawColor(0xff000000);
		
		final boolean scale = (movie.width() != bounds.width() || movie.height() != bounds.height());
		
		if (scale) {
			cv.save(Canvas.MATRIX_SAVE_FLAG);
			cv.scale(scaleHorizontal, scaleVertical, bounds.left, bounds.top);
		}
		movie.draw(cv, bounds.left, bounds.top, paint);
		if (scale) {
			cv.restore();
		}
		
		if (animating) {
			invalidateSelf();
		}
	}
	
	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
		
		scaleHorizontal = (float) (right - left) / movie.width();
		scaleVertical = (float) (bottom - top) / movie.height();
	}

	@Override
	public int getOpacity() {
//		return movie.isOpaque() ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int alpha) {
		if (this.alpha != alpha) {
			this.alpha = alpha;
			
			if (alpha == 0xff) {
				if (paint != null) {
					PAINT.recycle(paint);
				}
				paint = null;
			} else {
				if (paint == null) {
					paint = PAINT.obtain();
				}
				paint.setAlpha(alpha);
			}
			
			invalidateSelf();
		}
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (paint != null) {
			PAINT.recycle(paint);
		}
		super.finalize();
	}
	
	public void start() {
		if (!animating) {
			animating = true;
			invalidateSelf();
		}
	}
	
	public void stop() {
		animating = false;
		movieStart = 0;
	}
	
	@Override
	public int getIntrinsicWidth() {
		return movie.width();
	}
	
	@Override
	public int getIntrinsicHeight() {
		return movie.height();
	}
}
