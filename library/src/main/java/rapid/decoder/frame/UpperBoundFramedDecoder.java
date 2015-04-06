package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapMeta;

public class UpperBoundFramedDecoder extends FramedDecoder {
    private FramedDecoder mFramedDecoder;
    private int mGravity;

    private UpperBoundFramedDecoder(FramedDecoder other, int gravity) {
        super(other);
        mFramedDecoder = other;
        mGravity = gravity;
    }

    protected UpperBoundFramedDecoder(FramedDecoder other) {
        super(other);
    }

    @Override
    public FramedDecoder fork() {
        return new UpperBoundFramedDecoder(this);
    }

    @Override
    protected void getBounds(BitmapMeta meta, int frameWidth, int frameHeight, @Nullable Rect outSrc, @Nullable Rect outDest) {
        if (meta.width() > frameWidth || meta.height() > frameHeight) {
            mFramedDecoder.getBounds(meta, frameWidth, frameHeight, outSrc, outDest);
        } else {

        }
    }
}
