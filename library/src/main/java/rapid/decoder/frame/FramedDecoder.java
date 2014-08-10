package rapid.decoder.frame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapMeta;
import rapid.decoder.Decodable;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class FramedDecoder extends Decodable {
    private BitmapDecoder mDecoder;
    protected Drawable background;
    protected int frameWidth;
    protected int frameHeight;
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

    public abstract FramedDecoder mutate();

    protected abstract void getBounds(BitmapMeta meta, int frameWidth, int frameHeight,
                                      @Nullable Rect outSrc, @Nullable Rect outDest);

    private BitmapDecoder setRegion(BitmapDecoder decoder, BitmapMeta meta, int frameWidth,
                                    int frameHeight, Rect destRegion) {
        Rect region = RECT.obtainNotReset();
        getBounds(meta, frameWidth, frameHeight, region, destRegion);
        if (!(region.left == 0 && region.top == 0 && region.right == meta.width() && region
                .bottom == meta.height())) {

            decoder = decoder.mutate().region(region);
        }
        RECT.recycle(region);
        return decoder;
    }

    @Override
    public void draw(Canvas cv, Rect bounds) {
        setRegion(mDecoder, mDecoder, bounds.width(), bounds.height(), null).draw(cv, bounds);
    }

    @Override
    public Bitmap decode() {
        return decode(false);
    }

    private Bitmap decode(boolean fromCache) {
        Rect rectDest = RECT.obtainNotReset();
        BitmapDecoder decoder = null;
        Bitmap bitmap = null;
        if (fromCache) {
            BitmapMeta meta = mDecoder.getCachedMeta();
            if (meta != null) {
                decoder = setRegion(mDecoder, meta, frameWidth, frameHeight, rectDest);
                bitmap = decoder.getCachedBitmap();
            }
        } else {
            decoder = setRegion(mDecoder, mDecoder, frameWidth, frameHeight, rectDest);
            bitmap = decoder.createAndDraw(frameWidth, frameHeight, rectDest, background);
        }
        RECT.recycle(rectDest);
        if (decoder != null) {
            mCacheSource = decoder.cacheSource();
        }
        return bitmap;
    }

    @Override
    public Bitmap getCachedBitmap() {
        return decode(true);
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
