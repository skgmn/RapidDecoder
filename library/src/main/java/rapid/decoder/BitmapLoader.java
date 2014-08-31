package rapid.decoder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.builtin.BuiltInDecoder;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.frame.FramingMethod;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class BitmapLoader extends BitmapDecoder {
    @SuppressWarnings("UnusedDeclaration")
    public static final int SIZE_AUTO = 0;

    protected Options mOptions;
    protected boolean mIsMutable;
    private boolean mScaleFilter = true;
    private boolean mUseBuiltInDecoder = false;
    Object mId;
    boolean mIsFromDiskCache;
    protected CacheSource mCacheSource;

    private int mSourceWidth;
    private int mSourceHeight;
    private boolean mBoundsDecoded;

    // Temporary variables
    private float mAdjustedDensityRatio;
    private float mAdjustedWidthRatio;
    private float mAdjustedHeightRatio;

    protected BitmapLoader() {
        super();
        mOptions = OPTIONS.obtain();
        mOptions.inScaled = false;
    }

    protected BitmapLoader(BitmapLoader other) {
        super(other);

        mId = other.mId;

        mOptions = CloneUtils.clone(other.mOptions);

        mIsMutable = other.mIsMutable;
        mScaleFilter = other.mScaleFilter;
        mUseBuiltInDecoder = other.mUseBuiltInDecoder;

        mSourceWidth = other.mSourceWidth;
        mSourceHeight = other.mSourceHeight;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            OPTIONS.recycle(mOptions);
        } finally {
            super.finalize();
        }
    }

    @Override
    public BitmapMeta getCachedMeta() {
        if (isMemoryCacheEnabled() && mId != null && sMemCache != null) {
            synchronized (sMemCacheLock) {
                return sMemCache.getMeta(mId);
            }
        } else {
            return null;
        }
    }

    protected void decodeBounds() {
        if (mBoundsDecoded) return;

        BitmapMeta meta = getCachedMeta();
        if (meta != null) {
            mSourceWidth = meta.width();
            mSourceHeight = meta.height();
            mOptions.inDensity = 0;
            mOptions.inTargetDensity = 0;
            return;
        }

        mOptions.inJustDecodeBounds = true;
        decode(mOptions);
        mOptions.inJustDecodeBounds = false;
        mBoundsDecoded = true;

        mSourceWidth = Math.max(0, mOptions.outWidth);
        mSourceHeight = Math.max(0, mOptions.outHeight);
    }

    @Override
    public int sourceWidth() {
        if (mSourceWidth == 0) {
            decodeBounds();
        }
        return mSourceWidth;
    }

    @Override
    public int sourceHeight() {
        if (mSourceHeight == 0) {
            decodeBounds();
        }
        return mSourceHeight;
    }

    @Override
    public Bitmap getCachedBitmap() {
        synchronized (sMemCacheLock) {
            if (sMemCache == null) return null;
            return sMemCache.get(this);
        }
    }

    @Override
    public Bitmap decode() {
        final boolean memCacheEnabled = isMemoryCacheEnabled();
        if (memCacheEnabled) {
            final Bitmap cachedBitmap = getCachedBitmap();
            if (cachedBitmap != null) {
                mCacheSource = CacheSource.MEMORY;
                return cachedBitmap;
            }
        }

        // reset

        mOptions.mCancel = false;
        mAdjustedDensityRatio = 0;

        //

        resolveCrafts();

        // Setup sample size.

        mAdjustedWidthRatio = mRatioWidth;
        mAdjustedHeightRatio = mRatioHeight;

        if (mRatioWidth != 1 || mRatioHeight != 1) {
            mOptions.inSampleSize = calculateInSampleSizeByRatio();
        } else {
            mOptions.inSampleSize = 1;
        }

        // Execute actual decoding

        if (mOptions.mCancel) return null;

        Bitmap bitmap = executeDecoding();
        if (bitmap == null) return null;

        // Scale it finally.

        if (mAdjustedWidthRatio != 1 || mAdjustedHeightRatio != 1) {
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            Bitmap bitmap2;

            int newWidth = mRatioIntegerMode.toInteger(bitmap.getWidth() * mAdjustedWidthRatio);
            int newHeight = mRatioIntegerMode.toInteger(bitmap.getHeight() * mAdjustedHeightRatio);
            bitmap2 = Bitmap.createBitmap(newWidth, newHeight, bitmap.getConfig());
            Canvas canvas = CANVAS.obtain(bitmap2);
            Paint paint = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
            Rect rectDest = RECT.obtain(0, 0, newWidth, newHeight);
            canvas.drawBitmap(bitmap, null, rectDest, paint);
            RECT.recycle(rectDest);
            PAINT.recycle(paint);
            CANVAS.recycle(canvas);

            bitmap.recycle();
            bitmap2.setDensity(mOptions.inTargetDensity);
            bitmap = bitmap2;
        }

        if (mOptions.mCancel) return null;
        bitmap = postProcess(bitmap);
        if (bitmap == null) return null;

        if (memCacheEnabled) {
            synchronized (sMemCacheLock) {
                if (sMemCache != null) {
                    sMemCache.put(this, bitmap);
                }
            }
        }

        mCacheSource = mIsFromDiskCache ? CacheSource.DISK : CacheSource.NOT_CACHED;
        return bitmap;
    }

    @Override
    public boolean isMemoryCacheEnabled() {
        return mId != null && super.isMemoryCacheEnabled();
    }

    private int calculateInSampleSizeByRatio() {
        int sampleSize = 1;
        while (mAdjustedWidthRatio <= 0.5f && mAdjustedHeightRatio <= 0.5f) {
            sampleSize *= 2;
            mAdjustedWidthRatio *= 2f;
            mAdjustedHeightRatio *= 2f;
        }

        return sampleSize;
    }

    private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
        boolean memCacheEnabled = isMemoryCacheEnabled();
        synchronized (sMemCacheLock) {
            memCacheEnabled &= (sMemCache != null);
        }
        if (memCacheEnabled) {
            return decode();
        }

        resolveCrafts();
        mOptions.inSampleSize = calculateSampleSize(regionWidth(), regionHeight(),
                targetWidth, targetHeight);

        Bitmap bitmap = executeDecoding();
        mCacheSource = mIsFromDiskCache ? CacheSource.DISK : CacheSource.NOT_CACHED;
        return bitmap;
    }

    @SuppressLint("NewApi")
    protected Bitmap executeDecoding() {
        final boolean regional = mRegion != null &&
                !(mRegion.left == 0 && mRegion.top == 0 &&
                        mRegion.width() == width() && mRegion.height() == height());
        final boolean useBuiltInDecoder =
                this.mUseBuiltInDecoder ||
                        (regional && Build.VERSION.SDK_INT < 10) ||
                        (mIsMutable && (Build.VERSION.SDK_INT < 11 || regional)) ||
                        (mOptions.inSampleSize > 1 && !mScaleFilter);

        if (useBuiltInDecoder || regional) {
            decodeBounds();
        }
        onDecodingStarted(useBuiltInDecoder);

        if (useBuiltInDecoder) {
            InputStream in = getInputStream();
            if (in == null) return null;

            TwiceReadableInputStream in2 = TwiceReadableInputStream.getInstanceFrom(in);
            try {
                return decodeBuiltIn(in2);
            } catch (UnsatisfiedLinkError ignored) {
                in2.seekToBeginning();
                return decodeInMemory(in2);
            }
        } else {
            if (regional) {
                return decodeRegional(mOptions, mRegion);
            } else {
                return decode(mOptions);
            }
        }
    }

    private Bitmap decodeInMemory(TwiceReadableInputStream in) {
        Bitmap bitmap = BitmapLoader.from(in).scaleBy(mRatioWidth, mRatioHeight).decode();
        if (bitmap == null) return null;

        mAdjustedWidthRatio = mAdjustedHeightRatio = 1;

        BitmapDecoder decoder = BitmapLoader.from(bitmap);
        if (mRegion != null) {
            int left = Math.round(mRegion.left * mRatioWidth);
            int top = Math.round(mRegion.top * mRatioHeight);
            int right = left + width();
            int bottom = top + height();
            decoder = decoder.region(left, top, right, bottom);
        }
        return decoder.config(mOptions.inPreferredConfig)
                .filterBitmap(mScaleFilter)
                .mutable(mIsMutable)
                .decode();
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

    @Nullable
    protected abstract Bitmap decode(Options opts);

    @Nullable
    protected abstract InputStream getInputStream();

    protected abstract BitmapRegionDecoder createBitmapRegionDecoder();

    protected void onDecodingStarted(boolean builtInDecoder) {
    }

    @Nullable
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

    protected Bitmap decodeBuiltIn(TwiceReadableInputStream in) {
        adjustDensityRatio(true);

        final BuiltInDecoder d = new BuiltInDecoder(in);
        d.setRegion(mRegion);
        d.setUseFilter(mScaleFilter);

        final Bitmap bitmap = d.decode(mOptions);
        d.close();

        return bitmap;
    }

    @Override
    public void cancel() {
        if (!BackgroundTaskManager.cancelStrong(this)) {
            mOptions.requestCancelDecode();
        }
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
        if (bitmap != bitmap2 && !isMemoryCacheEnabled()) {
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
        if (mAdjustedDensityRatio == 0) {
            if (checkRatio && (mRatioWidth != 1 || mRatioHeight != 1)) {
                mAdjustedDensityRatio = 1;
            } else {
                mAdjustedDensityRatio = densityRatio();

                while (mAdjustedDensityRatio <= 0.5) {
                    mOptions.inSampleSize *= 2;
                    mAdjustedDensityRatio *= 2;
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
        final int hashConfig = (mOptions.inPreferredConfig == null ? 0 : mOptions
                .inPreferredConfig.hashCode());

        mHashCode = hashId ^ hashOptions ^ hashConfig ^ craftsHash();
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
                craftsEqual(d);
    }

    BitmapDecoder id(Object id) {
        if (mId != null) {
            throw new IllegalStateException("id can be set only once.");
        }
        mId = id;
        mHashCode = 0;
        return this;
    }

    public BitmapDecoder id(@NonNull Uri uri) {
        return id((Object) uri);
    }

    public BitmapDecoder id(@NonNull String uri) {
        return id(Uri.parse(uri));
    }

    @ForInternalUse
    public Object a() {
        return mId;
    }

    @Override
    public BitmapDecoder filterBitmap(boolean filter) {
        mScaleFilter = filter;
        mHashCode = 0;
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

    @Override
    protected ViewFrameBuilder setupFrameBuilder(ViewBinder<?> binder, FramingMethod framing) {
        return new ViewFrameBuilder(this, binder, framing);
    }
}
