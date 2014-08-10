package rapid.decoder.cache;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rapid.decoder.BitmapUtils;

public class BitmapLruCache<K> extends LruCache<K, Bitmap> {
    private HashMap<K, WeakReference<Bitmap>> mEvictedBitmap =
            new HashMap<K, WeakReference<Bitmap>>();

    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    @SuppressLint("NewApi")
    @Override
    protected int sizeOf(K key, Bitmap value) {
        return BitmapUtils.getByteCount(value);
    }

    @Override
    protected void entryRemoved(boolean evicted, K key, Bitmap oldValue, Bitmap newValue) {
        if (!oldValue.isRecycled()) {
            mEvictedBitmap.put(key, new WeakReference<Bitmap>(oldValue));
        }
    }

    @Override
    public Bitmap get(K key) {
        Bitmap bitmap = super.get(key);
        if (bitmap != null) {
            if (bitmap.isRecycled()) {
                remove(key);
            } else {
                return bitmap;
            }
        }

        WeakReference<Bitmap> ref = mEvictedBitmap.get(key);
        if (ref == null) {
            return null;
        }

        bitmap = ref.get();
        if (bitmap == null || bitmap.isRecycled()) {
            mEvictedBitmap.remove(key);
            return null;
        } else {
            return bitmap;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void compact() {
        Iterator<Map.Entry<K, WeakReference<Bitmap>>> it =
                mEvictedBitmap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, WeakReference<Bitmap>> entry = it.next();
            if (entry.getValue().get() == null) {
                it.remove();
            }
        }
    }
}
