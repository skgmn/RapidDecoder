package agu.bitmap.async;

import android.graphics.Bitmap;


public abstract class BitmapBinder implements AsyncBitmapCallback {
	private ImageTurningOutEffect mEffect;
	private ImagePostProcessor mPostProcessor;
	
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
	
	protected Bitmap doPostProcess(Bitmap bitmap) {
		return (mPostProcessor == null ? bitmap : mPostProcessor.postProcess(bitmap));
	}
}
