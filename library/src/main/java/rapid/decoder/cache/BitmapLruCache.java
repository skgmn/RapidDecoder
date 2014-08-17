package rapid.decoder.cache;

import android.graphics.Bitmap;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import rapid.decoder.BitmapLoader;
import rapid.decoder.BitmapMeta;
import rapid.decoder.BitmapUtils;

public class BitmapLruCache extends LruCache<BitmapLoader, Bitmap> {
    private static class CachedMeta implements BitmapMeta {
        public int width;
        public int height;
        public WeakHashMap<Bitmap, Object> bitmaps = new WeakHashMap<Bitmap, Object>();

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }
    }

    private HashMap<BitmapLoader, WeakReference<Bitmap>> mEvictedBitmap =
            new HashMap<BitmapLoader, WeakReference<Bitmap>>();
    private HashMap<Object, CachedMeta> mMetaCache = new HashMap<Object, CachedMeta>();

    public BitmapLruCache(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(BitmapLoader key, Bitmap value) {
        return BitmapUtils.getByteCount(value);
    }

    @Override
    protected void entryRemoved(boolean evicted, BitmapLoader key, Bitmap oldValue,
                                Bitmap newValue) {
        if (!oldValue.isRecycled()) {
            mEvictedBitmap.put(key, new WeakReference<Bitmap>(oldValue));
        }
    }

    @Override
    public Bitmap put(BitmapLoader key, Bitmap value) {
        Bitmap bitmap = super.put(key, value);
        Object id = key.a();
        if (id != null) {
            CachedMeta info = mMetaCache.get(id);
            if (info == null) {
                info = new CachedMeta();
                mMetaCache.put(id, info);
            }
            info.width = key.sourceWidth();
            info.height = key.sourceHeight();
            info.bitmaps.put(value, null);
        }
        return bitmap;
    }

    @Override
    public Bitmap get(BitmapLoader key) {
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
            if (Math.random() <= 0.2) {
                gcEvictedBitmaps();
            }
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

    public BitmapMeta getMeta(Object id) {
        CachedMeta meta = mMetaCache.get(id);
        if (meta == null) {
            if (Math.random() <= 0.2) {
                gcMetaCache();
            }
            return null;
        }

        if (meta.bitmaps.isEmpty()) {
            mMetaCache.remove(id);
            return null;
        }

        return meta;
    }

    private void gcEvictedBitmaps() {
        Iterator<Map.Entry<BitmapLoader, WeakReference<Bitmap>>> it = mEvictedBitmap.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<BitmapLoader, WeakReference<Bitmap>> entry = it.next();
            Bitmap bitmap = entry.getValue().get();
            if (bitmap == null || bitmap.isRecycled()) {
                it.remove();
            }
        }
    }

    private void gcMetaCache() {
        Iterator<Map.Entry<Object, CachedMeta>> it = mMetaCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, CachedMeta> entry = it.next();
            if (entry.getValue().bitmaps.isEmpty()) {
                it.remove();
            }
        }
    }
}
