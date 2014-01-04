package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public interface Decoder {
	int sourceWidth();
	int sourceHeight();
	Bitmap decode();
	Decoder scale(int width, int height, boolean scaleFilter);
	Decoder scaleBy(double widthRatio, double heightRatio, boolean scaleFilter);
	Decoder region(int left, int top, int right, int bottom);
	void draw(Canvas cv, Rect rectDest);
}
