package rapid.decoder.binder;

import android.graphics.Bitmap;

import rapid.decoder.cache.CacheSource;

public abstract class BitmapBinder {
    protected Effect mEffect;

    protected BitmapBinder(Effect effect) {
        mEffect = effect;
    }

    public abstract void bind(Bitmap bitmap, CacheSource cacheSource);

    public abstract Object key();

    public abstract boolean isKeyStrong();
}
