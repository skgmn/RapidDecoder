package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;

@SuppressWarnings("UnusedDeclaration")
class MatrixFramedDecoder extends FramedDecoder {
    public MatrixFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected MatrixFramedDecoder(MatrixFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(int frameWidth, int frameHeight, @Nullable Rect outSrc,
                             @Nullable Rect outDest) {

        int width = Math.min(decoder.width(), frameWidth);
        int height = Math.min(decoder.height(), frameHeight);
        if (outSrc != null) {
            outSrc.set(0, 0, width, height);
        }
        if (outDest != null) {
            outDest.set(0, 0, width, height);
        }
    }

    @Override
    public FramedDecoder mutate() {
        return new MatrixFramedDecoder(this);
    }
}
