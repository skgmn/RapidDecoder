package rapid.decoder.binder;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

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
                               boolean isAsync);

    public static Effect NO_EFFECT = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          boolean isAsync) {
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
                          boolean isAsync) {
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

    public static Effect FADE_IN_IF_SYNC = new Effect() {
        @Override
        public void apply(Context context, EffectTarget target, Drawable newDrawable,
                          boolean isAsync) {
            if (!isAsync) {
                NO_EFFECT.apply(context, target, newDrawable, false);
            } else {
                FADE_IN.apply(context, target, newDrawable, true);
            }
        }
    };
}
