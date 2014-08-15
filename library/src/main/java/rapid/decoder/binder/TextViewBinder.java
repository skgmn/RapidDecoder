package rapid.decoder.binder;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import rapid.decoder.cache.ResourcePool;

public class TextViewBinder extends ViewBinder<TextView> {
    private static final ResourcePool<TextViewBinder> POOL = new Pool<TextViewBinder>() {
        @Override
        protected TextViewBinder newInstance() {
            return new TextViewBinder();
        }
    };

    private static final int[] sGravityMask = new int[]{
            Gravity.LEFT,
            Gravity.TOP,
            Gravity.RIGHT,
            Gravity.BOTTOM,
            Gravity.START,
            Gravity.END
    };

    private int mGravity;
    private int mWidth;
    private int mHeight;

    public static TextViewBinder obtain(TextView v, int gravity, int width, int height) {
        TextViewBinder binder = POOL.obtainNotReset();
        binder.init(v, gravity, width, height);
        return binder;
    }

    protected void init(TextView v, int gravity, int width, int height) {
        init(v);
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
    public int getDrawableCount() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 6 : 4;
    }

    @Override
    public void recycle() {
        POOL.recycle(this);
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
                case Gravity.START:
                    return v.getCompoundDrawablesRelative()[0];
                case Gravity.END:
                    return v.getCompoundDrawablesRelative()[2];
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    @SuppressLint("NewApi")
    private void setCompoundDrawable(int gravity, Drawable d) {
        TextView v = getView();
        if (v != null) {
            d.setBounds(0, 0, mWidth, mHeight);
            Drawable[] drawables;
            switch (gravity) {
                case Gravity.LEFT:
                    drawables = v.getCompoundDrawables();
                    v.setCompoundDrawables(d, drawables[1], drawables[2], drawables[3]);
                    break;

                case Gravity.TOP:
                    drawables = v.getCompoundDrawables();
                    v.setCompoundDrawables(drawables[0], d, drawables[2], drawables[3]);
                    break;

                case Gravity.RIGHT:
                    drawables = v.getCompoundDrawables();
                    v.setCompoundDrawables(drawables[0], drawables[1], d, drawables[3]);
                    break;

                case Gravity.BOTTOM:
                    drawables = v.getCompoundDrawables();
                    v.setCompoundDrawables(drawables[0], drawables[1], drawables[2], d);
                    break;

                case Gravity.START:
                    drawables = v.getCompoundDrawablesRelative();
                    v.setCompoundDrawablesRelative(d, drawables[1], drawables[2], drawables[3]);
                    break;

                case Gravity.END:
                    drawables = v.getCompoundDrawablesRelative();
                    v.setCompoundDrawablesRelative(drawables[0], drawables[1], d, drawables[3]);
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

    @Override
    protected void onPlaceholderInflated(Drawable placeholder) {
        placeholder.setBounds(0, 0, mWidth, mHeight);
    }
}
