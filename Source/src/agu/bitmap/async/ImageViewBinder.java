package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class ImageViewBinder extends BitmapBinder {
	private WeakReference<ImageView> mImageView;
	
	public ImageViewBinder(ImageView imageView) {
		mImageView = new WeakReference<ImageView>(imageView);
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap) {
		ImageView iv = mImageView.get();
		if (iv == null) return;
		
		getEffect().visit(iv, doPostProcess(bitmap));
	}
	
	@Override
	Object singletonKey() {
		return mImageView.get();
	}
}
