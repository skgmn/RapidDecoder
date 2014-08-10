package rapid.decoder.cache;

import rapid.decoder.BitmapMeta;

public class BitmapMetaLruCache extends LruCache<Object, BitmapMeta> {
    public BitmapMetaLruCache(int maxCount) {
        super(maxCount);
    }

    @Override
    protected int sizeOf(Object key, BitmapMeta value) {
        return 1;
    }
}
