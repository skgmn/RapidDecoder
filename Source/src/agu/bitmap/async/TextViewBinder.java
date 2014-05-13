package agu.bitmap.async;

import java.lang.ref.WeakReference;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.BitmapDecoderDelegate;
import agu.scaling.BitmapFrameBuilder;
import android.content.res.Resources;
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
		
		final Resources res = tv.getResources();
		
		bitmap = doPostProcess(bitmap);
		
		final ImageTurningOutEffect effect = getEffect();
		
		if ((mPlace & PLACE_LEFT) != 0) effect.visit(tv, 0, mWidth, mHeight, createDrawable(res, bitmap)); 
		if ((mPlace & PLACE_TOP) != 0) effect.visit(tv, 1, mWidth, mHeight, createDrawable(res, bitmap)); 
		if ((mPlace & PLACE_RIGHT) != 0) effect.visit(tv, 2, mWidth, mHeight, createDrawable(res, bitmap)); 
		if ((mPlace & PLACE_BOTTOM) != 0) effect.visit(tv, 3, mWidth, mHeight, createDrawable(res, bitmap)); 
	}
	
	private Drawable createDrawable(Resources res, Bitmap bitmap) {
		return (bitmap != null ? new BitmapDrawable(res, bitmap) : getFailImage(res).mutate());
	}
	
	@Override
	public void execute(final AsyncBitmapLoaderJob job) {
		job.setDelegate(new BitmapDecoderDelegate() {
			@Override
			public Bitmap decode(BitmapDecoder decoder) {
				if (mWidth != 0 && mHeight != 0 && mFrameMode != null) {
					return new BitmapFrameBuilder(decoder, mWidth, mHeight, mFrameOptions).build(mFrameMode);
				} else {
					return decoder.decode();
				}
			}
		});
		job.start();
	}
}
