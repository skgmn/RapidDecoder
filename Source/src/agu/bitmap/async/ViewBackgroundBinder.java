package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.View;

public class ViewBackgroundBinder extends BitmapBinder {
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
		
		if (Build.VERSION.SDK_INT >= 16) {
			v.setBackground(new BitmapDrawable(v.getResources(), bitmap));
		} else {
			v.setBackgroundDrawable(new BitmapDrawable(v.getResources(), bitmap));
		}
	}
	
	@Override
	public Object getSingletonKey() {
		return mView.get();
	}
}
