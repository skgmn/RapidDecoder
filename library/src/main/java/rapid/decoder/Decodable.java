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

public abstract class Decodable {
    public interface OnBitmapDecodedListener {
        void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource);
    }

    public void into(final ViewBinder binder) {
        View v = binder.getView();
        if (v == null) return;

        final BackgroundTaskRecord record = BitmapDecoder.sTaskManager.register(v, false);
        binder.runAfterReady(new ViewBinder.OnReadyListener() {
            @Override
            public void onReady(View v, boolean async) {
                if (record.isStale) return;
                loadBitmapWhenReady(record, binder, v, async);
            }
        });
    }

    void loadBitmapWhenReady(BackgroundTaskRecord record, final ViewBinder binder,
                             View v, boolean async) {

        ViewFrameBuilder frameBuilder = null;
        FramingMethod framing = binder.framing();
        if (framing != null) {
            frameBuilder = setupFrameBuilder(v, framing);
            if (frameBuilder != null) {
                frameBuilder.prepareFraming();
                if (!async) {
                    FramedDecoder framedDecoder = frameBuilder.getFramedDecoder(true);
                    if (framedDecoder != null) {
                        Bitmap bitmap = framedDecoder.getCachedBitmap();
                        if (bitmap != null) {
                            binder.bind(bitmap, false);
                            return;
                        }
                    }
                }
            }
        }

        LoadIntoViewTask task = new LoadIntoViewTask(this, new OnBitmapDecodedListener() {
            @Override
            public void onBitmapDecoded(@Nullable Bitmap bitmap, @NonNull CacheSource cacheSource) {
                binder.bind(bitmap, true);
            }
        });
        task.setKey(v, false);
        task.setFrameBuilder(frameBuilder);
        record.execute(task);
    }

    protected ViewFrameBuilder setupFrameBuilder(View v, FramingMethod framing) {
        return null;
    }

    public void into(View v) {
        if (v instanceof ImageView) {
            into(new ImageViewBinder((ImageView) v));
        } else {
            into(new ViewBackgroundBinder(v));
        }
    }

    public void decode(@NonNull OnBitmapDecodedListener listener) {
        LoadIntoViewTask task = new LoadIntoViewTask(this, listener);
        task.setKey(this, false);
        BitmapDecoder.sTaskManager.execute(task);
    }

    public boolean isMemoryCacheSupported() {
        return false;
    }

    public Bitmap getCachedBitmap() {
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
