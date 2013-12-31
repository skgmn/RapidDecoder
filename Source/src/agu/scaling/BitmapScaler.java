package agu.scaling;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapScaler {
	public static Bitmap scaleWidth(Bitmap bitmap, int width) {
		return scaleWidth(bitmap, width, true);
	}
	
	public static Bitmap scaleWidth(Bitmap bitmap, int width, boolean filter) {
		final double ratio = (double) bitmap.getHeight() / bitmap.getWidth();
		final int newHeight = (int) (ratio * width);
		
		return Bitmap.createScaledBitmap(bitmap, width, newHeight, filter);
	}

	public static Bitmap scaleHeight(Bitmap bitmap, int height) {
		return scaleHeight(bitmap, height, true);
	}
	
	public static Bitmap scaleHeight(Bitmap bitmap, int height, boolean filter) {
		final double ratio = (double) bitmap.getWidth() / bitmap.getHeight();
		final int newWidth = (int) (ratio * height);
		
		return Bitmap.createScaledBitmap(bitmap, newWidth, height, filter);
	}

	public static Bitmap scaleByRatio(Bitmap bitmap, float ratio) {
		return scaleByRatio(bitmap, ratio, ratio, true);
	}

	public static Bitmap scaleByRatio(Bitmap bitmap, float widthRatio, float heightRatio) {
		return scaleByRatio(bitmap, widthRatio, heightRatio, true);
	}
	
	public static Bitmap scaleByRatio(Bitmap bitmap, float widthRatio, float heightRatio, boolean filter) {
		final Matrix m = new Matrix();
		m.setScale(widthRatio, heightRatio);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, filter);
	}
}
