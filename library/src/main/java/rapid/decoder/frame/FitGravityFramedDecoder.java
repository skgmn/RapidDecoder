package rapid.decoder.frame;

import android.graphics.Rect;
import android.support.annotation.Nullable;

import rapid.decoder.BitmapDecoder;
import rapid.decoder.BitmapMeta;

@SuppressWarnings("UnusedDeclaration")
class FitGravityFramedDecoder extends FramedDecoder {
    public static final int GRAVITY_START = 0;
    public static final int GRAVITY_CENTER = 1;
    public static final int GRAVITY_END = 2;

    private int mGravity;

    public FitGravityFramedDecoder(BitmapDecoder decoder, int frameWidth, int frameHeight,
                                   int gravity) {

        super(decoder, frameWidth, frameHeight);
        mGravity = gravity;
    }

    protected FitGravityFramedDecoder(FitGravityFramedDecoder other) {
        super(other);
    }

    @Override
    protected void getBounds(BitmapMeta meta, int frameWidth, int frameHeight,
                             @Nullable Rect outSrc, @Nullable Rect outDest) {

        int width = meta.width();
        int height = meta.height();
        if (outSrc != null) {
            outSrc.set(0, 0, width, height);
        }
        if (outDest != null) {
            int targetWidth;
            int targetHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
            if (targetHeight <= frameHeight) {
                targetWidth = frameWidth;
            } else {
                targetWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                targetHeight = frameHeight;
            }
            switch (mGravity) {
                case GRAVITY_START:
                default:
                    outDest.set(0, 0, targetWidth, targetHeight);
                    break;

                case GRAVITY_CENTER:
                    outDest.left = (frameWidth - targetWidth) / 2;
                    outDest.top = (frameHeight - targetHeight) / 2;
                    outDest.right = outDest.left + targetWidth;
                    outDest.bottom = outDest.top + targetHeight;
                    break;

                case GRAVITY_END:
                    outDest.right = frameWidth;
                    outDest.bottom = frameHeight;
                    outDest.left = outDest.right - targetWidth;
                    outDest.top = outDest.bottom - targetHeight;
                    break;
            }
        }
    }

    @Override
    public FramedDecoder mutate() {
        return new FitGravityFramedDecoder(this);
    }
}
