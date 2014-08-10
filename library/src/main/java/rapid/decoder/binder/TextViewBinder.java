package rapid.decoder.binder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public class TextViewBinder extends ViewBinder<TextView> {
    private static final int[] sGravityMask = new int[]{
            Gravity.LEFT,
            Gravity.TOP,
            Gravity.RIGHT,
            Gravity.BOTTOM
    };

    private int mGravity;
    private int mWidth;
    private int mHeight;

    public TextViewBinder(TextView v, int gravity, int width, int height) {
        super(v);
        mGravity = gravity;
        mWidth = width;
        mHeight = height;
    }

    public void runAfterReady(final OnReadyListener listener) {
        View v = getView();
        if (v == null) return;
        listener.onReady(v, false);
    }

    @Override
    public void bind(Bitmap bitmap, boolean isAsync) {
        final TextView v = getView();
        if (v == null) return;

        Drawable d = createDrawable(v.getContext(), bitmap);
        if (d == null) return;

        effect().apply(v.getContext(), this, d, isAsync);
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
            d.setBounds(0, 0, mWidth, mHeight);
            Drawable[] drawables = v.getCompoundDrawables();
            switch (gravity) {
                case Gravity.LEFT:
                    v.setCompoundDrawables(d, drawables[1], drawables[2], drawables[3]);
                    break;

                case Gravity.TOP:
                    v.setCompoundDrawables(drawables[0], d, drawables[2], drawables[3]);
                    break;

                case Gravity.RIGHT:
                    v.setCompoundDrawables(drawables[0], drawables[1], d, drawables[3]);
                    break;

                case Gravity.BOTTOM:
                    v.setCompoundDrawables(drawables[0], drawables[1], drawables[2], d);
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

    @Override
    public int getLayoutWidth() {
        return mWidth;
    }

    @Override
    public int getLayoutHeight() {
        return mHeight;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }
}
