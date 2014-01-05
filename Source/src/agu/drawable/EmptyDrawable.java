package agu.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class EmptyDrawable extends Drawable {
	private int width = 0;
	private int height = 0;

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
		return width;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return height;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
}
