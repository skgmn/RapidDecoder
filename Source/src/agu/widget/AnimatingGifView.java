package agu.widget;

import agu.drawable.AnimatingGifDrawable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AnimatingGifView extends ImageView {
	private static final String NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
	
	public AnimatingGifView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public AnimatingGifView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public AnimatingGifView(Context context) {
		super(context);
	}
	
	private void init(AttributeSet attrs) {
		if (isInEditMode()) return;
		
		final int src = attrs.getAttributeResourceValue(NAMESPACE_ANDROID, "src", 0);
		if (src != 0) {
			setImageResource(src);
		}
	}
	
	@Override
	public void setImageResource(int resId) {
		try {
			final AnimatingGifDrawable d = new AnimatingGifDrawable(getResources(), resId);
			if (isInEditMode()) {
				d.stop();
			}
			
			super.setImageDrawable(d);
		} catch (IllegalArgumentException e) {
			super.setImageResource(resId);
		}
	}
	
	public void start() {
		final Drawable d = getDrawable();
		if (d instanceof AnimatingGifDrawable) {
			((AnimatingGifDrawable) d).start();
		}
	}
	
	public void stop() {
		final Drawable d = getDrawable();
		if (d instanceof AnimatingGifDrawable) {
			((AnimatingGifDrawable) d).stop();
		}
	}
}
