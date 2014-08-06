package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import rapid.decoder.cache.CacheSource;

public class ViewBackgroundBinder extends ViewBinder<View> {
    public ViewBackgroundBinder(View v) {
        super(v);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void bind(Bitmap bitmap, CacheSource cacheSource) {
        View v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        effect().apply(v.getContext(), this, d, cacheSource);
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

    @Override
    public boolean isDrawableEnabled(int index) {
        return true;
    }
}
