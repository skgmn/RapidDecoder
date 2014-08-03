package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;

@SuppressWarnings("UnusedDeclaration")
class CenterCropFramedDecoder extends FramedDecoder {
    public CenterCropFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected CenterCropFramedDecoder(CenterCropFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(int frameWidth, int frameHeight, @Nullable Rect outSrc,
                             @Nullable Rect outDest) {

        int width = decoder.width();
        int height = decoder.height();

        int targetWidth;
        int targetHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
        if (targetHeight >= frameHeight) {
            targetWidth = frameWidth;
        } else {
            targetWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
            targetHeight = frameHeight;
        }

        int targetLeft = (frameWidth - targetWidth) / 2;
        int targetTop = (frameHeight - targetHeight) / 2;

        float ratioWidth = (float) targetWidth / width;
        float ratioHeight = (float) targetHeight / height;

        if (outSrc != null) {
            outSrc.left = Math.round(-targetLeft / ratioWidth);
            outSrc.top = Math.round(-targetTop / ratioHeight);
            outSrc.right = outSrc.left + Math.round(frameWidth / ratioWidth);
            outSrc.bottom = outSrc.top + Math.round(frameHeight / ratioHeight);
        }
        if (outDest != null) {
            outDest.set(0, 0, frameWidth, frameHeight);
        }
    }

    @Override
    public FramedDecoder mutate() {
        return new CenterCropFramedDecoder(this);
    }
}
