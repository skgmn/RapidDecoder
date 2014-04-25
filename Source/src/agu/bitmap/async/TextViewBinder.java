package agu.bitmap.async;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.widget.TextView;

public class TextViewBinder extends BitmapBinder {
	public static final int PLACE_LEFT = 1;
	public static final int PLACE_TOP = 2;
	public static final int PLACE_RIGHT = 4;
	public static final int PLACE_BOTTOM = 8;
	
	private WeakReference<TextView> mTextView;
	private int mPlace;
	private int mWidth;
	private int mHeight;
	
	public TextViewBinder(TextView tv, int place) {
		this(tv, place, 0, 0);
	}
	
	public TextViewBinder(TextView tv, int place, int width, int height) {
		mTextView = new WeakReference<TextView>(tv);
		mPlace = place;
		mWidth = width;
		mHeight = height;
	}
	
	@Override
	public void onBitmapLoaded(Bitmap bitmap) {
		TextView tv = mTextView.get();
		if (tv == null) return;
		
		getEffect().visit(tv, mPlace, mWidth, mHeight, doPostProcess(bitmap));
	}
}
