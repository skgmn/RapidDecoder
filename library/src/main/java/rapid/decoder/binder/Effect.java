package rapid.decoder.binder;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;

import rapid.decoder.cache.CacheSource;

public abstract class Effect {
    public interface EffectTarget {
        int getDrawableCount();

        boolean isDrawableEnabled(int index);

        Drawable getDrawable(int index);

        void setDrawable(int index, Drawable d);

        void postDelayed(Runnable r, int delay);
    }

    private static final int DURATION_ID = android.R.integer.config_mediumAnimTime;

    public abstract void apply(Context context, EffectTarget target, Drawable newDrawable,
                               CacheSource cacheSource);

    public static Effect NO_EFFECT = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          CacheSource cacheSource) {
            int count = target.getDrawableCount();
            for (int i = 0; i < count; ++i) {
                if (!target.isDrawableEnabled(i)) continue;
                target.setDrawable(i, newDrawable);
            }
        }
    };

    public static Effect FADE_IN = new Effect() {
        @Override
        public void apply(Context context, final EffectTarget target, final Drawable newDrawable,
                          CacheSource cacheSource) {
            int count = target.getDrawableCount();
            for (int i = 0; i < count; ++i) {
                if (!target.isDrawableEnabled(i)) continue;
                Drawable oldDrawable = target.getDrawable(i);
                if (oldDrawable == null) {
                    oldDrawable = new ColorDrawable(0);
                }
                final TransitionDrawable d = new TransitionDrawable(new Drawable[]{oldDrawable, newDrawable});
                int duration = context.getResources().getInteger(DURATION_ID);
                d.startTransition(duration);
                target.setDrawable(i, d);
                final int finalI = i;
                target.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (target.getDrawable(finalI) == d) {
                            target.setDrawable(finalI, newDrawable);
                        }
                    }
                }, duration);
            }
        }
    };

    public static Effect FADE_IN_IF_NOT_CACHED = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          CacheSource cacheSource) {

            Log.e("asdf", "cache source = " + cacheSource);
            if (CacheSource.MEMORY.equals(cacheSource)) {
                NO_EFFECT.apply(context, target, newDrawable, cacheSource);
            } else {
                FADE_IN.apply(context, target, newDrawable, cacheSource);
            }
        }
    };
}
