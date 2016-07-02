package rapid.decoder.frame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapMeta;
import rapid.decoder.Decodable;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.PAINT;
import static rapid.decoder.cache.ResourcePool.RECT;

public abstract class FramedDecoder extends Decodable {
    private BitmapDecoder mDecoder;
    protected Drawable background;
    protected final int frameWidth;
    protected final int frameHeight;
    protected CacheSource mCacheSource;

    public FramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        mDecoder = decoder;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    protected FramedDecoder(FramedDecoder other) {
        background = other.background;
        frameWidth = other.frameWidth;
        frameHeight = other.frameHeight;
    }

    public FramedDecoder background(Drawable d) {
        this.background = d;
        return this;
    }

    public abstract FramedDecoder fork();

    protected abstract void getBounds(BitmapMeta meta, int frameWidth, int frameHeight,
                                      @Nullable Rect outSrc, @Nullable Rect outDest);

    private BitmapDecoder setRegion(BitmapDecoder decoder, BitmapMeta meta, int frameWidth, int frameHeight, Rect
            destRegion) {
        Rect region = RECT.obtainDirty();
        getBounds(meta, frameWidth, frameHeight, region, destRegion);
        if (region.left != 0 || region.top != 0 || region.right != meta.width() || region.bottom != meta.height()) {
            decoder = decoder.fork().region(region);
        }
        RECT.recycle(region);
        return decoder;
    }

    @Override
    public void draw(Canvas cv, int left, int top) {
        setRegion(mDecoder, mDecoder, frameWidth, frameHeight, null).draw(cv, left, top);
    }

    @Nullable
    @Override
    protected Bitmap decode(boolean approximately) {
        return decodeImpl(false);
    }

    private Bitmap decodeImpl(boolean fromCache) {
        Rect rectDest = RECT.obtainDirty();
        BitmapDecoder decoder = null;
        Bitmap bitmap = null;
        if (fromCache) {
            BitmapMeta meta = mDecoder.getCachedMeta();
            if (meta != null) {
                decoder = setRegion(mDecoder, meta, frameWidth, frameHeight, rectDest);
                bitmap = decoder.getCachedBitmap(true);
            }
        } else {
            decoder = setRegion(mDecoder, mDecoder, frameWidth, frameHeight, rectDest);
            Bitmap decodedBitmap = decoder.decodeApproximately();
            if (decodedBitmap != null) {
                boolean filter = decoder.filterBitmap();
                if (rectDest.left == 0 && rectDest.top == 0 && rectDest.right == frameWidth && rectDest.bottom ==
                        frameHeight) {
                    bitmap = Bitmap.createScaledBitmap(decodedBitmap, frameWidth, frameHeight, filter);
                } else {
                    bitmap = Bitmap.createBitmap(frameWidth, frameHeight, decodedBitmap.getConfig());
                    Canvas canvas = new Canvas(bitmap);
                    if (background != null) {
                        background.setBounds(0, 0, frameWidth, frameHeight);
                        background.draw(canvas);
                    }
                    Paint paint = filter ? PAINT.obtain(Paint.FILTER_BITMAP_FLAG) : null;
                    canvas.drawBitmap(decodedBitmap, null, rectDest, paint);
                    PAINT.recycle(paint);
                }
            }
        }
        RECT.recycle(rectDest);
        if (decoder != null) {
            mCacheSource = decoder.cacheSource();
        }
        return bitmap;
    }

    @Override
    public Bitmap getCachedBitmap(boolean approximately) {
        return decodeImpl(true);
    }

    @Override
    public boolean isMemoryCacheEnabled() {
        return mDecoder.isMemoryCacheEnabled();
    }

    @Override
    public void cancel() {
        mDecoder.cancel();
    }

    @Override
    public boolean isCancelled() {
        return mDecoder.isCancelled();
    }

    @Override
    public int width() {
        return frameWidth;
    }

    @Override
    public int height() {
        return frameHeight;
    }

    @Override
    public String mimeType() {
        return mDecoder.mimeType();
    }

    @Override
    public CacheSource cacheSource() {
        return mCacheSource;
    }

    public static FramedDecoder newInstance(BitmapDecoder decoder, int frameWidth, int frameHeight,
                                            ImageView.ScaleType scaleType) {
        if (ImageView.ScaleType.MATRIX.equals(scaleType)) {
            return new MatrixFramedDecoder(decoder, frameWidth, frameHeight);
        } else if (ImageView.ScaleType.FIT_XY.equals(scaleType)) {
            return new FitXYFramedDecoder(decoder, frameWidth, frameHeight);
        } else if (ImageView.ScaleType.FIT_START.equals(scaleType)) {
            return new FitGravityFramedDecoder(decoder, frameWidth, frameHeight,
                    FitGravityFramedDecoder.GRAVITY_START);
        } else if (ImageView.ScaleType.FIT_CENTER.equals(scaleType)) {
            return new FitGravityFramedDecoder(decoder, frameWidth, frameHeight,
                    FitGravityFramedDecoder.GRAVITY_CENTER);
        } else if (ImageView.ScaleType.FIT_END.equals(scaleType)) {
            return new FitGravityFramedDecoder(decoder, frameWidth, frameHeight,
                    FitGravityFramedDecoder.GRAVITY_END);
        } else if (ImageView.ScaleType.CENTER.equals(scaleType)) {
            return new CenterFramedDecoder(decoder, frameWidth, frameHeight);
        } else if (ImageView.ScaleType.CENTER_CROP.equals(scaleType)) {
            return new CenterCropFramedDecoder(decoder, frameWidth, frameHeight);
        } else if (ImageView.ScaleType.CENTER_INSIDE.equals(scaleType)) {
            return new CenterInsideFramedDecoder(decoder, frameWidth, frameHeight);
        } else {
            throw new IllegalArgumentException("scaleType");
        }
    }
}
