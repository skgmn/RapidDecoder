package rapid.decoder.cache;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rapid.decoder.BitmapUtils;

public class BitmapLruCache<K> extends LruCache<K, Bitmap> {
    private HashMap<K, WeakReference<Bitmap>> mRemovedBitmaps =
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
    protected void entryRemoved(boolean evicted, K key, Bitmap oldValue,
                                Bitmap newValue) {

        mRemovedBitmaps.put(key, new WeakReference<Bitmap>(oldValue));
    }

    @Override
    public Bitmap get(K key) {
        Bitmap bitmap = super.get(key);
        if (bitmap != null) return bitmap;

        WeakReference<Bitmap> ref = mRemovedBitmaps.get(key);
        if (ref == null) {
            return null;
        }

        bitmap = ref.get();
        if (bitmap == null) {
            mRemovedBitmaps.remove(key);
            return null;
        } else {
            return bitmap;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void compact() {
        Iterator<Map.Entry<K, WeakReference<Bitmap>>> it =
                mRemovedBitmaps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, WeakReference<Bitmap>> entry = it.next();
            if (entry.getValue().get() == null) {
                it.remove();
            }
        }
    }
}
