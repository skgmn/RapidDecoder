package rapid.decoder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import rapid.decoder.cache.BitmapMetaInfo;
import rapid.decoder.compat.ImageViewCompat;
import rapid.decoder.compat.ViewCompat;
import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramedDecoder;
import rapid.decoder.frame.FramingMethod;

class ViewFrameBuilder {
    private static final int AUTOSIZE_NONE = 0;
    private static final int AUTOSIZE_WIDTH = 1;
    private static final int AUTOSIZE_HEIGHT = 2;
    private static final int AUTOSIZE_BOTH = 3;

    private BitmapDecoder mDecoder;
    private Object mId;
    private View mView;
    private FramingMethod mFraming;
    private int mAutoSizeMode = AUTOSIZE_NONE;
    private int mMinWidth;
    private int mMinHeight;
    private int mMaxWidth;
    private int mMaxHeight;
    private FramedDecoder mFramedDecoder;

    public ViewFrameBuilder(@NonNull BitmapDecoder decoder, @NonNull Object id, @NonNull View v,
                            @NonNull FramingMethod framing) {

        mDecoder = decoder;
        mId = id;
        mView = v;
        mFraming = framing;
    }

    public void prepareFraming() {
        ViewGroup.LayoutParams lp = mView.getLayoutParams();

        if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                mAutoSizeMode = AUTOSIZE_BOTH;
                mMinWidth = ViewCompat.getMinimumWidth(mView);
                mMaxWidth = getMaxWidth(mView);
                mMinHeight = ViewCompat.getMinimumHeight(mView);
                mMaxHeight = getMaxHeight(mView);
            } else {
                mAutoSizeMode = AUTOSIZE_WIDTH;
                mMinWidth = ViewCompat.getMinimumWidth(mView);
                mMaxWidth = getMaxWidth(mView);
                mMinHeight = mView.getHeight();
            }
        } else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            mAutoSizeMode = AUTOSIZE_HEIGHT;
            mMinWidth = mView.getWidth();
            mMinHeight = ViewCompat.getMinimumHeight(mView);
            mMaxHeight = getMaxHeight(mView);
        } else {
            mAutoSizeMode = AUTOSIZE_NONE;
            mMinWidth = mView.getWidth();
            mMinHeight = mView.getHeight();
        }
    }

    private static int getMaxWidth(View v) {
        return v instanceof ImageView ? ImageViewCompat.getMaxWidth((ImageView) v) : Integer
                .MAX_VALUE;
    }

    private static int getMaxHeight(View v) {
        return v instanceof ImageView ? ImageViewCompat.getMaxHeight((ImageView) v) : Integer
                .MAX_VALUE;
    }

    private int getWidth(boolean cacheOnly) {
        BitmapMetaInfo meta;
        synchronized (BitmapDecoder.sMemCacheLock) {
            meta = BitmapDecoder.sMetaCache.get(mId);
        }
        if (cacheOnly && meta == null) {
            return 0;
        }
        return meta != null ? meta.width : mDecoder.width();
    }

    private int getHeight(boolean cacheOnly) {
        BitmapMetaInfo meta;
        synchronized (BitmapDecoder.sMemCacheLock) {
            meta = BitmapDecoder.sMetaCache.get(mId);
        }
        if (cacheOnly && meta == null) {
            return 0;
        }
        return meta != null ? meta.height : mDecoder.height();
    }

    @Nullable
    public FramedDecoder getFramedDecoder(boolean cacheOnly) {
        if (mFramedDecoder != null) {
            return mFramedDecoder;
        }

        int frameWidth, frameHeight;
        if (mAutoSizeMode == AUTOSIZE_NONE) {
            frameWidth = mMinWidth;
            frameHeight = mMinHeight;
        } else {
            int width = getWidth(cacheOnly);
            int height = getHeight(cacheOnly);
            if (width == 0 || height == 0) return null;

            switch (mAutoSizeMode) {
                case AUTOSIZE_WIDTH:
                    frameHeight = mMinHeight;
                    frameWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                    break;

                case AUTOSIZE_HEIGHT:
                    frameWidth = mMinWidth;
                    frameHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
                    break;

                case AUTOSIZE_BOTH:
                    frameWidth = width;
                    frameHeight = height;

                    for (int j = Math.min(width, mMaxWidth); j > 0; --j) {
                        int i = AspectRatioCalculator.getHeight(width, height, j);
                        if (i >= mMinHeight && j <= mMaxHeight) {
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
