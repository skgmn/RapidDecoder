package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.TextView;

import rapid.decoder.cache.CacheSource;

public class TextViewBinder extends ViewBinder<TextView> {
    private static final int[] sGravityMask = new int[] {
            Gravity.LEFT,
            Gravity.TOP,
            Gravity.RIGHT,
            Gravity.BOTTOM
    };

    private int mGravity;

    public TextViewBinder(TextView v, int gravity) {
        super(v);
        mGravity = gravity;
    }

    @Override
    public void bind(Bitmap bitmap, CacheSource cacheSource) {
        final TextView v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        effect().apply(v.getContext(), this, d, cacheSource);
    }

    @Override
    public int getDrawableCount() {
        return 4;
    }

    @Override
    public boolean isDrawableEnabled(int index) {
        int mask = sGravityMask[index];
        return (mGravity & mask) == mask;
    }

    private Drawable getCompoundDrawable(int gravity) {
        TextView v = getView();
        if (v != null) {
            switch (gravity) {
                case Gravity.LEFT:
                    return v.getCompoundDrawables()[0];
                case Gravity.TOP:
                    return v.getCompoundDrawables()[1];
                case Gravity.RIGHT:
                    return v.getCompoundDrawables()[2];
                case Gravity.BOTTOM:
                    return v.getCompoundDrawables()[3];
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private void setCompoundDrawable(int gravity, Drawable d) {
        TextView v = getView();
        if (v != null) {
            Drawable[] drawables = v.getCompoundDrawables();
            switch (gravity) {
                case Gravity.LEFT:
                    v.setCompoundDrawablesWithIntrinsicBounds(d, drawables[1], drawables[2],
                            drawables[3]);
                    break;

                case Gravity.TOP:
                    v.setCompoundDrawablesWithIntrinsicBounds(drawables[0], d, drawables[2],
                            drawables[3]);
                    break;

                case Gravity.RIGHT:
                    v.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], d,
                            drawables[3]);
                    break;

                case Gravity.BOTTOM:
                    v.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1],
                            drawables[2], d);
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    public Drawable getDrawable(int index) {
        return getCompoundDrawable(sGravityMask[index]);
    }

    @Override
    public void setDrawable(int index, Drawable d) {
        setCompoundDrawable(sGravityMask[index], d);
    }

    @Override
    public void postDelayed(Runnable r, int delay) {
        TextView v = getView();
        if (v != null) {
            v.postDelayed(r, delay);
        }
    }
}
