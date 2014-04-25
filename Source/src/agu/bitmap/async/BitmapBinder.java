package agu.bitmap.async;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;


public abstract class BitmapBinder implements AsyncBitmapCallback {
	private ImageTurningOutEffect mEffect;
	private ImagePostProcessor mPostProcessor;
	private Drawable mFailImage;
	private int mFailImageResId; 
	
	Object singletonKey() {
		return null;
	}
	
	@Override
	public void onBitmapCancelled() {
	}
	
	protected ImageTurningOutEffect getEffect() {
		return (mEffect == null ? NoEffect.getInstance() : mEffect);
	}
	
	public BitmapBinder effect(ImageTurningOutEffect effect) {
		mEffect = effect;
		return this;
	}
	
	public BitmapBinder postProcess(ImagePostProcessor processor) {
		mPostProcessor = processor;
		return this;
	}
	
	public BitmapBinder failImage(Drawable d) {
		mFailImage = d;
		mFailImageResId = 0;
		return this;
	}
	
	public BitmapBinder failImage(int resId) {
		mFailImageResId = resId;
		mFailImage = null;
		return this;
	}
	
	protected Bitmap doPostProcess(Bitmap bitmap) {
		return (mPostProcessor == null ? bitmap : mPostProcessor.postProcess(bitmap));
	}
	
	protected Drawable getFailImage(Resources res) {
		if (mFailImageResId == 0) {
			return mFailImage;
		} else {
			return res.getDrawable(mFailImageResId);
		}
	}
}
