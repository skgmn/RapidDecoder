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

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;
import rapid.decoder.NextLayoutInspector;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.frame.FramedDecoder;
import rapid.decoder.frame.FramedDecoderCreator;

public abstract class BitmapBinder<T extends View> implements Effect.EffectTarget {
    public interface OnReadyListener {
        void onReady(View v);
    }

    private Effect mEffect;
    private FramedDecoderCreator mFrameCreator;
    private WeakReference<T> mView;

    public BitmapBinder(T v) {
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

    public BitmapBinder<T> scaleType(final ImageView.ScaleType scaleType) {
        framedDecoderCreator(new FramedDecoderCreator() {
            @Override
            public Decodable createFramedDecoder(BitmapDecoder decoder, int frameWidth,
                                                 int frameHeight) {
                return FramedDecoder.newInstance(decoder, frameWidth, frameHeight, scaleType);
            }
        });
        return this;
    }

    public void runAfterReady(final OnReadyListener listener) {
        View v = getView();
        if (v == null) return;

        if (v.isLayoutRequested() && hasNoWrapContentLayoutParam(v)) {
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

    private static boolean hasNoWrapContentLayoutParam(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        return lp.width != ViewGroup.LayoutParams.WRAP_CONTENT &&
                lp.height != ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public abstract void bind(Bitmap bitmap, CacheSource cacheSource);

    @NonNull
    public Effect effect() {
        return mEffect == null ? Effect.FADE_IN_IF_NOT_CACHED : mEffect;
    }

    public BitmapBinder effect(Effect effect) {
        mEffect = effect;
        return this;
    }

    protected void framedDecoderCreator(FramedDecoderCreator creator) {
        mFrameCreator = creator;
    }

    public Decodable createFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        return mFrameCreator != null ? mFrameCreator.createFramedDecoder(decoder, frameWidth,
                frameHeight) : decoder;
    }
}
