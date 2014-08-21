package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapMeta;

@SuppressWarnings("UnusedDeclaration")
class CenterFramedDecoder extends FramedDecoder {
    public CenterFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected CenterFramedDecoder(CenterFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(BitmapMeta meta, int frameWidth, int frameHeight,
                             @Nullable Rect outSrc, @Nullable Rect outDest) {
        int width = meta.width();
        int height = meta.height();
        if (width > frameWidth) {
            if (outSrc != null) {
                outSrc.left = (width - frameWidth) / 2;
                outSrc.right = outSrc.left + frameWidth;
            }
            if (outDest != null) {
                outDest.left = 0;
                outDest.right = frameWidth;
            }
        } else {
            if (outSrc != null) {
                outSrc.left = 0;
                outSrc.right = width;
            }
            if (outDest != null) {
                outDest.left = (frameWidth - width) / 2;
                outDest.right = outDest.left + width;
            }
        }
        if (height > frameHeight) {
            if (outSrc != null) {
                outSrc.top = (height - frameHeight) / 2;
                outSrc.bottom = outSrc.top + frameHeight;
            }
            if (outDest != null) {
                outDest.top = 0;
                outDest.bottom = frameHeight;
            }
        } else {
            if (outSrc != null) {
                outSrc.top = 0;
                outSrc.bottom = height;
            }
            if (outDest != null) {
                outDest.top = (frameHeight - height) / 2;
                outDest.bottom = outDest.top + height;
            }
        }
    }

    @Override
    public FramedDecoder fork() {
        return new CenterFramedDecoder(this);
    }
}
