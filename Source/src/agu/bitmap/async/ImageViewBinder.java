package agu.bitmap.async;

import java.lang.ref.WeakReference;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.BitmapDecoderDelegate;
import agu.scaling.BitmapFrameBuilder;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class ImageViewBinder extends BitmapBinder {
	private WeakReference<ImageView> mImageView;
	
	public ImageViewBinder(ImageView imageView) {
		mImageView = new WeakReference<ImageView>(imageView);
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap) {
		final ImageView iv = mImageView.get();
		if (iv == null) return;
		
		final Resources res = iv.getResources();
		
		bitmap = doPostProcess(bitmap);
		Drawable d = (bitmap != null ? new BitmapDrawable(res, bitmap) : getFailImage(res));
		getEffect().visit(iv, d);
	}
	
	@Override
	Object singletonKey() {
		return mImageView.get();
	}
	
	@Override
	public void execute(final AsyncBitmapLoaderJob job) {
		final ImageView iv = mImageView.get();
		if (iv == null) return;
		
		if (iv.getWidth() == 0 || iv.getHeight() == 0) {
			iv.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					iv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					if (job.isValid()) {
						startLoad(iv, job);
					}
				}
			});
		} else {
			startLoad(iv, job);
		}
	}
	
	private void startLoad(ImageView iv, AsyncBitmapLoaderJob job) {
		final int width = iv.getWidth();
		final int height = iv.getHeight();
		
		job.setDelegate(new BitmapDecoderDelegate() {
			@Override
			public Bitmap decode(BitmapDecoder decoder) {
				if (mFrameMode == null) {
					return decoder.clone().scale(width, height).decode();
				} else {
					return new BitmapFrameBuilder(decoder, width, height)
						.setOptions(mFrameOptions)
						.build(mFrameMode);
				}
			}
		});
		job.start();
	}
}