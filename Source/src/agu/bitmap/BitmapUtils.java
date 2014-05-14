package agu.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import static agu.caching.ResourcePool.*;

public final class BitmapUtils {
	public static Bitmap getBitmap(Drawable d) {
		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		} else {
			final boolean opaque = (d.getOpacity() == PixelFormat.OPAQUE);
			
			final Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
					opaque ? Config.RGB_565 : Config.ARGB_8888);
			d.setDither(opaque);
			d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
			
			Canvas cv = CANVAS.obtain(bitmap);
			d.draw(cv);
			CANVAS.recycle(cv);
			
			return bitmap;
		}
	}
	
	public static int getByteCount(Bitmap bitmap) {
		if (Build.VERSION.SDK_INT >= 19) {
			return bitmap.getAllocationByteCount();
		} else if (Build.VERSION.SDK_INT >= 12) {
			return bitmap.getByteCount();
		} else {
			return getByteCount(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
		}
	}
	
	public static int getByteCount(int width, int height, Config config) {
		final int bytesPerPixel = (config.equals(Config.ARGB_8888) ? 4 : 2);
		return width * height * bytesPerPixel;
	}
}
