package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.PAINT;
import static rapid.decoder.cache.ResourcePool.RECT;

class BitmapTransformer extends BitmapDecoder {
    private Bitmap mBitmap;
    private boolean mScaleFilter;
    private boolean mIsMutable = false;
    private Config mTargetConfig;

    BitmapTransformer(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    @Override
    public int sourceWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int sourceHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public String mimeType() {
        return "image/bmp";
    }

    private Bitmap redraw(Rect rectSrc, int targetWidth, int targetHeight) {
        Config config = (mTargetConfig != null ? mTargetConfig : mBitmap.getConfig());
        Bitmap bitmap2 = Bitmap.createBitmap(targetWidth, targetHeight, config);
        Canvas cv = new Canvas(bitmap2);

        Rect rectDest = RECT.obtain(0, 0, targetWidth, targetHeight);
        Paint paint = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);

        cv.drawBitmap(mBitmap, rectSrc, rectDest, paint);

        if (paint != null) {
            PAINT.recycle(paint);
        }
        RECT.recycle(rectDest);

        return bitmap2;
    }

    @Override
    protected Bitmap decode(boolean approximately) {
        resolveTransformations();
        Bitmap bitmap;
        if (mRegion != null) {
            bitmap = redraw(mRegion, width(), height());
        } else if (mRatioWidth != 1f || mRatioHeight != 1f) {
            bitmap = redraw(null, width(), height());
        } else {
            if (mIsMutable && !mBitmap.isMutable() ||
                    mTargetConfig != null && !mTargetConfig.equals(mBitmap.getConfig())) {
                bitmap = redraw(null, mBitmap.getWidth(), mBitmap.getHeight());
            } else {
                bitmap = mBitmap;
            }
        }
        return postProcess(bitmap);
    }

    @Override
    public void decode(@NonNull OnBitmapDecodedListener listener) {
        listener.onBitmapDecoded(decode(), CacheSource.MEMORY);
    }

    @Override
    public void draw(Canvas cv, int left, int top) {
        resolveTransformations();
        final Paint p = mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null;
        Rect rectDest = RECT.obtain(left, top, left + width(), top + height());
        cv.drawBitmap(mBitmap, mRegion, rectDest, p);
        RECT.recycle(rectDest);
        PAINT.recycle(p);
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public BitmapTransformer fork() {
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
    public int hashCode() {
        final int hashBitmap = mBitmap.hashCode();
        final int hashOptions = (mIsMutable ? 0x55555555 : 0) | (mScaleFilter ? 0xAAAAAAAA : 0);
        final int hashConfig = (mTargetConfig == null ? 0 : mTargetConfig.hashCode());

        return hashBitmap ^ hashOptions ^ hashConfig ^ transformationsHash();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BitmapTransformer)) return false;

        final BitmapTransformer d = (BitmapTransformer) o;
        return super.transformationsEqual(d) &&
                mIsMutable == d.mIsMutable &&
                mScaleFilter == d.mScaleFilter &&
                (mTargetConfig == null ? d.mTargetConfig == null : mTargetConfig == d.mTargetConfig) &&
                mBitmap.equals(d.mBitmap);
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
