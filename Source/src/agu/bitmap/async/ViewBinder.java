package agu.bitmap.async;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.BitmapDecoderDelegate;
import agu.scaling.BitmapFrameBuilder;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public abstract class ViewBinder extends BitmapBinder {
	protected abstract View getView();

	@Override
	Object singletonKey() {
		return getView();
	}
	
	@Override
	public void execute(final AsyncBitmapLoaderJob job) {
		final View v = getView();
		if (v == null) return;
		
		if (mFrameMode != null && (v.getWidth() == 0 || v.getHeight() == 0)) {
			v.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					if (job.isValid()) {
						startLoad(v, job);
					}
				}
			});
		} else {
			startLoad(v, job);
		}
	}
	
	private void startLoad(View v, AsyncBitmapLoaderJob job) {
		if (mFrameMode != null) {
			final int width = v.getWidth();
			final int height = v.getHeight();
			job.setDelegate(new BitmapDecoderDelegate() {
				@Override
				public Bitmap decode(BitmapDecoder decoder) {
					return new BitmapFrameBuilder(decoder, width, height, mFrameOptions).build(mFrameMode);
				}
			});
		}
		job.start();
	}
}
