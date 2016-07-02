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
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.builtin.BuiltInDecoder;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.cache.MemoryCacheKey;
import rapid.decoder.frame.FramingMethod;

import static rapid.decoder.cache.ResourcePool.OPTIONS;
import static rapid.decoder.cache.ResourcePool.PAINT;
import static rapid.decoder.cache.ResourcePool.RECT;

public abstract class BitmapLoader extends BitmapDecoder {
    @SuppressWarnings("UnusedDeclaration")
    public static final int SIZE_AUTO = 0;

    protected Options mOptions;
    protected boolean mIsMutable;
    private boolean mScaleFilter = true;
    private boolean mUseBuiltInDecoder = false;
    private boolean mShouldConvertToOpaqueOnScale = false;
    Object mId;
    boolean mIsFromDiskCache;
    protected CacheSource mCacheSource;

    private int mSourceWidth;
    private int mSourceHeight;
    private String mMimeType;
    private boolean mBoundsDecoded;

    // Transient variables
    private float mAdjustedDensityRatio;
    private float mAdjustedWidthRatio;
    private float mAdjustedHeightRatio;

    protected BitmapLoader() {
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
        mShouldConvertToOpaqueOnScale = other.mShouldConvertToOpaqueOnScale;

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
        int sampleSize = mOptions.inSampleSize;
        mOptions.inSampleSize = 1;
        decode(mOptions);
        mOptions.inSampleSize = sampleSize;
        mOptions.inJustDecodeBounds = false;
        mBoundsDecoded = true;

        mSourceWidth = Math.max(0, mOptions.outWidth);
        mSourceHeight = Math.max(0, mOptions.outHeight);
        mMimeType = mOptions.outMimeType;
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
    public String mimeType() {
        if (mMimeType == null) {
            decodeBounds();
        }
        return mMimeType;
    }

    @Override
    public Bitmap getCachedBitmap(boolean approximately) {
        synchronized (sMemCacheLock) {
            if (sMemCache == null) return null;
            return sMemCache.get(new MemoryCacheKey(this, approximately));
        }
    }

    @Nullable
    @Override
    protected Bitmap decode(boolean approximately) {
        final boolean memCacheEnabled = isMemoryCacheEnabled();
        if (memCacheEnabled) {
            final Bitmap cachedBitmap = getCachedBitmap(approximately);
            if (cachedBitmap != null) {
                mCacheSource = CacheSource.MEMORY;
                return cachedBitmap;
            }
        }

        // reset

        mOptions.mCancel = false;
        mAdjustedDensityRatio = 0;

        //

        resolveTransformations();

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

        if (!approximately) {
            if (mAdjustedWidthRatio != 1 || mAdjustedHeightRatio != 1) {
                bitmap.setDensity(Bitmap.DENSITY_NONE);
                Bitmap bitmap2;

                int newWidth = mRatioIntegerMode.toInteger(bitmap.getWidth() * mAdjustedWidthRatio);
                int newHeight = mRatioIntegerMode.toInteger(bitmap.getHeight() * mAdjustedHeightRatio);
                Config newConfig = (mShouldConvertToOpaqueOnScale ? Config.RGB_565 : bitmap.getConfig());
                if (newConfig == null) {
                    newConfig = Config.ARGB_8888;
                }
                bitmap2 = Bitmap.createBitmap(newWidth, newHeight, newConfig);
                Canvas canvas = new Canvas(bitmap2);
                Paint paint = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
                if (Config.RGB_565.equals(newConfig) && !Config.RGB_565.equals(bitmap.getConfig())) {
                    if (paint == null) {
                        paint = PAINT.obtain();
                    }
                    paint.setDither(true);
                }
                Rect rectDest = RECT.obtain(0, 0, newWidth, newHeight);
                canvas.drawBitmap(bitmap, null, rectDest, paint);
                RECT.recycle(rectDest);
                PAINT.recycle(paint);

                bitmap.recycle();
                bitmap2.setDensity(mOptions.inTargetDensity);
                bitmap = bitmap2;
            } else if (mShouldConvertToOpaqueOnScale) {
                Bitmap bitmap2 = new BitmapTransformer(bitmap).config(Config.RGB_565).decode();
                if (bitmap != bitmap2) {
                    bitmap.recycle();
                }
                bitmap = bitmap2;
            }
        }

        if (mOptions.mCancel) return null;
        bitmap = postProcess(bitmap);
        if (bitmap == null) return null;

        // TODO: Check !approximately cache whether it's indentical
        if (memCacheEnabled) {
            synchronized (sMemCacheLock) {
                if (sMemCache != null) {
                    sMemCache.put(new MemoryCacheKey(this, approximately), bitmap);
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

    @SuppressLint("NewApi")
    protected Bitmap executeDecoding() {
        final boolean regional = mRegion != null &&
                !(mRegion.left == 0 && mRegion.top == 0 &&
                        mRegion.width() == sourceWidth() && mRegion.height() == sourceHeight());
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
            InputStream in = openInputStream();
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
    protected abstract InputStream openInputStream();

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
    public void draw(Canvas cv, int left, int top) {
        final Bitmap bitmap = decode(true);
        if (bitmap == null) return;

        int width = width();
        int height = height();
        if (bitmap.getWidth() == width && bitmap.getHeight() == height) {
            cv.drawBitmap(bitmap, left, top, null);
        } else {
            final Paint p = (mScaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
            Rect rectDest = RECT.obtain(left, top, left + width, top + height);
            cv.drawBitmap(bitmap, null, rectDest, p);
            RECT.recycle(rectDest);
            PAINT.recycle(p);
        }
        bitmap.recycle();
    }

    @Override
    public BitmapLoader useBuiltInDecoder(boolean force) {
        this.mUseBuiltInDecoder = force;
        return this;
    }

    @Override
    public BitmapLoader config(Config config) {
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
        if (mHashCode == 0) {
            final int hashId = (mId != null ? mId.hashCode() : 0);
            final int hashOptions = (mIsMutable ? 1 : 0) + 31 * (mScaleFilter ? 1 : 0);
            final int hashConfig = (mOptions.inPreferredConfig == null ? 0 : mOptions.inPreferredConfig.hashCode());

            mHashCode = hashId + 31 * (hashOptions + 31 * (hashConfig + 31 * transformationsHash()));
        }
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
                transformationsEqual(d);
    }

    BitmapLoader id(Object id) {
        if (mId != null) {
            throw new IllegalStateException("id can be set only once.");
        }
        mId = id;
        mHashCode = 0;
        return this;
    }

    public BitmapLoader id(@NonNull Uri uri) {
        return id((Object) uri);
    }

    public BitmapLoader id(@NonNull String uri) {
        return id(Uri.parse(uri));
    }

    public Object id() {
        return mId;
    }

    @Override
    public BitmapLoader filterBitmap(boolean filter) {
        mScaleFilter = filter;
        mHashCode = 0;
        return this;
    }

    @Override
    public boolean filterBitmap() {
        return mScaleFilter;
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

    @SuppressWarnings("UnusedDeclaration")
    public BitmapLoader quality(@NonNull Quality quality) {
        quality.applyTo(mOptions);
        mShouldConvertToOpaqueOnScale = quality.shouldConvertToOpaqueOnScale();
        return this;
    }

    @Override
    public BitmapLoader useMemoryCache(boolean useCache) {
        super.useMemoryCache(useCache);
        return this;
    }
}
