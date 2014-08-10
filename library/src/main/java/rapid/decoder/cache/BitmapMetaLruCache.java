package rapid.decoder.cache;

public class BitmapMetaLruCache extends LruCache<Object, BitmapMetaInfo> {
    public BitmapMetaLruCache(int maxCount) {
        super(maxCount);
    }

    @Override
    protected int sizeOf(Object key, BitmapMetaInfo value) {
        return 1;
    }
}
