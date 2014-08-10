package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;

@SuppressWarnings("UnusedDeclaration")
class CenterInsideFramedDecoder extends FramedDecoder {
    public CenterInsideFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight) {
        super(decoder, frameWidth, frameHeight);
    }

    protected CenterInsideFramedDecoder(CenterInsideFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(int frameWidth, int frameHeight, @Nullable Rect outSrc,
                             @Nullable Rect outDest) {

        int width = mDecoder.width();
        int height = mDecoder.height();
        if (outSrc != null) {
            outSrc.set(0, 0, width, height);
        }

        if (outDest != null) {
            if (width <= frameWidth && height <= frameHeight) {
                outDest.left = (frameWidth - width) / 2;
                outDest.top = (frameHeight - height) / 2;
                outDest.right = outDest.left + width;
                outDest.bottom = outDest.top + height;
            } else {
                int targetWidth;
                int targetHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
                if (targetHeight <= frameHeight) {
                    targetWidth = frameWidth;
                } else {
                    targetWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                    targetHeight = frameHeight;
                }

                outDest.left = (frameWidth - targetWidth) / 2;
                outDest.top = (frameHeight - targetHeight) / 2;
                outDest.right = outDest.left + targetWidth;
                outDest.bottom = outDest.top + targetHeight;
            }
        }
    }

    @Override
    public FramedDecoder mutate() {
        return new CenterInsideFramedDecoder(this);
    }
}
