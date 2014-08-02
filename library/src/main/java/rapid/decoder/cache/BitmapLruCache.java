package rapid.decoder.cache;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import rapid.decoder.BitmapUtils;

public class BitmapLruCache<K> extends LruCache<K, Bitmap> {
	private boolean mAutoRecycle;
	
	public BitmapLruCache(int maxSize) {
		this(maxSize, false);
	}

	public BitmapLruCache(int maxSize, boolean autoRecycle) {
		super(maxSize);
		mAutoRecycle = autoRecycle;
	}
	
	@SuppressLint("NewApi")
	@Override
	protected int sizeOf(K key, Bitmap value) {
		return BitmapUtils.getByteCount(value);
	}
	
	@Override
	protected void entryRemoved(boolean evicted, K key, Bitmap oldValue,
			Bitmap newValue) {

		if (mAutoRecycle) {
			oldValue.recycle();
		}
	}
}
