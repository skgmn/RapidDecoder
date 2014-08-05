package rapid.decoder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

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
import rapid.decoder.compat.DisplayCompat;
import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramedDecoder;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class BitmapDecoder extends Decodable {
    static final String MESSAGE_INVALID_RATIO = "Ratio should be positive.";

    private static final String MESSAGE_INVALID_URI = "Invalid uri: %s";
    private static final String MESSAGE_PACKAGE_NOT_FOUND = "Package not found: %s";
    private static final String MESSAGE_RESOURCE_NOT_FOUND = "Resource not found: %s";
    private static final String MESSAGE_UNSUPPORTED_SCHEME = "Unsupported scheme: %s";
    private static final String MESSAGE_URI_REQUIRES_CONTEXT = "This type of uri requires Context" +
            ". Use BitmapDecoder.from(Context, Uri) instead.";

    protected static final int HASHCODE_NULL_REGION = 0x09DF79A9;
    protected static final int HASHCODE_NULL_BITMAP_OPTIONS = 0x00F590B9;

    //
    // Cache
    //

    private static final long DEFAULT_CACHE_SIZE = 4 * 1024 * 1024; // 4MB

    protected static final Object sMemCacheLock = new Object();
    protected static BitmapLruCache<Object> sMemCache;

    static final Object sDiskCacheLock = new Object();
    static DiskLruCache sDiskCache;

    @SuppressWarnings("UnusedDeclaration")
    public static void initMemoryCache(Context context) {
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();

        Point size = POINT.obtainNotReset();
        DisplayCompat.getSize(display, size);

        final Config defaultConfig = Build.VERSION.SDK_INT < 9 ? Config.RGB_565 : Config.ARGB_8888;
        initMemoryCache(BitmapUtils.getByteCount(size.x, size.y, defaultConfig));

        POINT.recycle(size);
    }

    public static void initMemoryCache(int size) {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                sMemCache.evictAll();
            }
            sMemCache = new BitmapLruCache<Object>(size);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void destroyMemoryCache() {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                sMemCache.evictAll();
                sMemCache = null;
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void clearMemoryCache() {
        synchronized (sMemCacheLock) {
            if (sMemCache != null) {
                sMemCache.evictAll();
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
    // Queries
    //

    private static final int INITIAL_REQUEST_LIST_CAPACITY = 3;

    protected static class ScaleTo {
        public float width;
        public float height;

        public ScaleTo(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    protected static class ScaleBy {
        public float width;
        public float height;

        public ScaleBy(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    protected static class Region {
        public int left;
        public int top;
        public int right;
        public int bottom;

        public Region(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    protected ArrayList<Object> requests;
    private boolean requestsResolved = false;

    //
    // Async jobs
    //

    static BackgroundTaskManager sTaskManager = new BackgroundTaskManager();

    //
    //
    //

    protected float ratioWidth = 1;
    protected float ratioHeight = 1;
    protected Rect region;

    protected BitmapDecoder() {
    }

    protected BitmapDecoder(BitmapDecoder other) {
        if (other.requests != null) {
            requests = new ArrayList<Object>(INITIAL_REQUEST_LIST_CAPACITY);
            requests.addAll(other.requests);
        }
    }

    protected void addRequest(Object request) {
        if (requests == null) {
            requests = new ArrayList<Object>(INITIAL_REQUEST_LIST_CAPACITY);
        }
        requests.add(request);
    }

    /**
     * @return The width of the source image.
     */
    public abstract int sourceWidth();

    /**
     * @return The height of the source image.
     */
    public abstract int sourceHeight();

    /**
     * @return The estimated width of decoded image.
     */
    public int width() {
        resolveQueries();
        return (int) Math.ceil(
                (region == null ? sourceWidth() : region.width()) * ratioWidth);
    }

    /**
     * @return The estimated height of decoded image.
     */
    public int height() {
        resolveQueries();
        return (int) Math.ceil(
                (region == null ? sourceHeight() : region.height()) * ratioHeight);
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

        requestsResolved = false;

        Object lastRequest = (requests == null ? null : requests.get(requests.size() - 1));
        if (lastRequest != null) {
            if (lastRequest instanceof ScaleTo) {
                ScaleTo scaleTo = (ScaleTo) lastRequest;
                scaleTo.width = width;
                scaleTo.height = height;

                return this;
            } else if (lastRequest instanceof ScaleBy) {
                requests.remove(requests.size() - 1);
            }
        }

        addRequest(new ScaleTo(width, height));
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

        requestsResolved = false;

        Object lastRequest = (requests == null ? null : requests.get(requests.size() - 1));
        if (lastRequest != null) {
            if (lastRequest instanceof ScaleTo) {
                ScaleTo scaleTo = (ScaleTo) lastRequest;
                scaleTo.width = scaleTo.width * widthRatio;
                scaleTo.height = scaleTo.height * heightRatio;

                return this;
            } else if (lastRequest instanceof ScaleBy) {
                ScaleBy scaleBy = (ScaleBy) lastRequest;
                scaleBy.width *= widthRatio;
                scaleBy.height *= heightRatio;

                return this;
            }
        }

        addRequest(new ScaleBy(widthRatio, heightRatio));
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

        requestsResolved = false;

        Object lastRequest = (requests == null ? null : requests.get(requests.size() - 1));
        if (lastRequest != null) {
            if (lastRequest instanceof Region) {
                Region region = (Region) lastRequest;
                region.left += left;
                region.top += top;
                region.right = region.left + (right - left);
                region.bottom = region.top + (bottom - top);

                return this;
            }
        }

        addRequest(new Region(left, top, right, bottom));
        return this;
    }

    /**
     * Equivalent to <code>region(region.left, region.top, region.right, region.bottom)</code>.
     */
    @SuppressWarnings("UnusedDeclaration")
    public BitmapDecoder region(Rect region) {
        return region(region.left, region.top, region.right, region.bottom);
    }

    /**
     * Directly draw the image to canvas without any unnecessary scaling.
     */
    public abstract void draw(Canvas cv, Rect rectDest);

    /**
     * Set preferred bitmap configuration.
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract BitmapDecoder config(Config config);

    @SuppressWarnings("UnusedDeclaration")
    public abstract Config config();

    public abstract Bitmap createAndDraw(int width, int height, @NonNull Rect rectDest,
                                         @Nullable Drawable background);

    /**
     * Tell the decoder to either force using built-in decoder or not.
     *
     * @param force true if it should always use built-in decoder.
     */
    public abstract BitmapDecoder useBuiltInDecoder(boolean force);

    @SuppressWarnings("UnusedDeclaration")
    public abstract BitmapDecoder filterBitmap(boolean filter);

    public abstract Bitmap decode();

    @NonNull
    @Override
    public abstract BitmapDecoder mutate();

    protected int regionWidth() {
        if (region != null) {
            return region.width();
        } else {
            return sourceWidth();
        }
    }

    protected int regionHeight() {
        if (region != null) {
            return region.height();
        } else {
            return sourceHeight();
        }
    }

    protected void resolveQueries() {
        if (requestsResolved) return;

        final float densityRatio = getDensityRatio();
        ratioWidth = ratioHeight = densityRatio;

        if (region != null) {
            RECT.recycle(region);
        }
        region = null;

        requestsResolved = true;
        if (requests == null) return;

        for (Object r : requests) {
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

                ratioWidth = targetWidth / w;
                ratioHeight = targetHeight / h;
            } else if (r instanceof ScaleBy) {
                ScaleBy scaleBy = (ScaleBy) r;

                ratioWidth *= scaleBy.width;
                ratioHeight *= scaleBy.height;
            } else if (r instanceof Region) {
                Region rr = (Region) r;

                if (region == null) {
                    final int left = Math.round(rr.left / ratioWidth);
                    final int top = Math.round(rr.top / ratioHeight);
                    final int right = Math.round(rr.right / ratioWidth);
                    final int bottom = Math.round(rr.bottom / ratioHeight);

                    region = RECT.obtainNotReset();

                    // Check boundaries
                    region.left = Math.max(0, Math.min(left, sourceWidth()));
                    region.top = Math.max(0, Math.min(top, sourceHeight()));
                    region.right = Math.max(region.left, Math.min(right, sourceWidth()));
                    region.bottom = Math.max(region.top, Math.min(bottom, sourceHeight()));
                } else {
                    final int left = region.left + Math.round(rr.left / ratioWidth);
                    final int top = region.top + Math.round(rr.top / ratioHeight);
                    final int right = region.left + Math.round((rr.right - rr.left) / ratioWidth);
                    final int bottom = region.top + Math.round((rr.bottom - rr.top) / ratioHeight);

                    // Check boundaries
                    region.left = Math.max(0, Math.min(left, region.right));
                    region.top = Math.max(0, Math.min(top, region.bottom));
                    region.right = Math.max(region.left, Math.min(right, region.right));
                    region.bottom = Math.max(region.top, Math.min(bottom, region.bottom));
                }

                ratioWidth = (float) (rr.right - rr.left) / region.width();
                ratioHeight = (float) (rr.bottom - rr.top) / region.height();
            }
        }
    }

    protected boolean queriesEquals(BitmapDecoder other) {
        if (requests == null) {
            return other.requests == null || other.requests.isEmpty();
        } else {
            int otherSize = (other.requests == null ? 0 : other.requests.size());
            if (requests.size() != otherSize) return false;

            Iterator<Object> it1 = requests.iterator();
            Iterator<Object> it2 = other.requests.iterator();

            while (it1.hasNext()) {
                if (!it2.hasNext() || !it1.next().equals(it2.next())) return false;
            }

            return true;
        }
    }

    protected int queriesHash() {
        return (requests == null ? 0 : requests.hashCode());
    }

    /**
     * Request the decoder to cancel the decoding job currently working.
     * This should be called by another thread.
     */
    @SuppressWarnings("UnusedDeclaration")
    public abstract void cancel();

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
     * Equivalent to {@link #draw(Canvas, Rect)}.
     */
    public void draw(Canvas cv, int left, int top, int right, int bottom) {
        final Rect bounds = RECT.obtain(left, top, right, bottom);
        draw(cv, bounds);
        RECT.recycle(bounds);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void draw(Canvas cv, int left, int top) {
        draw(cv, left, top, left + width(), top + height());
    }

    /**
     * Equivalent to <code>mutable(true)</code>.
     */
    @SuppressWarnings("UnusedDeclaration")
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

    protected float getDensityRatio() {
        return 1f;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            RECT.recycle(region);
        } finally {
            super.finalize();
        }
    }
    
    //
    // Frame
    //

    public FramedDecoder frame(int frameWidth, int frameHeight, ImageView.ScaleType scaleType) {
        return FramedDecoder.newInstance(this, frameWidth, frameHeight, scaleType);
    }

    //
    // from()
    //

    public static BitmapDecoder from(byte[] data) {
        return new ByteArrayLoader(data, 0, data.length);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(byte[] data, int offset, int length) {
        return new ByteArrayLoader(data, offset, length);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(Resources res, int id) {
        return new ResourceLoader(res, id);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(String pathName) {
        return new FileLoader(pathName);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(FileDescriptor fd) {
        return new FileDescriptorLoader(fd);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(InputStream in) {
        return new StreamLoader(in);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(Uri uri) {
        return from(null, uri);
    }

    public static BitmapDecoder from(Context context, final Uri uri) {
        String scheme = uri.getScheme();

        if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
            if (context == null) {
                throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
            }

            List<String> segments = uri.getPathSegments();
            if (segments.size() != 2 || !segments.get(0).equals("drawable")) {
                throw new IllegalArgumentException(String.format(MESSAGE_INVALID_URI, uri));
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
                    throw new IllegalArgumentException(String.format(MESSAGE_PACKAGE_NOT_FOUND,
                            packageName));
                }
            }

            String resName = segments.get(1);
            int id = res.getIdentifier(resName, "drawable", packageName);
            if (id == 0) {
                throw new IllegalArgumentException(String.format(MESSAGE_RESOURCE_NOT_FOUND,
                        resName));
            }

            return new ResourceLoader(res, id);
        } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (context == null) {
                throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
            }

            try {
                StreamLoader d = new StreamLoader(context.getContentResolver().openInputStream
                        (uri));
                synchronized (sMemCacheLock) {
                    if (sMemCache != null) {
                        d.setMemCacheEnabler(new MemCacheEnabler<Uri>(uri));
                    }
                }
                return d;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            return new FileLoader(uri.getPath());
        } else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")) {
            String uriString = uri.toString();
            BitmapLoader d = null;

            synchronized (sDiskCacheLock) {
                if (sDiskCache != null) {
                    InputStream in = sDiskCache.get(uriString);
                    if (in != null) {
                        d = new StreamLoader(in);
                    }
                }

                if (d == null) {
                    StreamLoader sd = new StreamLoader(new LazyInputStream(new StreamOpener() {
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
                    if (sDiskCache != null) {
                        sd.setCacheOutputStream(sDiskCache.getOutputStream(uriString));
                    }

                    d = sd;
                }
            }

            synchronized (sMemCacheLock) {
                if (sMemCache != null) {
                    d.setMemCacheEnabler(new MemCacheEnabler<Uri>(uri));
                }
            }
            return d;
        } else {
            throw new IllegalArgumentException(String.format(MESSAGE_UNSUPPORTED_SCHEME, scheme));
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static BitmapDecoder from(@NonNull Bitmap bitmap) {
        return new BitmapTransformer(bitmap);
    }
}
