package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public class TextViewBinder extends BitmapBinder {
	public static final int PLACE_LEFT = 1;
	public static final int PLACE_TOP = 2;
	public static final int PLACE_RIGHT = 4;
	public static final int PLACE_BOTTOM = 8;
	
	private WeakReference<TextView> mTextView;
	private int mPlace;
	
	public TextViewBinder(TextView tv, int place) {
		mTextView = new WeakReference<TextView>(tv);
		mPlace = place;
	}
	
	@Override
	public void onBitmapLoaded(Bitmap bitmap) {
		TextView tv = mTextView.get();
		if (tv == null) return;

		Drawable left = ((mPlace & PLACE_LEFT) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : null);
		Drawable top = ((mPlace & PLACE_TOP) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : null);
		Drawable right = ((mPlace & PLACE_RIGHT) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : null);
		Drawable bottom = ((mPlace & PLACE_BOTTOM) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : null);
		
		tv.setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
	}
}
