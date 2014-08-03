package rapid.decoder.binder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.lang.ref.WeakReference;

public abstract class ViewBitmapBinder<T extends View> extends BitmapBinder implements Effect
        .EffectTarget {
    private WeakReference<T> mView;

    public ViewBitmapBinder(T v) {
        this(v, Effect.FADE_IN_IF_NOT_CACHED);
        mView = new WeakReference<T>(v);
    }

    public ViewBitmapBinder(T v, Effect effect) {
        super(effect);
        mView = new WeakReference<T>(v);
    }

    protected T getView() {
        return mView.get();
    }

    public Drawable createDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @Override
    public Object key() {
        return getView();
    }

    @Override
    public boolean isKeyStrong() {
        return false;
    }

    @Override
    public void postDelayed(Runnable r, int delay) {
        View v = getView();
        if (v != null) {
            v.postDelayed(r, delay);
        }
    }
}
