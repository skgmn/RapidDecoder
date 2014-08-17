package rapid.decoder;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.*;

class BitmapTransformer extends BitmapDecoder {
    private Bitmap mBitmap;
    private boolean scaleFilter;
    private Rect region;
    private boolean mutable = false;
    private Config targetConfig;

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

    private Bitmap redraw(Bitmap bitmap, Rect rectSrc, int targetWidth, int targetHeight) {
        Config config = (targetConfig != null ? targetConfig : bitmap.getConfig());
        Bitmap bitmap2 = Bitmap.createBitmap(targetWidth, targetHeight, config);
        Canvas cv = CANVAS.obtain(bitmap2);

        Rect rectDest = RECT.obtain(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
        Paint paint = (scaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);

        cv.drawBitmap(bitmap, rectSrc, rectDest, paint);

        if (paint != null) {
            PAINT.recycle(paint);
        }
        RECT.recycle(rectDest);

        CANVAS.recycle(cv);

        return (mutable ? bitmap2 : Bitmap.createBitmap(bitmap2));
    }

    @Override
    public Bitmap decode() {
        resolveRequests();

        final boolean redraw = !((targetConfig == null || mBitmap.getConfig().equals
                (targetConfig)) && !mutable);
        Bitmap bitmap;

        if (region != null) {
            if (mRatioWidth == 1f && mRatioHeight == 1f) {
                if (!redraw) {
                    bitmap = Bitmap.createBitmap(mBitmap, region.left, region.top, region.width(),
                            region.height());
                } else {
                    bitmap = redraw(mBitmap, region, region.width(), region.height());
                }
            } else {
                if (!redraw) {
                    Matrix m = MATRIX.obtain();
                    m.setScale(mRatioWidth, mRatioHeight);

                    bitmap = Bitmap.createBitmap(mBitmap,
                            region.left, region.top,
                            region.width(), region.height(),
                            m, scaleFilter);

                    MATRIX.recycle(m);
                } else {
                    bitmap = redraw(mBitmap, region,
                            Math.round(mRatioWidth * region.width()),
                            Math.round(mRatioHeight * region.height()));
                }
            }
        } else if (mRatioWidth != 1f || mRatioHeight != 1f) {
            if (!redraw) {
                Matrix m = MATRIX.obtain();
                m.setScale(mRatioWidth, mRatioHeight);

                bitmap = Bitmap.createBitmap(mBitmap,
                        0, 0, mBitmap.getWidth(), mBitmap.getHeight(),
                        m, scaleFilter);

                MATRIX.recycle(m);
            } else {
                bitmap = redraw(mBitmap, null,
                        Math.round(mRatioWidth * mBitmap.getWidth()),
                        Math.round(mRatioHeight * mBitmap.getHeight()));
            }
        } else {
            if (!redraw) {
                bitmap = mBitmap;
            } else {
                bitmap = redraw(mBitmap, null, mBitmap.getWidth(), mBitmap.getHeight());
            }
        }

        return postProcess(bitmap);
    }

    @Override
    public void decode(@NonNull OnBitmapDecodedListener listener) {
        listener.onBitmapDecoded(decode(), CacheSource.MEMORY);
    }

    @Override
    public BitmapDecoder region(int left, int top, int right, int bottom) {
        if (region == null) {
            region = RECT.obtainNotReset();
        }
        region.set(left, top, right, bottom);

        return this;
    }

    @Override
    public void draw(Canvas cv, Rect rectDest) {
        final Paint p = PAINT.obtain(Paint.FILTER_BITMAP_FLAG);
        cv.drawBitmap(mBitmap, region, rectDest, p);
        PAINT.recycle(p);
    }

    @Override
    protected void finalize() throws Throwable {
        if (region != null) {
            RECT.recycle(region);
        }
        super.finalize();
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public BitmapTransformer mutate() {
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
    public BitmapDecoder region(Rect region) {
        if (region == null) {
            if (this.region != null) {
                RECT.recycle(this.region);
            }
            this.region = null;
        } else {
            region(region.left, region.top, region.right, region.bottom);
        }

        return this;
    }

    @Override
    public BitmapDecoder config(Config config) {
        targetConfig = config;
        return this;
    }

    @Override
    public Config config() {
        return targetConfig;
    }

    @Override
    public Bitmap createAndDraw(int width, int height, @NonNull Rect rectDest, @Nullable Drawable
            background) {

        Bitmap bitmap2;
        if (rectDest.left == 0 && rectDest.top == 0 && rectDest.right == width && rectDest.bottom
                == height) {

            bitmap2 = Bitmap.createScaledBitmap(mBitmap, width, height, scaleFilter);
        } else {
            bitmap2 = Bitmap.createBitmap(width, height, mBitmap.getConfig());
            Canvas cv = CANVAS.obtain(bitmap2);
            if (background != null) {
                background.setBounds(0, 0, width, height);
                background.draw(cv);
            }
            Paint p = (scaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
            cv.drawBitmap(mBitmap, null, rectDest, p);
            PAINT.recycle(p);
            CANVAS.recycle(cv);
        }

        if (mBitmap != bitmap2) {
            mBitmap.recycle();
        }
        return bitmap2;
    }

    @Override
    public BitmapDecoder useBuiltInDecoder(boolean force) {
        return this;
    }

    @Override
    public BitmapDecoder mutable(boolean mutable) {
        this.mutable = mutable;
        return this;
    }

    @Override
    public int hashCode() {
        final int hashBitmap = mBitmap.hashCode();
        final int hashOptions = (mutable ? 0x55555555 : 0) | (scaleFilter ? 0xAAAAAAAA : 0);
        final int hashConfig = (targetConfig == null ? 0 : targetConfig.hashCode());

        return hashBitmap ^ hashOptions ^ hashConfig ^ requestsHash();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BitmapTransformer)) return false;

        final BitmapTransformer d = (BitmapTransformer) o;
        return mBitmap.equals(d.mBitmap) &&
                (region == null ? d.region == null : region.equals(d.region)) &&
                mutable == d.mutable &&
                scaleFilter == d.scaleFilter &&
                (targetConfig == null ? d.targetConfig == null : targetConfig.equals(d.targetConfig));
    }

    @Override
    public BitmapDecoder filterBitmap(boolean filter) {
        scaleFilter = filter;
        return this;
    }
}
