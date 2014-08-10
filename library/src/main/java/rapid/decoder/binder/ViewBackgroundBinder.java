package rapid.decoder.binder;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import rapid.decoder.cache.ResourcePool;

public class ViewBackgroundBinder extends ViewBinder<View> {
    private static final ResourcePool<ViewBackgroundBinder> POOL = new Pool<ViewBackgroundBinder>
            () {
        @Override
        protected ViewBackgroundBinder newInstance() {
            return new ViewBackgroundBinder();
        }
    };

    public static ViewBackgroundBinder obtain(View v) {
        ViewBackgroundBinder binder = POOL.obtainNotReset();
        binder.init(v);
        return binder;
    }

    @Override
    public void recycle() {
        POOL.recycle(this);
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
