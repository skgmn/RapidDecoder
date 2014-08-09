package rapid.decoder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.InputStream;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.builtin.BuiltInDecoder;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.compat.ImageViewCompat;
import rapid.decoder.compat.ViewCompat;
import rapid.decoder.frame.FramingMethod;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class BitmapLoader extends BitmapDecoder {
    @SuppressWarnings("UnusedDeclaration")
    public static final int SIZE_AUTO = 0;

    protected Options mOptions;
    protected boolean mIsMutable;
    private boolean mScaleFilter = true;
    private boolean mUseBuiltInDecoder = false;
    private boolean memCacheEnabled = true;
    private Object mId;
    boolean isFromDiskCache;
    protected CacheSource mCacheSource;

    private int width;
    private int height;

    // Temporary variables
    private float adjustedDensityRatio;
    private float adjustedWidthRatio;
    private float adjustedHeightRatio;

    protected BitmapLoader() {
        super();
        mOptions = OPTIONS.obtain();
        mOptions.inScaled = false;
    }

    protected BitmapLoader(BitmapLoader other) {
        super(other);

        mId = other.mId;

        mOptions = Cloner.clone(other.mOptions);

        mIsMutable = other.mIsMutable;
        mScaleFilter = other.mScaleFilter;
        mUseBuiltInDecoder = other.mUseBuiltInDecoder;

        width = other.width;
        height = other.height;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            OPTIONS.recycle(mOptions);
        } finally {
            super.finalize();
        }
    }

    protected void decodeBounds() {
        if (isMemCacheEnabled()) {
            Bitmap cachedBitmap = getCachedBitmap();
            if (cachedBitmap != null) {
                width = cachedBitmap.getWidth();
                height = cachedBitmap.getHeight();
                mOptions.inDensity = 0;
                mOptions.inTargetDensity = 0;
                return;
            }
        }

        mOptions.inJustDecodeBounds = true;
        decode(mOptions);
        mOptions.inJustDecodeBounds = false;

        width = mOptions.outWidth;
        height = mOptions.outHeight;
    }

    @Override
    public int sourceWidth() {
        if (width == 0) {
            decodeBounds();
        }
        return width;
    }

    @Override
    public int sourceHeight() {
        if (height == 0) {
            decodeBounds();
        }
        return height;
    }

    public Bitmap getCachedBitmap() {
        synchronized (sMemCacheLock) {
            if (sMemCache == null) return null;
            return sMemCache.get(this);
        }
    }

    @Override
    public Bitmap decode() {
        final boolean memCacheEnabled = isMemCacheEnabled();
        if (memCacheEnabled) {
            final Bitmap cachedBitmap = getCachedBitmap();
            if (cachedBitmap != null) {
                mCacheSource = CacheSource.MEMORY;
                return cachedBitmap;
            }
        }

        // reset

        mOptions.mCancel = false;
        adjustedDensityRatio = 0;

        //

        resolveRequests();

        // Setup sample size.

        final boolean postScaleBy = (mRatioWidth != 1 || mRatioHeight != 1);
        if (postScaleBy) {
            mOptions.inSampleSize = calculateInSampleSizeByRatio();
        } else {
            mOptions.inSampleSize = 1;
        }

        // Execute actual decoding

        if (mOptions.mCancel) return null;

        Bitmap bitmap = executeDecoding();
        if (bitmap == null) return null;

        // Scale it finally.

        if (postScaleBy) {
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            Bitmap bitmap2;

            Matrix m = MATRIX.obtain();
            m.setScale(adjustedWidthRatio, adjustedHeightRatio);
            bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                    mScaleFilter);
            MATRIX.recycle(m);

            if (bitmap != bitmap2) {
                bitmap.recycle();
            }

            bitmap2.setDensity(mOptions.inTargetDensity);
            bitmap = bitmap2;
        }

        if (memCacheEnabled) {
            synchronized (sMemCacheLock) {
                if (sMemCache != null) {
                    sMemCache.put(this, bitmap);
                }
            }
        }

        mCacheSource = isFromDiskCache ? CacheSource.DISK : CacheSource.NOT_CACHED;
        return bitmap;
    }

    private boolean isMemCacheEnabled() {
        return this.memCacheEnabled && isMemCacheSupported();
    }

    private int calculateInSampleSizeByRatio() {
        adjustedWidthRatio = mRatioWidth;
        adjustedHeightRatio = mRatioHeight;

        int sampleSize = 1;
        while (adjustedWidthRatio <= 0.5f && adjustedHeightRatio <= 0.5f) {
            sampleSize *= 2;
            adjustedWidthRatio *= 2f;
            adjustedHeightRatio *= 2f;
        }

        return sampleSize;
    }

    private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
        boolean memCacheEnabled = isMemCacheEnabled();
        synchronized (sMemCacheLock) {
            memCacheEnabled &= (sMemCache != null);
        }
        if (memCacheEnabled) {
            return decode();
        }

        resolveRequests();
        mOptions.inSampleSize = calculateSampleSize(regionWidth(), regionHeight(),
                targetWidth, targetHeight);

        Bitmap bitmap = executeDecoding();
        mCacheSource = isFromDiskCache ? CacheSource.DISK : CacheSource.NOT_CACHED;
        return bitmap;
    }

    @SuppressLint("NewApi")
    protected Bitmap executeDecoding() {
        final boolean regional = mRegion != null &&
                !(mRegion.left == 0 && mRegion.top == 0 &&
                        mRegion.width() == width() && mRegion.height() == height());
        final boolean useBuiltInDecoder =
                this.mUseBuiltInDecoder ||
                        (regional && Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) ||
                        (mIsMutable && (Build.VERSION.SDK_INT < 11 || regional)) ||
                        (mOptions.inSampleSize > 1 && !mScaleFilter);

        onDecodingStarted(useBuiltInDecoder);

        try {
            if (useBuiltInDecoder) {
                return decodeBuiltIn(mRegion);
            } else {
                if (regional) {
                    return decodeRegional(mOptions, mRegion);
                } else {
                    return decode(mOptions);
                }
            }
        } finally {
            onDecodingFinished();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public BitmapLoader mutable(boolean mutable) {
        this.mIsMutable = mutable;
        if (Build.VERSION.SDK_INT >= 11) {
            mOptions.inMutable = mutable;
        }
        mHashCode = 0;
        return this;
    }

    protected abstract Bitmap decode(Options opts);

    protected abstract InputStream getInputStream();

    protected abstract BitmapRegionDecoder createBitmapRegionDecoder();

    protected abstract boolean isMemCacheSupported();

    protected void onDecodingStarted(boolean builtInDecoder) {
    }

    protected void onDecodingFinished() {
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    protected Bitmap decodeRegional(Options opts, Rect region) {
        adjustDensityRatio(false);

        final BitmapRegionDecoder d = createBitmapRegionDecoder();
        return (d == null ? null : d.decodeRegion(region, opts));
    }

    private static int calculateSampleSize(int width, int height, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    protected Bitmap decodeBuiltIn(Rect region) {
        final InputStream in = getInputStream();
        if (in == null) return null;

        adjustDensityRatio(true);

        final BuiltInDecoder d = new BuiltInDecoder(in);
        d.setRegion(region);
        d.setUseFilter(mScaleFilter);

        final Bitmap bitmap = d.decode(mOptions);
        d.close();

        return mIsMutable ? bitmap : Bitmap.createBitmap(bitmap);
    }

    @Override
    public void cancel() {
        mOptions.requestCancelDecode();
    }

    @Override
    public void draw(Canvas cv, Rect rectDest) {
        final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());

        final Paint p = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
        cv.drawBitmap(bitmap, null, rectDest, p);
        PAINT.recycle(p);

        bitmap.recycle();
    }

    @Override
    @Nullable
    public Bitmap createAndDraw(int width, int height, @NonNull Rect rectDest,
                                @Nullable Drawable background) {

        Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());
        if (bitmap == null) return null;

        Bitmap bitmap2;
        if (rectDest.left == 0 && rectDest.top == 0 && rectDest.right == width && rectDest.bottom
                == height) {

            bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, mScaleFilter);
        } else {
            bitmap2 = Bitmap.createBitmap(width, height, bitmap.getConfig());
            Canvas cv = CANVAS.obtain(bitmap2);
            if (background != null) {
                background.setBounds(0, 0, width, height);
                background.draw(cv);
            }
            Paint p = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
            cv.drawBitmap(bitmap, null, rectDest, p);
            PAINT.recycle(p);
            CANVAS.recycle(cv);
        }

        // Don't recycle it if memory cache is enabled because it could be from the cache.
        if (bitmap != bitmap2 && !isMemCacheEnabled()) {
            bitmap.recycle();
        }
        return bitmap2;
    }

    @Override
    public BitmapDecoder useBuiltInDecoder(boolean force) {
        this.mUseBuiltInDecoder = force;
        return this;
    }

    @Override
    public BitmapDecoder config(Config config) {
        mOptions.inPreferredConfig = config;
        return this;
    }

    public Config config() {
        return mOptions.inPreferredConfig;
    }

    private void adjustDensityRatio(boolean checkRatio) {
        if (adjustedDensityRatio == 0) {
            if (checkRatio && (mRatioWidth != 1 || mRatioHeight != 1)) {
                adjustedDensityRatio = 1;
            } else {
                adjustedDensityRatio = getDensityRatio();

                while (adjustedDensityRatio <= 0.5) {
                    mOptions.inSampleSize *= 2;
                    adjustedDensityRatio *= 2;
                }
            }
        }
    }

    @Override
    public int hashCode() {
        if (mHashCode != 0) {
            return mHashCode;
        }

        final int hashId = (mId != null ? mId.hashCode() : 0);
        final int hashOptions = (mIsMutable ? 0x55555555 : 0) | (mScaleFilter ? 0xAAAAAAAA : 0);
        final int hashConfig = (mOptions.inPreferredConfig == null ? HASHCODE_NULL_BITMAP_OPTIONS :
                mOptions.inPreferredConfig.hashCode());

        mHashCode = hashId ^ hashOptions ^ hashConfig ^ requestsHash();
        return mHashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BitmapLoader)) return false;

        final BitmapLoader d = (BitmapLoader) o;

        final Config config1 = mOptions.inPreferredConfig;
        final Config config2 = d.mOptions.inPreferredConfig;

        return (config1 == null ? config2 == null : config1.equals(config2)) &&
                (mId == null ? d.mId == null : mId.equals(d.mId)) &&
                mIsMutable == d.mIsMutable &&
                mScaleFilter == d.mScaleFilter &&
                requestsEquals(d);
    }

    BitmapDecoder id(Object id) {
        mId = id;
        mHashCode = 0;
        return this;
    }

    public Object id() {
        return mId;
    }

    @Override
    public BitmapDecoder filterBitmap(boolean filter) {
        mScaleFilter = filter;
        mHashCode = 0;
        return this;
    }

    BitmapLoader memCacheEnabled(boolean useCache) {
        this.memCacheEnabled = useCache;
        return this;
    }

    @Override
    public CacheSource cacheSource() {
        return mCacheSource;
    }

    @Override
    public boolean isCancelled() {
        return mOptions.mCancel;
    }

    protected void setupLoadTask(BitmapLoadTask task, View v, ViewBinder<?> binder) {
        FramingMethod framing = binder.framing();
        if (framing != null) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();

            if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    task.setFraming(framing, BitmapLoadTask.AUTOSIZE_BOTH,
                            ViewCompat.getMinimumWidth(v), ViewCompat.getMinimumHeight(v),
                            getMaxWidth(v), getMaxHeight(v));
                } else {
                    task.setFraming(framing, BitmapLoadTask.AUTOSIZE_WIDTH,
                            ViewCompat.getMinimumWidth(v), v.getHeight(),
                            getMaxWidth(v), 0);
                }
            } else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                task.setFraming(framing, BitmapLoadTask.AUTOSIZE_HEIGHT, v.getWidth(),
                        ViewCompat.getMinimumHeight(v), 0, getMaxHeight(v));
            } else {
                task.setFraming(framing, BitmapLoadTask.AUTOSIZE_NONE, v.getWidth(), v.getHeight(),
                        0, 0);
            }
        }
    }

    private static int getMaxWidth(View v) {
        return v instanceof ImageView ? ImageViewCompat.getMaxWidth((ImageView) v) : Integer
                .MAX_VALUE;
    }

    private static int getMaxHeight(View v) {
        return v instanceof ImageView ? ImageViewCompat.getMaxHeight((ImageView) v) : Integer
                .MAX_VALUE;
    }
}
