package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapMeta;

@SuppressWarnings("UnusedDeclaration")
class FitXYFramedDecoder extends FramedDecoder {
    public FitXYFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected FitXYFramedDecoder(FitXYFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(BitmapMeta meta, int frameWidth, int frameHeight,
                             @Nullable Rect outSrc, @Nullable Rect outDest) {
        if (outSrc != null) {
            outSrc.set(0, 0, meta.width(), meta.height());
        }
        if (outDest != null) {
            outDest.set(0, 0, frameWidth, frameHeight);
        }
    }

    @Override
    public FramedDecoder fork() {
        return new FitXYFramedDecoder(this);
    }
}
