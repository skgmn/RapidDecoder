package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import rapid.decoder.frame.ScaleTypeFraming;

public abstract class Decodable implements BitmapMeta {
    public interface OnBitmapDecodedListener {
        void onBitmapDecoded(@Nullable Bitmap bitmap, @Nullable CacheSource cacheSource);

        void onCancel();
    }

    private boolean mIsMemoryCacheEnabled = true;

    public void into(final ViewBinder binder) {
        into(binder, null);
    }

    public void into(final ViewBinder binder, final OnBitmapDecodedListener listener) {
        View v = binder.getView();
        if (v == null) return;

        final BackgroundTask task = BackgroundTaskManager.register(v);
        binder.runAfterReady(new ViewBinder.OnReadyListener() {
            @Override
            public void onReady(View v, boolean async) {
                if (task.isCancelled()) return;
                loadBitmapWhenReady(task, binder, async, listener);
            }
        });
        if (!task.isCancelled()) {
            binder.showPlaceholder();
        }
    }

    void loadBitmapWhenReady(BackgroundTask task, final ViewBinder binder, boolean async,
                             final OnBitmapDecodedListener listener) {
        ViewFrameBuilder frameBuilder;
        FramingMethod framing = binder.framing();
        if (framing == null) {
            framing = new ScaleTypeFraming(ImageView.ScaleType.CENTER_CROP);
        }
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
                        if (listener != null) {
                            listener.onBitmapDecoded(bitmap, CacheSource.MEMORY);
                        }
                        return;
                    }
                }
            }
        }

        task.setDecodable(this);
        task.setOnBitmapDecodedListener(new OnBitmapDecodedListener() {
            @Override
            public void onBitmapDecoded(@Nullable Bitmap bitmap,
                                        @Nullable CacheSource cacheSource) {
                if (bitmap == null) {
                    binder.showErrorImage();
                    if (listener != null) {
                        listener.onBitmapDecoded(null, null);
                    }
                } else {
                    binder.bind(bitmap, true);
                    if (listener != null) {
                        listener.onBitmapDecoded(bitmap, cacheSource);
                    }
                }
            }

            @Override
            public void onCancel() {
                binder.recycle();
                if (listener != null) {
                    listener.onCancel();
                }
            }
        });
        task.setFrameBuilder(frameBuilder);
        task.start();
    }

    protected ViewFrameBuilder setupFrameBuilder(ViewBinder<?> binder, FramingMethod framing) {
        return null;
    }

    public void into(View v) {
        into(v, null);
    }

    public void into(View v, OnBitmapDecodedListener listener) {
        if (v instanceof ImageView) {
            into(ImageViewBinder.obtain((ImageView) v), listener);
        } else {
            into(ViewBackgroundBinder.obtain(v), listener);
        }
    }

    public void decode(@NonNull OnBitmapDecodedListener listener) {
        decode(this, listener);
    }

    public void decode(@NonNull Object key, @NonNull OnBitmapDecodedListener listener) {
        BackgroundTask task = BackgroundTaskManager.register(key);
        task.setDecodable(this);
        task.setOnBitmapDecodedListener(listener);
        task.start();
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
    protected abstract Bitmap decode(boolean approximately);

    @Nullable
    public final Bitmap decode() {
        return decode(false);
    }

    @Nullable
    public final Bitmap decodeApproximately() {
        return decode(true);
    }

    /**
     * Directly draw the image to canvas without any unnecessary scaling.
     */
    public abstract void draw(Canvas cv, int left, int top);

    public abstract Decodable fork();

    public abstract boolean isCancelled();

    public abstract int width();

    public abstract int height();
}
