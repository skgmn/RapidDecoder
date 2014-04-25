package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
		
		final Resources res = iv.getResources();
		
		bitmap = doPostProcess(bitmap);
		Drawable d = (bitmap != null ? new BitmapDrawable(res, bitmap) : getFailImage(res));
		getEffect().visit(iv, d);
	}
	
	@Override
	Object singletonKey() {
		return mImageView.get();
	}
}
