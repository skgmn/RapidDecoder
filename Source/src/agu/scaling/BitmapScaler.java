package agu.scaling;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.ExternalBitmapDecoder;
import android.graphics.Bitmap;
import android.widget.ImageView.ScaleType;

public final class BitmapScaler {
	public static Bitmap scale(BitmapDecoder decoder, int width, int height, 
			ScaleType scaleType) {
		
		if (scaleType.equals(ScaleType.MATRIX)) {
			final int w = decoder.width();
			final int h = decoder.height();
			
			Bitmap bitmap;
			if (w > width || h > height) {
				
			} else {
				
			}
		}
		
		return null;
	}
}
