package agu.bitmap.async;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

public class NoEffect extends ImageTurningOutEffect {
	private static NoEffect sInstance;

	@Override
	public void visit(ImageView iv, Drawable d) {
		iv.setImageDrawable(d);
	}

	@Override
	public void visit(TextView tv, int index, int width, int height, Drawable d) {
		setDrawableSize(d, width, height);

		Drawable[] drawables = tv.getCompoundDrawables();
		drawables[index] = d;
		
		tv.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
	}
	
	public static synchronized NoEffect getInstance() {
		if (sInstance == null) {
			sInstance = new NoEffect();
		}
		return sInstance;
	}
}
