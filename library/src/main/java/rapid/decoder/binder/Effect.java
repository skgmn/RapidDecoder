package rapid.decoder.binder;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

import rapid.decoder.cache.CacheSource;

public abstract class Effect {
    public interface EffectTarget {
        Drawable getDrawable();

        void setDrawable(Drawable d);

        void postDelayed(Runnable r, int delay);
    }

    private static final int DURATION_ID = android.R.integer.config_mediumAnimTime;

    public abstract void apply(Context context, EffectTarget target, Drawable newDrawable,
                               CacheSource cacheSource);

    public static Effect NO_EFFECT = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          CacheSource cacheSource) {
            target.setDrawable(newDrawable);
        }
    };

    public static Effect FADE_IN = new Effect() {
        @Override
        public void apply(Context context, final EffectTarget target, final Drawable newDrawable,
                          CacheSource cacheSource) {
            Drawable oldDrawable = target.getDrawable();
            if (oldDrawable == null) {
                oldDrawable = new ColorDrawable(0);
            }
            final TransitionDrawable d = new TransitionDrawable(new Drawable[]{oldDrawable, newDrawable});
            int duration = context.getResources().getInteger(DURATION_ID);
            d.startTransition(duration);
            target.setDrawable(d);
            target.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (target.getDrawable() == d) {
                        target.setDrawable(newDrawable);
                    }
                }
            }, duration);
        }
    };

    public static Effect FADE_IN_IF_NOT_CACHED = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          CacheSource cacheSource) {
            if (CacheSource.MEMORY.equals(cacheSource)) {
                NO_EFFECT.apply(context, target, newDrawable, cacheSource);
            } else {
                FADE_IN.apply(context, target, newDrawable, cacheSource);
            }
        }
    };
}
