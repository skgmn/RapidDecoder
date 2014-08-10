package rapid.decoder.frame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class FramedDecoder extends Decodable {
    protected BitmapDecoder mDecoder;
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

    protected abstract void getBounds(int frameWidth, int frameHeight, @Nullable Rect outSrc,
                                      @Nullable Rect outDest);

    private BitmapDecoder setRegion(BitmapDecoder decoder, int frameWidth, int frameHeight,
                                    Rect destRegion) {

        Rect region = RECT.obtainNotReset();
        getBounds(frameWidth, frameHeight, region, destRegion);
        if (!(region.left == 0 && region.top == 0 && region.right == decoder.width() && region
                .bottom == decoder.height())) {

            decoder = decoder.mutate().region(region);
        }
        RECT.recycle(region);
        return decoder;
    }

    @Override
    public void draw(Canvas cv, Rect bounds) {
        setRegion(mDecoder, bounds.width(), bounds.height(), null).draw(cv, bounds);
    }

    @Override
    public Bitmap decode() {
        return decode(false);
    }

    private Bitmap decode(boolean fromCache) {
        Rect rectDest = RECT.obtainNotReset();
        BitmapDecoder decoder = setRegion(mDecoder, frameWidth, frameHeight, rectDest);
        Bitmap bitmap;
        if (fromCache) {
            if (decoder.isMemoryCacheSupported()) {
                bitmap = decoder.getCachedBitmap();
            } else {
                bitmap = null;
            }
        } else {
            bitmap = decoder.createAndDraw(frameWidth, frameHeight, rectDest, background);
        }
        RECT.recycle(rectDest);
        mCacheSource = decoder.cacheSource();
        return bitmap;
    }

    @Override
    public Bitmap getCachedBitmap() {
        return decode(true);
    }

    @Override
    public boolean isMemoryCacheSupported() {
        return mDecoder.isMemoryCacheSupported();
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
