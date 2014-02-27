package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;

public interface BitmapSource {
	static final String MESSAGE_INVALID_RATIO = "Ratio should be positive.";

	/**
	 * @return The width of the source image.
	 */
	int sourceWidth();
	
	/**
	 * @return The height of the source image.
	 */
	int sourceHeight();
	
	Bitmap bitmap();
	
	/**
	 * <p>Request the decoder to scale the image to the specific dimension while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param width A desired width to be scaled to.
	 * @param height A desired height to be scaled to.
	 * @param scaleFilter true if the image should be filtered.
	 */
	BitmapSource scale(int width, int height, boolean scaleFilter);
	
	/**
	 * <p>Request the decoder to scale the image by the specific ratio while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param widthRatio Scale ratio of width. 
	 * @param heightRatio Scale ratio of height.
	 * @param scaleFilter true if the image should be filtered.
	 */
	BitmapSource scaleBy(double widthRatio, double heightRatio, boolean scaleFilter);
	
	/**
	 * <p>Request the decoder to crop the image while decoding.
	 * Decoded image will be the same as an image which is cropped after decoding.</p>
	 * <p>It uses {@link BitmapRegionDecoder} on API level 10 or higher, otherwise it uses built-in decoder.</p>
	 */
	BitmapSource region(int left, int top, int right, int bottom);

	/**
	 * Directly draw the image to canvas without any unnecessary scaling.
	 */
	void draw(Canvas cv, Rect rectDest);
}
