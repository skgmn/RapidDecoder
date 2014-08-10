package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;

@SuppressWarnings("UnusedDeclaration")
class FitXYFramedDecoder extends FramedDecoder {
    public FitXYFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected FitXYFramedDecoder(FitXYFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(int frameWidth, int frameHeight, @Nullable Rect outSrc,
                             @Nullable Rect outDest) {

        if (outSrc != null) {
            outSrc.set(0, 0, mDecoder.width(), mDecoder.height());
        }
        if (outDest != null) {
            outDest.set(0, 0, frameWidth, frameHeight);
        }
    }

    @Override
    public FramedDecoder mutate() {
        return new FitXYFramedDecoder(this);
    }
}
