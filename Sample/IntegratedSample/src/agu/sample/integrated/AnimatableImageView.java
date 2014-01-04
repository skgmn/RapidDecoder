package agu.sample.integrated;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AnimatableImageView extends ImageView {
	private Movie movie;
	private long movieStart = 0;
	private boolean animation = true;
	private SoftReference<Bitmap> buffer;
	
	public AnimatableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (isInEditMode()) return;
		
		int idGif = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", 0);
		if (idGif == 0) return;
		
		animation = attrs.getAttributeBooleanValue("http://net.suckga", "animation", true);
		
		setImageResource(idGif);
	}
	
	@Override
	public void setImageResource(int resId) {
		super.setImageResource(resId);

		if (animation) {
			InputStream istr = getContext().getResources().openRawResource(resId);
			try {
				movie = Movie.decodeStream(istr);
			}
			finally {
				try { istr.close(); } catch (IOException ioe) { }
			}
		}

		if (isInEditMode()) {
			invalidate();
		}
	}
	
	protected void onDraw(Canvas canvas) {
		if (isInEditMode() || !animation) {
			super.onDraw(canvas);
		}
		else {
			if (movie == null || movie.duration() == 0) {
				super.onDraw(canvas);
				return;
			}
	
			long now = android.os.SystemClock.uptimeMillis();
			if (movieStart == 0) {
				movieStart = now;
			}
			int relTime = (int) ((now - movieStart) % movie.duration());
			movie.setTime(relTime);
			
			if (movie.width() == getWidth() && movie.height() == getHeight()) {
				movie.draw(canvas, 0, 0);
			}
			else {
				Bitmap bmp = getBuffer(movie.width(), movie.height());
				movie.draw(new Canvas(bmp), 0, 0);
				
				canvas.drawBitmap(bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), new Rect(0, 0, getWidth(), getHeight()), null);
			}
			
			invalidate();
		}
	}
	
	private Bitmap getBuffer(int width, int height) {
		Bitmap bitmap = (buffer == null ? null : buffer.get());
		if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
			if (bitmap != null) {
				bitmap.recycle();
			}
			bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			
			buffer = new SoftReference<Bitmap>(bitmap);
		}
		return bitmap;
	}
	
	public void setAnimatable(boolean animatable) {
		this.animation = animatable;
		invalidate();
	}
}
