package rapid.decoder.frame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;
import rapid.decoder.cache.CacheSource;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class FramedDecoder extends Decodable {
    protected BitmapDecoder decoder;
    protected Drawable background;
    protected int frameWidth;
    protected int frameHeight;

    public FramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        this.decoder = decoder;
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
        setRegion(decoder, bounds.width(), bounds.height(), null).draw(cv, bounds);
    }

    @Override
    public Bitmap decode() {
        Rect rectDest = RECT.obtainNotReset();
        Bitmap bitmap = setRegion(decoder, frameWidth, frameHeight,
                rectDest).createAndDraw(frameWidth, frameHeight, rectDest, background);
        RECT.recycle(rectDest);
        return bitmap;
    }

    @Override
    public void cancel() {
        decoder.cancel();
    }

    @Override
    public void decode(@NonNull DecodeResult out) {
        // TODO Implement this
        out.bitmap = decode();
        out.cacheSource = CacheSource.NOT_CACHED;
    }

    @Override
    public boolean isCancelled() {
        return decoder.isCancelled();
    }

    // Factory methods

    public static FramedDecoder newInstance(BitmapDecoder decoder, int frameWidth,
                                            int frameHeight, ImageView.ScaleType scaleType) {

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
