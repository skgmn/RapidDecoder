package rapid.decoder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import rapid.decoder.binder.ViewBinder;
import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramedDecoder;
import rapid.decoder.frame.FramingMethod;

class ViewFrameBuilder {
    private static final int AUTOSIZE_NONE = 0;
    private static final int AUTOSIZE_WIDTH = 1;
    private static final int AUTOSIZE_HEIGHT = 2;
    private static final int AUTOSIZE_BOTH = 3;

    private BitmapDecoder mDecoder;
    private ViewBinder<?> mViewBinder;
    private FramingMethod mFraming;
    private int mAutoSizeMode = AUTOSIZE_NONE;
    private int mMinWidth;
    private int mMinHeight;
    private int mMaxWidth;
    private int mMaxHeight;
    private FramedDecoder mFramedDecoder;

    public ViewFrameBuilder(@NonNull BitmapDecoder decoder, @NonNull ViewBinder<?> binder,
                            @NonNull FramingMethod framing) {
        mDecoder = decoder;
        mViewBinder = binder;
        mFraming = framing;
    }

    public void prepareFraming() {
        if (mViewBinder.getLayoutWidth() == ViewGroup.LayoutParams.WRAP_CONTENT) {
            if (mViewBinder.getLayoutHeight() == ViewGroup.LayoutParams.WRAP_CONTENT) {
                mAutoSizeMode = AUTOSIZE_BOTH;
                mMinWidth = mViewBinder.getMinWidth();
                mMaxWidth = mViewBinder.getMaxWidth();
                mMinHeight = mViewBinder.getMinHeight();
                mMaxHeight = mViewBinder.getMaxHeight();
            } else {
                mAutoSizeMode = AUTOSIZE_WIDTH;
                mMinWidth = mViewBinder.getMinWidth();
                mMaxWidth = mViewBinder.getMaxWidth();
                mMinHeight = mViewBinder.getHeight();
            }
        } else if (mViewBinder.getLayoutHeight() == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mAutoSizeMode = AUTOSIZE_HEIGHT;
            mMinWidth = mViewBinder.getWidth();
            mMinHeight = mViewBinder.getMinHeight();
            mMaxHeight = mViewBinder.getMaxHeight();
        } else {
            mAutoSizeMode = AUTOSIZE_NONE;
            mMinWidth = mViewBinder.getWidth();
            mMinHeight = mViewBinder.getHeight();
        }
    }

    @Nullable
    public FramedDecoder getFramedDecoder(boolean fromCache) {
        if (mFramedDecoder != null) {
            return mFramedDecoder;
        }

        int frameWidth, frameHeight;
        if (mAutoSizeMode == AUTOSIZE_NONE) {
            frameWidth = mMinWidth;
            frameHeight = mMinHeight;
        } else {
            int width, height;

            BitmapMeta meta = mDecoder.getCachedMeta();
            if (meta == null) {
                if (fromCache) {
                    return null;
                } else {
                    width = mDecoder.width();
                    height = mDecoder.height();
                }
            } else {
                width = meta.width();
                height = meta.height();
            }

            switch (mAutoSizeMode) {
                case AUTOSIZE_WIDTH:
                    frameHeight = mMinHeight;
                    frameWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                    frameWidth = Math.max(mMinWidth, Math.min(frameWidth, mMaxWidth));
                    break;

                case AUTOSIZE_HEIGHT:
                    frameWidth = mMinWidth;
                    frameHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
                    frameHeight = Math.max(mMinHeight, Math.min(frameHeight, mMaxHeight));
                    break;

                case AUTOSIZE_BOTH:
                    frameWidth = Math.max(mMinWidth, Math.min(width, mMaxWidth));
                    frameHeight = Math.max(mMinHeight, Math.min(height, mMaxHeight));

                    for (int j = Math.min(width, mMaxWidth); j >= mMinWidth; --j) {
                        int i = AspectRatioCalculator.getHeight(width, height, j);
                        if (i >= mMinHeight && i <= mMaxHeight) {
                            frameWidth = j;
                            frameHeight = i;
                            break;
                        }
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        if (frameWidth != 0 && frameHeight != 0) {
            mFramedDecoder = mFraming.createFramedDecoder(mDecoder, frameWidth, frameHeight);
            return mFramedDecoder;
        } else {
            return null;
        }
    }
}
