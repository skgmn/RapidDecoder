package rapid.decoder.binder;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import rapid.decoder.cache.ResourcePool;

public class ImageViewBinder extends ViewBinder<ImageView> {
    private static final ResourcePool<ImageViewBinder> POOL = new Pool<ImageViewBinder>() {
        @Override
        protected ImageViewBinder newInstance() {
            return new ImageViewBinder();
        }
    };

    public static ImageViewBinder obtain(ImageView v) {
        ImageViewBinder binder = POOL.obtainNotReset();
        binder.init(v);
        return binder;
    }

    @Override
    public void recycle() {
        POOL.recycle(this);
    }

    @Override
    public int getDrawableCount() {
        return 1;
    }

    @Override
    public boolean isDrawableEnabled(int index) {
        return true;
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
