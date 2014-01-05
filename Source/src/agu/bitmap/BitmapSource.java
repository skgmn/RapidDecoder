package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public interface BitmapSource {
	static final String MESSAGE_INVALID_RATIO = "Ratio should be positive.";
	
	int sourceWidth();
	int sourceHeight();
	Bitmap bitmap();
	BitmapSource scale(int width, int height, boolean scaleFilter);
	BitmapSource scaleBy(double widthRatio, double heightRatio, boolean scaleFilter);
	BitmapSource region(int left, int top, int right, int bottom);
	void draw(Canvas cv, Rect rectDest);
}
