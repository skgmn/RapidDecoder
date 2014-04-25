package agu.bitmap.async;

import agu.drawable.PlaceHolderDrawable;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class FadeInEffect extends ImageTurningOutEffect {
	private boolean mTransition;
	private int mDuration;
	
	public FadeInEffect() {
		this(500, false);
	}
	
	public FadeInEffect(int duration) {
		this(duration, false);
	}
	
	public FadeInEffect(int duration, boolean transition) {
		mTransition = transition;
		mDuration = duration;
	}
	
	@Override
	public void visit(ImageView iv, Bitmap bitmap) {
		if (!mTransition) {
			iv.setImageBitmap(bitmap);
			
			AlphaAnimation anim = new AlphaAnimation(0f, 1f);
			anim.setDuration(mDuration);
			iv.startAnimation(anim);
		} else {
			TransitionDrawable td = createFadingInDrawable(
					iv.getDrawable(),
					new BitmapDrawable(iv.getResources(), bitmap),
					mDuration);
			iv.setImageDrawable(td);
		}
	}

	@Override
	public void visit(TextView tv, int place, int width, int height, Bitmap bitmap) {
		Drawable[] drawables = tv.getCompoundDrawables();
		
		Drawable left = ((place & TextViewBinder.PLACE_LEFT) != 0
				? createFadingInDrawable(mTransition ? drawables[0] : null,
						new BitmapDrawable(tv.getResources(), bitmap),
						mDuration)
				: drawables[0]);
		Drawable top = ((place & TextViewBinder.PLACE_TOP) != 0
				? createFadingInDrawable(mTransition ? drawables[1] : null,
						new BitmapDrawable(tv.getResources(), bitmap),
						mDuration)
				: drawables[1]);
		Drawable right = ((place & TextViewBinder.PLACE_RIGHT) != 0
				? createFadingInDrawable(mTransition ? drawables[2] : null,
						new BitmapDrawable(tv.getResources(), bitmap),
						mDuration)
				: drawables[2]);
		Drawable bottom = ((place & TextViewBinder.PLACE_BOTTOM) != 0
				? createFadingInDrawable(mTransition ? drawables[3] : null,
						new BitmapDrawable(tv.getResources(), bitmap),
						mDuration)
				: drawables[3]);

		setDrawableSize(left, width, height);
		setDrawableSize(top, width, height);
		setDrawableSize(right, width, height);
		setDrawableSize(bottom, width, height);
		
		tv.setCompoundDrawables(left, top, right, bottom);
	}

	protected static TransitionDrawable createFadingInDrawable(Drawable from, Drawable to, int duration) {
		if (from == null) {
			from = new PlaceHolderDrawable();
			from.setBounds(0, 0, to.getIntrinsicWidth(), to.getIntrinsicHeight());
		}
		
		TransitionDrawable td = new TransitionDrawable(new Drawable[] { from, to });
		td.setBounds(0, 0, to.getIntrinsicWidth(), to.getIntrinsicHeight());
		td.startTransition(duration);
		return td;
	}
}
