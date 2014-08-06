package rapid.decoder.binder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import rapid.decoder.NextLayoutInspector;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.frame.FramingAlgorithm;
import rapid.decoder.frame.ScaleTypeFraming;

public abstract class ViewBinder<T extends View> implements Effect.EffectTarget {
    public interface OnReadyListener {
        void onReady(View v);
    }

    private Effect mEffect;
    private FramingAlgorithm mFraming;
    private WeakReference<T> mView;

    public ViewBinder(T v) {
        mView = new WeakReference<T>(v);
    }

    @Nullable
    public T getView() {
        return mView.get();
    }

    public Drawable createDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public void postDelayed(Runnable r, int delay) {
        View v = getView();
        if (v != null) {
            v.postDelayed(r, delay);
        }
    }

    public ViewBinder<T> scaleType(final ImageView.ScaleType scaleType) {
        return framing(new ScaleTypeFraming(scaleType));
    }

    public void runAfterReady(final OnReadyListener listener) {
        View v = getView();
        if (v == null) return;

        if (v.isLayoutRequested() && !shouldWrapContent(v)) {
            NextLayoutInspector.inspectNextLayout(v, new NextLayoutInspector.OnNextLayoutListener
                    () {
                @Override
                public void onNextLayout(View v) {
                    listener.onReady(v);
                }
            });
        } else {
            listener.onReady(v);
        }
    }

    private static boolean shouldWrapContent(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        return lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                lp.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public abstract void bind(Bitmap bitmap, CacheSource cacheSource);

    @NonNull
    public Effect effect() {
        return mEffect == null ? Effect.FADE_IN_IF_NOT_CACHED : mEffect;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ViewBinder effect(Effect effect) {
        mEffect = effect;
        return this;
    }

    public ViewBinder<T> framing(FramingAlgorithm creator) {
        mFraming = creator;
        return this;
    }

    public FramingAlgorithm framing() {
        return mFraming;
    }
}
