package agu.bitmap.async;

import agu.drawable.PlaceHolderDrawable;
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
	public void visit(ImageView iv, Drawable d) {
		if (!mTransition) {
			iv.setImageDrawable(d);
			
			AlphaAnimation anim = new AlphaAnimation(0f, 1f);
			anim.setDuration(mDuration);
			iv.startAnimation(anim);
		} else {
			TransitionDrawable td = createFadingInDrawable(iv.getDrawable(), d, mDuration);
			iv.setImageDrawable(td);
		}
	}

	@Override
	public void visit(TextView tv, int index, int width, int height, Drawable d) {
		setDrawableSize(d, width, height);

		Drawable[] drawables = tv.getCompoundDrawables();
		drawables[index] = createFadingInDrawable(drawables[index], d, mDuration);
		
		tv.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
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
