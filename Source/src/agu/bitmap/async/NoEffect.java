package agu.bitmap.async;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

public class NoEffect extends ImageTurningOutEffect {
	private static NoEffect sInstance;

	@Override
	public void visit(ImageView iv, Bitmap bitmap) {
		iv.setImageBitmap(bitmap);
	}

	@Override
	public void visit(TextView tv, int place, Bitmap bitmap) {
		Drawable[] drawables = tv.getCompoundDrawables();
		
		Drawable left = ((place & TextViewBinder.PLACE_LEFT) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : drawables[0]);
		Drawable top = ((place & TextViewBinder.PLACE_TOP) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : drawables[1]);
		Drawable right = ((place & TextViewBinder.PLACE_RIGHT) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : drawables[2]);
		Drawable bottom = ((place & TextViewBinder.PLACE_BOTTOM) != 0 ? new BitmapDrawable(tv.getResources(), bitmap) : drawables[3]);

		tv.setCompoundDrawables(left, top, right, bottom);
	}

	public static synchronized NoEffect getInstance() {
		if (sInstance == null) {
			sInstance = new NoEffect();
		}
		return sInstance;
	}
}
