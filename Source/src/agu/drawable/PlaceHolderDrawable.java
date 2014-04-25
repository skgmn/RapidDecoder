package agu.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class PlaceHolderDrawable extends Drawable {
	public PlaceHolderDrawable() {
	}
	
	public PlaceHolderDrawable(int width, int height) {
		setBounds(0, 0, width, height);
	}
	
	@Override
	public void draw(Canvas arg0) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSPARENT;
	}

	@Override
	public void setAlpha(int arg0) {
	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
	}
	
	@Override
	public int getIntrinsicWidth() {
		return getBounds().width();
	}
	
	@Override
	public int getIntrinsicHeight() {
		return getBounds().height();
	}
}
