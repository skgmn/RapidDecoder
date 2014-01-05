package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BitmapUtils {
	public static Bitmap getBitmap(Drawable d) {
		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		} else {
			final boolean opaque = (d.getOpacity() == PixelFormat.OPAQUE);
			
			final Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
					opaque ? Config.RGB_565 : Config.ARGB_8888);
			d.setDither(opaque);
			d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
			d.draw(new Canvas(bitmap));
			
			return bitmap;
		}
	}
}
