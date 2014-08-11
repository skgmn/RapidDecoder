package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import rapid.decoder.binder.ImageViewBinder;
import rapid.decoder.binder.ViewBackgroundBinder;
import rapid.decoder.binder.ViewBinder;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.frame.FramedDecoder;
import rapid.decoder.frame.FramingMethod;

public abstract class Decodable implements BitmapMeta {
    public interface OnBitmapDecodedListener {
        void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource);
        void onCancel();
    }

    private boolean mIsMemoryCacheEnabled = true;

    public void into(final ViewBinder binder) {
        View v = binder.getView();
        if (v == null) return;

        final BackgroundBitmapLoadTask task = BitmapDecoder.sTaskManager.register(v, false);
        binder.runAfterReady(new ViewBinder.OnReadyListener() {
            @Override
            public void onReady(View v, boolean async) {
                if (task.isCancelled()) return;
                loadBitmapWhenReady(task, binder, v, async);
            }
        });
        if (!task.isCancelled()) {
            binder.displayPlaceholder();
        }
    }

    void loadBitmapWhenReady(BackgroundBitmapLoadTask task, final ViewBinder binder,
                             View v, boolean async) {

        ViewFrameBuilder frameBuilder = null;
        FramingMethod framing = binder.framing();
        if (framing != null) {
            frameBuilder = setupFrameBuilder(binder, framing);
            if (frameBuilder != null) {
                frameBuilder.prepareFraming();
                if (!async) {
                    FramedDecoder framedDecoder = frameBuilder.getFramedDecoder(true);
                    if (framedDecoder != null) {
                        Bitmap bitmap = framedDecoder.getCachedBitmap();
                        if (bitmap != null) {
                            task.cancel();
                            binder.bind(bitmap, false);
                            return;
                        }
                    }
                }
            }
        }

        task.setDecodable(this);
        task.setOnBitmapDecodedListener(new OnBitmapDecodedListener() {
            @Override
            public void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource) {
                binder.bind(bitmap, true);
            }

            @Override
            public void onCancel() {
                binder.recycle();
            }
        });
        task.setKey(v, false);
        task.setFrameBuilder(frameBuilder);
        task.start();
    }

    protected ViewFrameBuilder setupFrameBuilder(ViewBinder<?> binder, FramingMethod framing) {
        return null;
    }

    public void into(View v) {
        if (v instanceof ImageView) {
            into(ImageViewBinder.obtain((ImageView) v));
        } else {
            into(ViewBackgroundBinder.obtain(v));
        }
    }

    public void decode(@NonNull OnBitmapDecodedListener listener) {
        BackgroundBitmapLoadTask task = new BackgroundBitmapLoadTask();
        task.setDecodable(this);
        task.setOnBitmapDecodedListener(listener);
        task.setKey(this, false);
        BitmapDecoder.sTaskManager.execute(task);
    }

    public boolean isMemoryCacheEnabled() {
        return mIsMemoryCacheEnabled;
    }

    public Decodable useMemoryCache(boolean useCache) {
        this.mIsMemoryCacheEnabled = useCache;
        return this;
    }

    public Bitmap getCachedBitmap() {
        return null;
    }

    public BitmapMeta getCachedMeta() {
        return null;
    }

    public abstract CacheSource cacheSource();

    public abstract void cancel();

    @Nullable
    public abstract Bitmap decode();

    @SuppressWarnings("UnusedDeclaration")
    public abstract void draw(Canvas cv, Rect bounds);

    public abstract Decodable mutate();

    public abstract boolean isCancelled();

    public abstract int width();

    public abstract int height();
}
