package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import rapid.decoder.cache.CacheSource;

public class ImageViewBinder extends ViewBitmapBinder<ImageView> implements Effect.EffectTarget {
    public ImageViewBinder(ImageView v) {
        super(v);
    }

    public ImageViewBinder(ImageView v, Effect effect) {
        super(v, effect);
    }

    @Override
    public void bind(Bitmap bitmap, CacheSource cacheSource) {
        final ImageView v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        mEffect.apply(v.getContext(), this, d, cacheSource);
    }

    @Override
    public int getDrawableCount() {
        return 1;
    }

    @Override
    public Drawable getDrawable(int index) {
        ImageView v = getView();
        return v != null ? v.getDrawable() : null;
    }

    @Override
    public void setDrawable(int index, Drawable d) {
        ImageView v = getView();
        if (v != null) {
            v.setImageDrawable(d);
        }
    }

    @Override
    public void postDelayed(Runnable r, int delay) {
        ImageView v = getView();
        if (v != null) {
            v.postDelayed(r, delay);
        }
    }
}
