package rapid.decoder.frame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.Decodable;

import static rapid.decoder.cache.ResourcePool.*;

public abstract class FramedDecoder implements Decodable {
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

    @SuppressWarnings("UnusedDeclaration")
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
}
