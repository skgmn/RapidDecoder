package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import rapid.decoder.binder.BitmapBinder;
import rapid.decoder.binder.ImageViewBinder;
import rapid.decoder.binder.ViewBackgroundBinder;
import rapid.decoder.cache.CacheSource;

public abstract class Decodable {
    public interface OnBitmapDecodedListener {
        void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource);
    }

    public static class DecodeResult {
        public Bitmap bitmap;
        public CacheSource cacheSource;
    }

    public void into(final BitmapBinder binder) {
        Object key = binder.key();
        if (key == null) return;

        BitmapLoadTask task = new BitmapLoadTask(this, new OnBitmapDecodedListener() {
            @Override
            public void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource) {
                binder.bind(bitmap, cacheSource);
            }
        });
        task.setKey(key, binder.isKeyStrong());
        BitmapDecoder.execute(task);
    }

    public void into(View v) {
        if (v instanceof ImageView) {
            into(new ImageViewBinder((ImageView) v));
        } else {
            into(new ViewBackgroundBinder(v));
        }
    }

    public void decode(@NonNull OnBitmapDecodedListener listener) {
        BitmapLoadTask task = new BitmapLoadTask(this, listener);
        task.setKey(this, false);
        BitmapDecoder.execute(task);
    }

    public abstract void cancel();

    @Nullable
    public abstract Bitmap decode();

    public abstract void decode(@NonNull DecodeResult out);

    @SuppressWarnings("UnusedDeclaration")
    public abstract void draw(Canvas cv, Rect bounds);

    public abstract Decodable mutate();


    public abstract boolean isCancelled();
}
