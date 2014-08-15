package rapid.decoder;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class TransitionDrawable extends Drawable {
    private static final int TRANSITION_NONE = 0;
    private static final int TRANSITION_STARTING = 1;
    private static final int TRANSITION_RUNNING = 2;
    private static final int TRANSITION_FINISHED = 3;

    private int mTransitionState = TRANSITION_NONE;

    private Drawable mDrawableBack;
    private Drawable mDrawableFront;
    private long mStartTimeMillis;
    private int mDuration;
    private int mAlpha = 0xff;
    private Runnable mEndAction;

    public TransitionDrawable(Drawable back, Drawable front) {
        mDrawableBack = back;
        mDrawableFront = front;
    }

    public void startTransition(int durationMillis) {
        mDuration = durationMillis;
        mTransitionState = TRANSITION_STARTING;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        boolean done = false;
        int alpha = 0;

        if (mTransitionState == TRANSITION_NONE) {
            mDrawableBack.draw(canvas);
            return;
        } else if (mTransitionState == TRANSITION_FINISHED) {
            mDrawableFront.draw(canvas);
            return;
        } else if (mTransitionState == TRANSITION_STARTING) {
            mStartTimeMillis = SystemClock.uptimeMillis();
            alpha = 0;
            mTransitionState = TRANSITION_RUNNING;
        } else {
            if (mStartTimeMillis >= 0) {
                float normalized = (float)
                        (SystemClock.uptimeMillis() - mStartTimeMillis) / mDuration;
                done = normalized >= 1.0f;
                normalized = Math.min(normalized, 1.0f);
                alpha = (int) (0xff * normalized);
            }
        }

        if (done) {
            mTransitionState = TRANSITION_FINISHED;
            mDrawableFront.draw(canvas);
            if (mEndAction != null) {
                scheduleSelf(mEndAction, SystemClock.uptimeMillis());
            }
            return;
        }

        final Drawable d = mDrawableBack;
        d.setAlpha(0xff - alpha);
        d.draw(canvas);
        d.setAlpha(mAlpha);

        if (alpha != 0) {
            final Drawable front = mDrawableFront;
            front.setAlpha(alpha);
            front.draw(canvas);
            front.setAlpha(mAlpha);
        }

        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            mDrawableBack.setAlpha(alpha);
            mDrawableFront.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setEndAction(Runnable r) {
        mEndAction = r;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mDrawableBack.setBounds(bounds);
        mDrawableFront.setBounds(bounds);
    }
}