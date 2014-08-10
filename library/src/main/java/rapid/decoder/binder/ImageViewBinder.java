package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class ImageViewBinder extends ViewBinder<ImageView> {
    public ImageViewBinder(ImageView v) {
        super(v);
    }

    @Override
    public void bind(Bitmap bitmap, boolean isAsync) {
        final ImageView v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        effect().apply(v.getContext(), this, d, isAsync);
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
