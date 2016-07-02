package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.cache.CacheSource;

class DrawableTransformer extends BitmapDecoder {
    private Drawable mDrawable;
    private boolean mScaleFilter;
    private boolean mIsMutable = false;
    private Config mTargetConfig;

    DrawableTransformer(@NonNull Drawable d) {
        mDrawable = d;
    }

    @Override
    public int sourceWidth() {
        return mDrawable.getIntrinsicWidth();
    }

    @Override
    public int sourceHeight() {
        return mDrawable.getIntrinsicHeight();
    }

    @Override
    public String mimeType() {
        return "image/bmp";
    }

    private Bitmap redraw(Rect rectSrc, int targetWidth, int targetHeight) {
        Config config = getTargetConfig();
        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, config);
        Canvas canvas = new Canvas(bitmap);
        mDrawable.setFilterBitmap(mScaleFilter);
        if (rectSrc == null) {
            mDrawable.setBounds(0, 0, targetWidth, targetHeight);
        } else {
            float scaleWidth = (float) targetWidth / rectSrc.width();
            float scaleHeight = (float) targetHeight / rectSrc.height();
            int left = mRatioIntegerMode.toInteger(-rectSrc.left * scaleWidth);
            int top = mRatioIntegerMode.toInteger(-rectSrc.top * scaleHeight);
            int right = left + mRatioIntegerMode.toInteger(mDrawable.getIntrinsicWidth() * scaleWidth);
            int bottom = top + mRatioIntegerMode.toInteger(mDrawable.getIntrinsicHeight() * scaleHeight);
            mDrawable.setBounds(left, top, right, bottom);
        }
        mDrawable.draw(canvas);
        return bitmap;
    }

    @Nullable
    @Override
    protected Bitmap decode(boolean approximately) {
        resolveTransformations();
        Bitmap bitmap;
        if (mRegion != null) {
            bitmap = redraw(mRegion, width(), height());
        } else if (mRatioWidth != 1f || mRatioHeight != 1f) {
            bitmap = redraw(null, width(), height());
        } else {
            Bitmap nestedBitmap = getNestedBitmap();
            if (nestedBitmap == null || mIsMutable && !nestedBitmap.isMutable() ||
                    mTargetConfig != null && !mTargetConfig.equals(nestedBitmap.getConfig())) {
                bitmap = redraw(null, mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
            } else {
                bitmap = nestedBitmap;
            }
        }
        return postProcess(bitmap);
    }

    @Nullable
    private Bitmap getNestedBitmap() {
        return mDrawable instanceof BitmapDrawable ? ((BitmapDrawable) mDrawable).getBitmap() : null;
    }

    private Config getTargetConfig() {
        if (mTargetConfig != null) {
            return mTargetConfig;
        } else {
            return mDrawable.getOpacity() == PixelFormat.OPAQUE ? Config.RGB_565 : Config.ARGB_8888;
        }
    }

    @Override
    public void decode(@NonNull OnBitmapDecodedListener listener) {
        listener.onBitmapDecoded(decode(), CacheSource.MEMORY);
    }

    @Override
    public void draw(Canvas canvas, int left, int top) {
        resolveTransformations();
        int right = left + width();
        int bottom = top + height();
        mDrawable.setFilterBitmap(mScaleFilter);
        if (mRegion == null) {
            mDrawable.setBounds(left, top, right, bottom);
            mDrawable.draw(canvas);
        } else {
            canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.clipRect(left, top, right, bottom);
            float scaleWidth = (float) (right - left) / mRegion.width();
            float scaleHeight = (float) (bottom - top) / mRegion.height();
            left += + mRatioIntegerMode.toInteger(-mRegion.left * scaleWidth);
            top += mRatioIntegerMode.toInteger(-mRegion.top * scaleHeight);
            right = left + mRatioIntegerMode.toInteger(mDrawable.getIntrinsicWidth() * scaleWidth);
            bottom = top + mRatioIntegerMode.toInteger(mDrawable.getIntrinsicHeight() * scaleHeight);
            mDrawable.setBounds(left, top, right, bottom);
            canvas.restore();
        }
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public DrawableTransformer fork() {
        // TODO: Implement this
        return this;
    }

    @Override
    public void into(ViewBinder binder) {
        binder.bind(decode(), false);
    }

    @Override
    public CacheSource cacheSource() {
        return CacheSource.MEMORY;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public BitmapDecoder config(Config config) {
        mTargetConfig = config;
        return this;
    }

    @Override
    public Config config() {
        return mTargetConfig;
    }

    @Override
    public BitmapDecoder useBuiltInDecoder(boolean force) {
        return this;
    }

    @Override
    public BitmapDecoder mutable(boolean mutable) {
        this.mIsMutable = mutable;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DrawableTransformer)) return false;
        DrawableTransformer that = (DrawableTransformer) o;
        return mScaleFilter == that.mScaleFilter && mIsMutable == that.mIsMutable && mDrawable.equals(that.mDrawable)
                && mTargetConfig == that.mTargetConfig;

    }

    @Override
    public int hashCode() {
        int result = mDrawable.hashCode();
        result = 31 * result + (mScaleFilter ? 1 : 0);
        result = 31 * result + (mIsMutable ? 1 : 0);
        result = 31 * result + (mTargetConfig != null ? mTargetConfig.hashCode() : 0);
        return result;
    }

    @Override
    public BitmapDecoder filterBitmap(boolean filter) {
        mScaleFilter = filter;
        return this;
    }

    @Override
    public boolean filterBitmap() {
        return mScaleFilter;
    }
}
