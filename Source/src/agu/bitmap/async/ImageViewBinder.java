package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

public class ImageViewBinder extends ViewBinder {
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
	protected View getView() {
		return mImageView.get();
	}
}