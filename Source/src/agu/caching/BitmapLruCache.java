package agu.caching;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build;

public class BitmapLruCache<K> extends LruCache<K, Bitmap> {
	public BitmapLruCache(int maxSize) {
		super(maxSize);
	}
	
	@SuppressLint("NewApi")
	@Override
	protected int sizeOf(K key, Bitmap value) {
		if (Build.VERSION.SDK_INT >= 19) {
			return value.getAllocationByteCount();
		} else if (Build.VERSION.SDK_INT >= 12) {
			return value.getByteCount();
		} else {
			final Config config = value.getConfig();
			final int bytesPerPixel = (config.equals(Config.ARGB_8888) ? 4 : 2);
			
			return value.getWidth() * value.getHeight() * bytesPerPixel;
		}
	}
	
	@Override
	protected void entryRemoved(boolean evicted, K key, Bitmap oldValue,
			Bitmap newValue) {
		
		oldValue.recycle();
	}
}
