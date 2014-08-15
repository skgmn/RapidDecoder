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
import rapid.decoder.cache.ResourcePool;
import rapid.decoder.compat.ImageViewCompat;
import rapid.decoder.compat.ViewCompat;
import rapid.decoder.frame.FramingMethod;
import rapid.decoder.frame.ScaleTypeFraming;

public abstract class ViewBinder<T extends View> implements Effect.EffectTarget {
    public interface OnReadyListener {
        void onReady(View v, boolean async);
    }

    protected static abstract class Pool<T extends ViewBinder<?>> extends ResourcePool<T> {
        @Override
        protected boolean onRecycle(T obj) {
            obj.reset();
            return true;
        }
    }

    private Effect mEffect;
    private FramingMethod mFraming;
    private WeakReference<T> mView;
    private DrawableInflater mPlaceholderInflater;
    private DrawableInflater mErrorImageInflater;

    protected ViewBinder() {
    }

    protected void init(T v) {
        mView = new WeakReference<T>(v);
    }

    protected void reset() {
        mEffect = null;
        mFraming = null;
        mView = null;
    }

    @Nullable
    public T getView() {
        return mView.get();
    }

    public Drawable createDrawable(Context context, @Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        } else {
            return new BitmapDrawable(context.getResources(), bitmap);
        }
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

    public ViewBinder<T> placeholder(final Drawable d) {
        mPlaceholderInflater = new DrawableInflater() {
            @Override
            public Drawable inflate(Context context) {
                return d;
            }
        };
        return this;
    }

    public ViewBinder<T> placeholder(final int resId) {
        mPlaceholderInflater = new DrawableInflater() {
            @Override
            public Drawable inflate(Context context) {
                return context.getResources().getDrawable(resId);
            }
        };
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ViewBinder<T> placeholder(DrawableInflater inflater) {
        mPlaceholderInflater = inflater;
        return this;
    }

    public ViewBinder<T> errorImage(final int resId) {
        mErrorImageInflater = new DrawableInflater() {
            @Override
            public Drawable inflate(Context context) {
                return context.getResources().getDrawable(resId);
            }
        };
        return this;
    }

    public ViewBinder<T> errorImage(DrawableInflater inflater) {
        mErrorImageInflater = inflater;
        return this;
    }

    public void runAfterReady(final OnReadyListener listener) {
        View v = getView();
        if (v == null) return;

        if ((v.getWidth() == 0 || v.getHeight() == 0) && v.isLayoutRequested() &&
                !shouldWrapContent(v)) {
            NextLayoutInspector.inspectNextLayout(v, new NextLayoutInspector.OnNextLayoutListener
                    () {
                @Override
                public void onNextLayout(View v) {
                    listener.onReady(v, true);
                }
            });
        } else {
            listener.onReady(v, false);
        }
    }

    private static boolean shouldWrapContent(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        return lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                lp.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public void recycle() {
    }

    @Override
    public void dispose() {
        recycle();
    }

    public void showPlaceholder() {
        if (mPlaceholderInflater != null) {
            View v = getView();
            if (v != null) {
                Drawable placeholder = mPlaceholderInflater.inflate(v.getContext());
                onPlaceholderInflated(placeholder);
                for (int i = 0, c = getDrawableCount(); i < c; ++i) {
                    if (!isDrawableEnabled(i)) continue;
                    setDrawable(i, placeholder);
                }
            }
        }
    }

    public void showErrorImage() {
        if (mErrorImageInflater != null) {
            View v = getView();
            if (v != null) {
                Drawable d = mErrorImageInflater.inflate(v.getContext());
                onPlaceholderInflated(d);
                Context context = v.getContext();
                for (int i = 0, c = getDrawableCount(); i < c; ++i) {
                    if (!isDrawableEnabled(i)) continue;
                    effect().apply(context, this, d, true);
                }
            }
        }
    }

    protected void onPlaceholderInflated(Drawable placeholder) {
    }

    protected void onErrorImageInflated(Drawable errorImage) {
    }

    public void bind(Bitmap bitmap, boolean isAsync) {
        View v = getView();
        if (v != null) {
            Drawable d = createDrawable(v.getContext(), bitmap);
            if (d != null) {
                effect().apply(v.getContext(), this, d, isAsync);
                return;
            }
        }
        recycle();
    }

    @NonNull
    public Effect effect() {
        return mEffect == null ? Effect.FADE_IN_IF_SYNC : mEffect;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ViewBinder effect(Effect effect) {
        mEffect = effect;
        return this;
    }

    public ViewBinder<T> framing(FramingMethod framing) {
        mFraming = framing;
        return this;
    }

    public FramingMethod framing() {
        return mFraming;
    }

    public int getLayoutWidth() {
        View v = getView();
        if (v == null) return 0;
        return v.getLayoutParams().width;
    }

    public int getLayoutHeight() {
        View v = getView();
        if (v == null) return 0;
        return v.getLayoutParams().height;
    }

    public int getWidth() {
        View v = getView();
        if (v == null) return 0;
        return v.getWidth();
    }

    public int getHeight() {
        View v = getView();
        if (v == null) return 0;
        return v.getHeight();
    }

    public int getMinWidth() {
        View v = getView();
        if (v == null) return 0;
        return ViewCompat.getMinimumWidth(v);
    }

    public int getMinHeight() {
        View v = getView();
        if (v == null) return 0;
        return ViewCompat.getMinimumHeight(v);
    }

    public int getMaxWidth() {
        View v = getView();
        if (v == null) return 0;
        return v instanceof ImageView ? ImageViewCompat.getMaxWidth((ImageView) v) : Integer
                .MAX_VALUE;
    }

    public int getMaxHeight() {
        View v = getView();
        if (v == null) return 0;
        return v instanceof ImageView ? ImageViewCompat.getMaxHeight((ImageView) v) : Integer
                .MAX_VALUE;
    }
}
