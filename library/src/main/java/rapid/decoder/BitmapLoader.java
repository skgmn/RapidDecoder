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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.builtin.BuiltInDecoder;
import rapid.decoder.cache.CacheSource;
import rapid.decoder.compat.ImageViewCompat;
import rapid.decoder.compat.ViewCompat;
import rapid.decoder.frame.FramingAlgorithm;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class BitmapLoader extends BitmapDecoder {
    @SuppressWarnings("UnusedDeclaration")
    public static final int SIZE_AUTO = 0;

    private static AtomicReference<DecodeResult> sDecodeResultRef = new AtomicReference
            <DecodeResult>();

    protected Options opts;
    protected boolean mutable;
    private boolean scaleFilter = true;
    private boolean useBuiltInDecoder = false;
    private boolean memCacheEnabled = true;
    Object id;
    boolean isFromDiskCache;

    private int width;
    private int height;

    // Temporary variables
    private float adjustedDensityRatio;
    private float adjustedWidthRatio;
    private float adjustedHeightRatio;

    protected BitmapLoader() {
        super();
        opts = OPTIONS.obtain();
        opts.inScaled = false;
    }

    protected BitmapLoader(BitmapLoader other) {
        super(other);

        id = other.id;

        opts = Cloner.clone(other.opts);

        mutable = other.mutable;
        scaleFilter = other.scaleFilter;
        useBuiltInDecoder = other.useBuiltInDecoder;

        width = other.width;
        height = other.height;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            OPTIONS.recycle(opts);
        } finally {
            super.finalize();
        }
    }

    protected void decodeBounds() {
        opts.inJustDecodeBounds = true;
        decode(opts);
        opts.inJustDecodeBounds = false;

        width = opts.outWidth;
        height = opts.outHeight;
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
        DecodeResult result = obtainResultObject();
        decode(result);
        Bitmap bitmap = result.bitmap;
        recycleResultObject(result);
        return bitmap;
    }

    protected static DecodeResult obtainResultObject() {
        DecodeResult result = sDecodeResultRef.getAndSet(null);
        if (result == null) {
            result = new DecodeResult();
        }
        return result;
    }

    protected static void recycleResultObject(DecodeResult result) {
        result.bitmap = null;
        sDecodeResultRef.set(result);
    }

    private int calculateInSampleSizeByRatio() {
        adjustedWidthRatio = ratioWidth;
        adjustedHeightRatio = ratioHeight;

        int sampleSize = 1;
        while (adjustedWidthRatio <= 0.5f && adjustedHeightRatio <= 0.5f) {
            sampleSize *= 2;
            adjustedWidthRatio *= 2f;
            adjustedHeightRatio *= 2f;
        }

        return sampleSize;
    }

    private Bitmap decodeDontResizeButSample(int targetWidth, int targetHeight) {
        resolveRequests();
        opts.inSampleSize = calculateInSampleSize(regionWidth(), regionHeight(),
                targetWidth, targetHeight);
        return executeDecoding();
    }

    @SuppressLint("NewApi")
    protected Bitmap executeDecoding() {
        final boolean regional = region != null &&
                !(region.left == 0 && region.top == 0 &&
                        region.width() == width() && region.height() == height());
        final boolean useBuiltInDecoder =
                this.useBuiltInDecoder ||
                        (mutable && (Build.VERSION.SDK_INT < 11 || regional)) ||
                        (opts.inSampleSize > 1 && !scaleFilter);

        onDecodingStarted(useBuiltInDecoder);

        try {
            if (useBuiltInDecoder) {
                return builtInDecode(region);
            } else {
                if (regional) {
                    return decodeRegional(opts, region);
                } else {
                    return decode(opts);
                }
            }
        } finally {
            onDecodingFinished();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public BitmapLoader mutable(boolean mutable) {
        this.mutable = mutable;
        if (Build.VERSION.SDK_INT >= 11) {
            opts.inMutable = mutable;
        }
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

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
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

    protected Bitmap builtInDecode(Rect region) {
        final InputStream in = getInputStream();
        if (in == null) return null;

        adjustDensityRatio(true);

        final BuiltInDecoder d = new BuiltInDecoder(in);
        d.setRegion(region);
        d.setUseFilter(scaleFilter);

        final Bitmap bitmap = d.decode(opts);
        d.close();

        return mutable ? bitmap : Bitmap.createBitmap(bitmap);
    }

    @Override
    public void cancel() {
        opts.requestCancelDecode();
    }

    @Override
    public void draw(Canvas cv, Rect rectDest) {
        final Bitmap bitmap = decodeDontResizeButSample(rectDest.width(), rectDest.height());

        final Paint p = (scaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
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

            bitmap2 = Bitmap.createScaledBitmap(bitmap, width, height, scaleFilter);
        } else {
            bitmap2 = Bitmap.createBitmap(width, height, bitmap.getConfig());
            Canvas cv = CANVAS.obtain(bitmap2);
            if (background != null) {
                background.setBounds(0, 0, width, height);
                background.draw(cv);
            }
            Paint p = (scaleFilter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null);
            cv.drawBitmap(bitmap, null, rectDest, p);
            PAINT.recycle(p);
            CANVAS.recycle(cv);
        }

        if (bitmap != bitmap2) {
            bitmap.recycle();
        }
        return bitmap2;
    }

    @Override
    public BitmapDecoder useBuiltInDecoder(boolean force) {
        this.useBuiltInDecoder = force;
        return this;
    }

    @Override
    public BitmapDecoder config(Config config) {
        opts.inPreferredConfig = config;
        return this;
    }

    public Config config() {
        return opts.inPreferredConfig;
    }

    private void adjustDensityRatio(boolean checkRatio) {
        if (adjustedDensityRatio == 0) {
            if (checkRatio && (ratioWidth != 1 || ratioHeight != 1)) {
                adjustedDensityRatio = 1;
            } else {
                adjustedDensityRatio = getDensityRatio();

                while (adjustedDensityRatio <= 0.5) {
                    opts.inSampleSize *= 2;
                    adjustedDensityRatio *= 2;
                }
            }
        }
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }

        final int hashId = (id != null ? id.hashCode() : 0);
        final int hashRegion = (region == null ? HASHCODE_NULL_REGION : region.hashCode());
        final int hashOptions = (mutable ? 0x55555555 : 0) | (scaleFilter ? 0xAAAAAAAA : 0);
        final int hashConfig = (opts.inPreferredConfig == null ? HASHCODE_NULL_BITMAP_OPTIONS :
                opts.inPreferredConfig.hashCode());

        hashCode = hashId ^ hashRegion ^ hashOptions ^ hashConfig ^ queriesHash();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BitmapLoader)) return false;

        final BitmapLoader d = (BitmapLoader) o;

        final Config config1 = opts.inPreferredConfig;
        final Config config2 = d.opts.inPreferredConfig;

        return (region == null ? d.region == null : region.equals(d.region)) &&
                (config1 == null ? config2 == null : config1.equals(config2)) &&
                (id == null ? d.id == null : id.equals(d.id)) &&
                mutable == d.mutable &&
                scaleFilter == d.scaleFilter &&
                queriesEquals(d);
    }

    @Override
    public BitmapDecoder filterBitmap(boolean filter) {
        scaleFilter = filter;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    BitmapLoader memCacheEnabled(boolean useCache) {
        this.memCacheEnabled = useCache;
        return this;
    }

    @Override
    public boolean isCancelled() {
        return opts.mCancel;
    }

    @Override
    public void decode(@NonNull DecodeResult out) {
        final boolean memCacheEnabled = this.memCacheEnabled && isMemCacheSupported();
        if (memCacheEnabled) {
            final Bitmap cachedBitmap = getCachedBitmap();
            if (cachedBitmap != null) {
                out.bitmap = cachedBitmap;
                out.cacheSource = CacheSource.MEMORY;
                return;
            }
        }

        out.bitmap = null;

        // reset

        opts.mCancel = false;
        adjustedDensityRatio = 0;

        //

        resolveRequests();

        // Setup sample size.

        final boolean postScaleBy = (ratioWidth != 1 || ratioHeight != 1);
        if (postScaleBy) {
            opts.inSampleSize = calculateInSampleSizeByRatio();
        } else {
            opts.inSampleSize = 1;
        }

        // Execute actual decoding

        if (opts.mCancel) return;

        Bitmap bitmap = executeDecoding();
        if (bitmap == null) return;

        // Scale it finally.

        if (postScaleBy) {
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            Bitmap bitmap2;

            Matrix m = MATRIX.obtain();
            m.setScale(adjustedWidthRatio, adjustedHeightRatio);
            bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                    scaleFilter);
            MATRIX.recycle(m);

            if (bitmap != bitmap2) {
                bitmap.recycle();
            }

            bitmap2.setDensity(opts.inTargetDensity);
            bitmap = bitmap2;
        }

        if (memCacheEnabled) {
            synchronized (sMemCacheLock) {
                if (sMemCache != null) {
                    Log.e("asdf", "cached ");
                    sMemCache.put(this, bitmap);
                }
            }
        }

        out.bitmap = bitmap;
        out.cacheSource = isFromDiskCache ? CacheSource.DISK : CacheSource.NOT_CACHED;
    }

    protected void setupLoadTask(BitmapLoadTask task, View v, ViewBinder<?> binder) {
        FramingAlgorithm framing = binder.framing();
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
