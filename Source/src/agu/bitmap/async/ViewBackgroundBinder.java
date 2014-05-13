package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

public class ViewBackgroundBinder extends ViewBinder {
	private WeakReference<View> mView;
	
	public ViewBackgroundBinder(View v) {
		mView = new WeakReference<View>(v);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onBitmapLoaded(Bitmap bitmap) {
		View v = mView.get();
		if (v == null) return;
		
		Drawable d = new BitmapDrawable(v.getResources(), doPostProcess(bitmap));
		if (Build.VERSION.SDK_INT >= 16) {
			v.setBackground(d);
		} else {
			v.setBackgroundDrawable(d);
		}
	}
	
	@Override
	protected View getView() {
		return mView.get();
	}
}
