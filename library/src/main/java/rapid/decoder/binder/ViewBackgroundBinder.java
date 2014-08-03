package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import rapid.decoder.cache.CacheSource;

public class ViewBackgroundBinder extends ViewBitmapBinder<View> implements Effect.EffectTarget {
    public ViewBackgroundBinder(View v) {
        super(v);
    }

    public ViewBackgroundBinder(View v, Effect effect) {
        super(v, effect);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void bind(Bitmap bitmap, CacheSource cacheSource) {
        View v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackground(d);
        } else {
            v.setBackgroundDrawable(d);
        }
    }

    @Override
    public Drawable getDrawable(int index) {
        View v = getView();
        return v != null ? v.getBackground() : null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setDrawable(int index, Drawable d) {
        View v = getView();
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                v.setBackground(d);
            } else {
                v.setBackgroundDrawable(d);
            }
        }
    }

    @Override
    public int getDrawableCount() {
        return 1;
    }
}
