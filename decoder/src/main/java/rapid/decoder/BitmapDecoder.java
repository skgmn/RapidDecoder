package rapid.decoder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rapid.decoder.cache.BitmapLruCache;
import rapid.decoder.cache.DiskLruCache;
import rapid.decoder.cache.ResourcePool;
import rapiddecoder.compat.DisplayCompat;
import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramedDecoder;
import rapid.decoder.frame.FramingMethod;
import rapiddecoder.util.BitmapUtils;

import static rapid.decoder.cache.ResourcePool.POINT;
import static rapid.decoder.cache.ResourcePool.RECT;

public abstract class BitmapDecoder extends Decodable {
    static final String MESSAGE_INVALID_RATIO = "Ratio should be positive.";
    private static final String MESSAGE_URI_REQUIRES_CONTEXT = "This type of uri requires Context" +
            ". Use BitmapDecoder.from(Context, Uri) instead.";

    private static final String ASSET_PATH_PREFIX = "/android_asset/";

    //
    // Cache
    //

    private static final long DEFAULT_CACHE_SIZE = 8 * 1024 * 1024;

    protected static final Object sMemCacheLock = new Object();
    protected static BitmapLruCache sMemCache;

    static final Object sDiskCacheLock = new Object();
    static DiskLruCache sDiskCache;

    public static void initMemoryCache(Context context) {
        initMemoryCache(context, 2);
    }

    public static void initMemoryCache(Context context, float factor) {
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();

        Point size = POINT.obtainDirty();
        DisplayCompat.getSize(display, size);

        final Config defaultConfig = Build.VERSION.SDK_INT < 9 ? Config.RGB_565 : Config.ARGB_8888;
        initMemoryCache((int) (factor * BitmapUtils.getByteCount(size.x, size.y, defaultConfig)));

        POINT.recycle(size);
    }

    public static void initMemoryCache(int size) {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                try {
                    sMemCache.evictAll();
                } catch (IllegalStateException ignored) {
                }
            }
            sMemCache = new BitmapLruCache(size);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void destroyMemoryCache() {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                try {
                    sMemCache.evictAll();
                } catch (IllegalStateException ignored) {
                }
                sMemCache = null;
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void clearMemoryCache() {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                try {
                    sMemCache.evictAll();
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void initDiskCache(Context context) {
        initDiskCache(context, DEFAULT_CACHE_SIZE);
    }

    public static void initDiskCache(Context context, long size) {
        synchronized (sDiskCacheLock) {
            sDiskCache = new DiskLruCache(context, "agu", size);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void destroyDiskCache() {
        synchronized (sDiskCacheLock) {
            if (sDiskCache != null) {
                sDiskCache.close();
                sDiskCache = null;
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void clearDiskCache() {
        synchronized (sDiskCacheLock) {
            if (sDiskCache != null) {
                sDiskCache.clear();
            }
        }
    }

    //
    // Transformations
    //

    private static final int INITIAL_TRANSFORMATION_LIST_CAPACITY = 2;

    private static abstract class Transformation {
        public abstract void recycle();
    }

    protected static class ScaleTo extends Transformation {
        private static ResourcePool<ScaleTo> POOL = new ResourcePool<ScaleTo>() {
            @Override
            protected ScaleTo newInstance() {
                return new ScaleTo();
            }
        };

        public float width;
        public float height;

        ScaleTo() {
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits(width) + 31 * Float.floatToIntBits(height);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ScaleTo)) return false;
            ScaleTo scaleTo = (ScaleTo) o;
            return width == scaleTo.width && height == scaleTo.height;
        }

        public static ScaleTo obtain(float width, float height) {
            ScaleTo scaleTo = POOL.obtainDirty();
            scaleTo.width = width;
            scaleTo.height = height;
            return scaleTo;
        }

        @Override
        public void recycle() {
            POOL.recycle(this);
        }
    }

    protected static class ScaleBy extends Transformation {
        private static ResourcePool<ScaleBy> POOL = new ResourcePool<ScaleBy>() {
            @Override
            protected ScaleBy newInstance() {
                return new ScaleBy();
            }
        };

        public float width;
        public float height;

        ScaleBy() {
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits(width) + 31 * Float.floatToIntBits(height);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ScaleBy)) return false;
            ScaleBy scaleBy = (ScaleBy) o;
            return width == scaleBy.width && height == scaleBy.height;
        }

        public static ScaleBy obtain(float width, float height) {
            ScaleBy scaleBy = POOL.obtainDirty();
            scaleBy.width = width;
            scaleBy.height = height;
            return scaleBy;
        }

        @Override
        public void recycle() {
            POOL.recycle(this);
        }
    }

    protected static class Region extends Transformation {
        private static ResourcePool<Region> POOL = new ResourcePool<Region>() {
            @Override
            protected Region newInstance() {
                return new Region();
            }
        };

        public int left;
        public int top;
        public int right;
        public int bottom;

        Region() {
        }

        @Override
        public int hashCode() {
            return left + 31 * (top + 31 * (right + 31 * bottom));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Region)) return false;
            Region region = (Region) o;
            return left == region.left && top == region.top && right == region.right && bottom ==
                    region.bottom;
        }

        public static Region obtain(int left, int top, int right, int bottom) {
            Region region = POOL.obtainDirty();
            region.left = left;
            region.top = top;
            region.right = right;
            region.bottom = bottom;
            return region;
        }

        @Override
        public void recycle() {
            POOL.recycle(this);
        }
    }

    protected ArrayList<Transformation> mTransformations;
    private boolean mTransformationsResolved = false;

    //
    // Fields
    //

    protected float mRatioWidth = 1;
    protected float mRatioHeight = 1;
    protected IntegerMaker mRatioIntegerMode;
    protected Rect mRegion;
    protected int mHashCode;
    private BitmapPostProcessor mPostProcessor;
    private int mWidth;
    private int mHeight;

    protected BitmapDecoder() {
    }

    protected BitmapDecoder(BitmapDecoder other) {
        if (other.mTransformations != null) {
            //noinspection unchecked
            mTransformations = new ArrayList<>(other.mTransformations);
        }
    }

    protected void addTransformation(Transformation transformation) {
        if (mTransformations == null) {
            mTransformations = new ArrayList<>(INITIAL_TRANSFORMATION_LIST_CAPACITY);
        }
        mTransformations.add(transformation);
        mHashCode = 0;
    }

    /**
     * @return The width of the source image.
     */
    public abstract int sourceWidth();

    /**
     * @return The height of the source image.
     */
    public abstract int sourceHeight();

    public abstract String mimeType();

    /**
     * @return The estimated width of decoded image.
     */
    public int width() {
        if (mWidth != 0) {
            return mWidth;
        }
        resolveTransformations();
        return mWidth = mRatioIntegerMode.toInteger(regionWidth() * mRatioWidth);
    }

    /**
     * @return The estimated height of decoded image.
     */
    public int height() {
        if (mHeight != 0) {
            return mHeight;
        }
        resolveTransformations();
        return mHeight = mRatioIntegerMode.toInteger(regionHeight() * mRatioHeight);
    }

    private void invalidateTransformations() {
        mTransformationsResolved = false;
        mWidth = mHeight = 0;
    }

    /**
     * <p>Request the decoder to scale the image to the specific dimension while decoding.
     * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize}
     * internally,
     * so you don't need to be concerned about it.</p>
     * <p>It uses built-in decoder when scaleFilter is false.</p>
     *
     * @param width  A desired width to be scaled to.
     * @param height A desired height to be scaled to.
     */
    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder scale(int width, int height) {
        if (width < 0) {
            throw new IllegalArgumentException("Invalid width");
        }
        if (height < 0) {
            throw new IllegalArgumentException("Invalid height");
        }
        if (width == 0 && height == 0) {
            throw new IllegalArgumentException();
        }

        invalidateTransformations();

        Object lastTransformation = (mTransformations == null ? null :
                mTransformations.get(mTransformations.size() - 1));
        if (lastTransformation != null) {
            if (lastTransformation instanceof ScaleTo) {
                ScaleTo scaleTo = (ScaleTo) lastTransformation;
                scaleTo.width = width;
                scaleTo.height = height;

                return this;
            } else if (lastTransformation instanceof ScaleBy) {
                mTransformations.remove(mTransformations.size() - 1);
            }
        }

        addTransformation(ScaleTo.obtain(width, height));
        return this;
    }

    /**
     * <p>Request the decoder to scale the image by the specific ratio while decoding.
     * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize}
     * internally,
     * so you don't need to be concerned about it.</p>
     * <p>It uses built-in decoder when scaleFilter is false.</p>
     *
     * @param widthRatio  Scale ratio of width.
     * @param heightRatio Scale ratio of height.
     */
    public BitmapDecoder scaleBy(float widthRatio, float heightRatio) {
        if (widthRatio <= 0 || heightRatio <= 0) {
            throw new IllegalArgumentException(MESSAGE_INVALID_RATIO);
        }

        invalidateTransformations();

        Object lastTransformation = (mTransformations == null ? null :
                mTransformations.get(mTransformations.size() - 1));
        if (lastTransformation != null) {
            if (lastTransformation instanceof ScaleTo) {
                ScaleTo scaleTo = (ScaleTo) lastTransformation;
                scaleTo.width = scaleTo.width * widthRatio;
                scaleTo.height = scaleTo.height * heightRatio;

                return this;
            } else if (lastTransformation instanceof ScaleBy) {
                ScaleBy scaleBy = (ScaleBy) lastTransformation;
                scaleBy.width *= widthRatio;
                scaleBy.height *= heightRatio;

                return this;
            }
        }

        addTransformation(ScaleBy.obtain(widthRatio, heightRatio));
        return this;
    }

    /**
     * <p>Request the decoder to crop the image while decoding.
     * Decoded image will be the same as an image which is cropped after decoding.</p>
     * <p>It uses {@link BitmapRegionDecoder} on API level 10 or higher,
     * otherwise it uses built-in decoder.</p>
     */
    public BitmapDecoder region(int left, int top, int right, int bottom) {
        if (right < left || bottom < top) {
            throw new IllegalArgumentException();
        }

        invalidateTransformations();

        Object lastTransformation = (mTransformations == null ? null :
                mTransformations.get(mTransformations.size() - 1));
        if (lastTransformation != null) {
            if (lastTransformation instanceof Region) {
                Region region = (Region) lastTransformation;
                region.left += left;
                region.top += top;
                region.right = region.left + (right - left);
                region.bottom = region.top + (bottom - top);

                return this;
            }
        }

        addTransformation(Region.obtain(left, top, right, bottom));
        return this;
    }

    /**
     * Equivalent to <code>region(region.left, region.top, region.right, region.bottom)</code>.
     */
    public BitmapDecoder region(@NonNull Rect region) {
        return region(region.left, region.top, region.right, region.bottom);
    }

    /**
     * Set preferred bitmap configuration.
     */
    public abstract BitmapDecoder config(Config config);

    public abstract Config config();

    /**
     * Tell the decoder to either force using built-in decoder or not.
     *
     * @param force true if it should always use built-in decoder.
     */
    public abstract BitmapDecoder useBuiltInDecoder(boolean force);

    public abstract BitmapDecoder filterBitmap(boolean filter);

    public abstract boolean filterBitmap();

    @NonNull
    @Override
    public abstract BitmapDecoder fork();

    protected int regionWidth() {
        if (mRegion != null) {
            return mRegion.width();
        } else {
            return sourceWidth();
        }
    }

    protected int regionHeight() {
        if (mRegion != null) {
            return mRegion.height();
        } else {
            return sourceHeight();
        }
    }

    protected void resolveTransformations() {
        if (mTransformationsResolved) return;

        final float densityRatio = densityRatio();
        mRatioWidth = mRatioHeight = densityRatio;
        mRatioIntegerMode = IntegerMaker.CEIL;

        if (mRegion != null) {
            RECT.recycle(mRegion);
        }
        mRegion = null;

        mTransformationsResolved = true;
        if (mTransformations == null) return;

        for (Object r : mTransformations) {
            if (r instanceof ScaleTo) {
                ScaleTo scaleTo = (ScaleTo) r;

                int w = regionWidth();
                int h = regionHeight();

                float targetWidth, targetHeight;
                if (scaleTo.width == 0) {
                    targetHeight = scaleTo.height;
                    targetWidth = AspectRatioCalculator.getWidth(w, h, targetHeight);
                } else if (scaleTo.height == 0) {
                    targetWidth = scaleTo.width;
                    targetHeight = AspectRatioCalculator.getHeight(w, h, targetWidth);
                } else {
                    targetWidth = scaleTo.width;
                    targetHeight = scaleTo.height;
                }

                mRatioWidth = targetWidth / w;
                mRatioHeight = targetHeight / h;
                mRatioIntegerMode = IntegerMaker.ROUND;
            } else if (r instanceof ScaleBy) {
                ScaleBy scaleBy = (ScaleBy) r;

                mRatioWidth *= scaleBy.width;
                mRatioHeight *= scaleBy.height;
            } else if (r instanceof Region) {
                Region rr = (Region) r;

                if (mRegion == null) {
                    mRegion = RECT.obtainDirty();
                    mRegion.left = Math.round(rr.left / mRatioWidth);
                    mRegion.top = Math.round(rr.top / mRatioHeight);
                    mRegion.right = Math.round(rr.right / mRatioWidth);
                    mRegion.bottom = Math.round(rr.bottom / mRatioHeight);
                } else {
                    mRegion.left = mRegion.left + Math.round(rr.left / mRatioWidth);
                    mRegion.top = mRegion.top + Math.round(rr.top / mRatioHeight);
                    mRegion.right = mRegion.left + Math.round((rr.right - rr.left) / mRatioWidth);
                    mRegion.bottom = mRegion.top + Math.round((rr.bottom - rr.top) / mRatioHeight);
                }

                mRatioWidth = (float) (rr.right - rr.left) / mRegion.width();
                mRatioHeight = (float) (rr.bottom - rr.top) / mRegion.height();
                mRatioIntegerMode = IntegerMaker.ROUND;
            }
        }
    }

    protected boolean transformationsEqual(BitmapDecoder other) {
        if (mTransformations == null) {
            return other.mTransformations == null || other.mTransformations.isEmpty();
        } else {
            int otherSize = (other.mTransformations == null ? 0 : other.mTransformations.size());
            if (mTransformations.size() != otherSize) return false;

            Iterator<Transformation> it1 = mTransformations.iterator();
            Iterator<Transformation> it2 = other.mTransformations.iterator();

            while (it1.hasNext()) {
                if (!it2.hasNext() || !it1.next().equals(it2.next())) return false;
            }

            return true;
        }
    }

    protected int transformationsHash() {
        return (mTransformations == null ? 0 : mTransformations.hashCode());
    }

    /**
     * <p>Tell the decoder whether decoded image should be mutable or not.</p>
     * <p>It sets {@link BitmapFactory.Options#inMutable} to true on API level 11 or higher,
     * otherwise it uses built-in decoder which always returns mutable bitmap.</p>
     *
     * @param mutable true if decoded image should be mutable.
     */
    public abstract BitmapDecoder mutable(boolean mutable);

    //
    // Shortcuts
    //

    /**
     * Equivalent to <code>useBuiltInDecoder(true)</code>.
     */
    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder useBuiltInDecoder() {
        return useBuiltInDecoder(true);
    }

    /**
     * Equivalent to <code>mutable(true)</code>.
     */
    public BitmapDecoder mutable() {
        return mutable(true);
    }

    /**
     * Equivalent to <code>scaleBy(ratio, ratio)</code>.
     */
    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder scaleBy(float ratio) {
        return scaleBy(ratio, ratio);
    }

    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder region(int width, int height) {
        return region(0, 0, width, height);
    }

    protected float densityRatio() {
        return 1f;
    }

    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder postProcessor(BitmapPostProcessor processor) {
        mPostProcessor = processor;
        return this;
    }

    protected Bitmap postProcess(Bitmap bitmap) {
        if (mPostProcessor != null) {
            Bitmap bitmap2 = mPostProcessor.process(bitmap);
            if (bitmap2 != bitmap) {
                bitmap.recycle();
            }
            return bitmap2;
        } else {
            return bitmap;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            RECT.recycle(mRegion);
            if (mTransformations != null) {
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0, c = mTransformations.size(); i < c; ++i) {
                    mTransformations.get(i).recycle();
                }
            }
        } finally {
            super.finalize();
        }
    }

    public BitmapDecoder reset() {
        mRatioWidth = mRatioHeight = 1;
        mTransformationsResolved = false;
        mRegion = null;
        mHashCode = 0;
        mTransformations = null;
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static boolean isLoadingInBackground() {
        return BackgroundTaskManager.hasAnyTasks();
    }

    public static boolean cancel(Object key) {
        if (BackgroundTaskManager.shouldBeWeak(key)) {
            return BackgroundTaskManager.cancelWeak(key);
        } else {
            return BackgroundTaskManager.cancelStrong(key);
        }
    }

    //
    // Frame
    //

    public FramedDecoder frame(int frameWidth, int frameHeight, ImageView.ScaleType scaleType) {
        return FramedDecoder.newInstance(this, frameWidth, frameHeight, scaleType);
    }

    public FramedDecoder frame(int frameWidth, int frameHeight, FramingMethod framing) {
        return framing.createFramedDecoder(this, frameWidth, frameHeight);
    }

    //
    // from()
    //

    public static BitmapLoader from(byte[] data) {
        return new ByteArrayBitmapLoader(data, 0, data.length);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapLoader from(byte[] data, int offset, int length) {
        return new ByteArrayBitmapLoader(data, offset, length);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapLoader from(Resources res, int id) {
        return new ResourceBitmapLoader(res, id);
    }

    public static BitmapLoader from(@NonNull String uriOrPath) {
        return from(uriOrPath, true);
    }

    public static BitmapLoader from(@NonNull String uriOrPath, boolean useCache) {
        if (uriOrPath.contains("://")) {
            return from(Uri.parse(uriOrPath), useCache);
        } else {
            return new FileBitmapLoader(uriOrPath).useMemoryCache(useCache);
        }
    }

    public static BitmapLoader from(File file) {
        return new FileBitmapLoader(file.getAbsolutePath());
    }

    public static BitmapLoader from(Context context, @NonNull String uri) {
        return from(context, Uri.parse(uri));
    }

    public static BitmapLoader from(Context context, String uri, boolean useCache) {
        return from(context, Uri.parse(uri), useCache);
    }

    public static BitmapLoader from(FileDescriptor fd) {
        return new FileDescriptorBitmapLoader(fd);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapLoader from(InputStream in) {
        return new StreamBitmapLoader(in);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapLoader from(@NonNull Uri uri) {
        return from(null, uri, true);
    }

    public static BitmapLoader from(@NonNull Uri uri, boolean useCache) {
        return from(null, uri, useCache);
    }

    public static BitmapLoader from(Context context, @NonNull final Uri uri) {
        return from(context, uri, true);
    }

    public static BitmapLoader from(final Context context, @NonNull final Uri uri,
                                    boolean useCache) {
        String scheme = uri.getScheme();
        switch (scheme) {
            case ContentResolver.SCHEME_ANDROID_RESOURCE:
                if (context == null) {
                    throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
                }

                Resources res;
                String packageName = uri.getAuthority();
                if (context.getPackageName().equals(packageName)) {
                    res = context.getResources();
                } else {
                    PackageManager pm = context.getPackageManager();
                    try {
                        res = pm.getResourcesForApplication(packageName);
                    } catch (NameNotFoundException e) {
                        return new NullBitmapLoader();
                    }
                }

                int id = 0;
                List<String> segments = uri.getPathSegments();
                int size = segments.size();
                if (size == 2 && segments.get(0).equals("drawable")) {
                    String resName = segments.get(1);
                    id = res.getIdentifier(resName, "drawable", packageName);
                } else if (size == 1 && TextUtils.isDigitsOnly(segments.get(0))) {
                    try {
                        id = Integer.parseInt(segments.get(0));
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (id == 0) {
                    return new NullBitmapLoader();
                } else {
                    return new ResourceBitmapLoader(res, id).useMemoryCache(useCache);
                }
            case ContentResolver.SCHEME_FILE:
                String path = uri.getPath();
                if (path.startsWith(ASSET_PATH_PREFIX)) {
                    if (context == null) {
                        throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
                    }
                    return new AssetBitmapLoader(context,
                            path.substring(ASSET_PATH_PREFIX.length())).id(uri).useMemoryCache(useCache);
                } else {
                    return new FileBitmapLoader(path).useMemoryCache(useCache);
                }
            case "http":
            case "https":
            case "ftp": {
                String uriString = uri.toString();
                BitmapLoader d = null;

                synchronized (sDiskCacheLock) {
                    if (useCache && sDiskCache != null) {
                        InputStream in = sDiskCache.get(uriString);
                        if (in != null) {
                            d = new StreamBitmapLoader(in);
                            d.mIsFromDiskCache = true;
                        }
                    }

                    if (d == null) {
                        StreamBitmapLoader sd = new StreamBitmapLoader(new LazyInputStream(new StreamOpener() {
                            @Override
                            public InputStream openInputStream() {
                                try {
                                    return new URL(uri.toString()).openStream();
                                } catch (MalformedURLException e) {
                                    throw new IllegalArgumentException(e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }));
                        if (useCache && sDiskCache != null) {
                            sd.setCacheOutputStream(sDiskCache.getOutputStream(uriString));
                        }
                        d = sd;
                    }
                }
                return d.id(uri).useMemoryCache(useCache);
            }
            default: {
                if (context == null) {
                    throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
                }
                final ContentResolver cr = context.getContentResolver();
                StreamBitmapLoader d = new StreamBitmapLoader(new LazyInputStream(new StreamOpener() {
                    @Nullable
                    @Override
                    public InputStream openInputStream() {
                        try {
                            return cr.openInputStream(uri);
                        } catch (FileNotFoundException e) {
                            return null;
                        }
                    }
                }));
                return d.id(uri).useMemoryCache(useCache);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(@NonNull Bitmap bitmap) {
        return new BitmapTransformer(bitmap);
    }

    public static BitmapLoader from(Context context, Uri uri, String[] projection) {
        return from(context, uri, projection, null, null, null);
    }

    public static BitmapLoader from(final Context context, final Uri uri, final String[] projection,
                                    final String selection, final String[] selectionArgs,
                                    final String sortOrder) {
        return new StreamBitmapLoader(new LazyInputStream(new StreamOpener() {
            @Override
            public InputStream openInputStream() {
                Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                        selectionArgs, sortOrder);
                if (cursor == null) {
                    return null;
                }
                try {
                    if (!cursor.moveToNext()) {
                        return null;
                    }
                    byte[] bytes = cursor.getBlob(0);
                    if (bytes == null) {
                        return null;
                    }
                    return new ByteArrayInputStream(bytes);
                } finally {
                    cursor.close();
                }
            }
        })).id(uri);
    }
}
