package rapid.decoder;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramingAlgorithm;

public class BitmapLoadTask extends AsyncTask<Object, Object, Decodable.DecodeResult> {
    public static final int AUTOSIZE_NONE = 0;
    public static final int AUTOSIZE_WIDTH = 1;
    public static final int AUTOSIZE_HEIGHT = 2;
    public static final int AUTOSIZE_BOTH = 3;

    private Decodable mDecodable;
    private Decodable.OnBitmapDecodedListener mListener;
    private WeakReference<Object> mWeakKey;
    private Object mStrongKey;
    private FramingAlgorithm mFraming;
    private int mAutoSizeMode = AUTOSIZE_NONE;
    private int mMinWidth;
    private int mMinHeight;
    private int mMaxWidth;
    private int mMaxHeight;

    public BitmapLoadTask(Decodable decoder, Decodable.OnBitmapDecodedListener listener) {
        mDecodable = decoder;
        mListener = listener;
    }

    public void setKey(Object key, boolean isStrong) {
        if (isStrong) {
            mStrongKey = key;
            mWeakKey = null;
        } else {
            mStrongKey = null;
            mWeakKey = new WeakReference<Object>(key);
        }
    }

    @Override
    protected Decodable.DecodeResult doInBackground(Object... params) {
        Decodable d = mDecodable;
        if (mFraming != null && mDecodable instanceof BitmapDecoder) {
            int frameWidth, frameHeight;
            switch (mAutoSizeMode) {
                case AUTOSIZE_NONE:
                    frameWidth = mMinWidth;
                    frameHeight = mMinHeight;
                    break;

                case AUTOSIZE_WIDTH:
                    frameWidth = mMinWidth;
                    frameHeight = AspectRatioCalculator.getHeight(mDecodable.width(),
                            mDecodable.height(), frameWidth);
                    break;

                case AUTOSIZE_HEIGHT:
                    frameHeight = mMinHeight;
                    frameWidth = AspectRatioCalculator.getWidth(mDecodable.width(),
                            mDecodable.height(), frameHeight);
                    break;

                case AUTOSIZE_BOTH:
                    int width = mDecodable.width();
                    int height = mDecodable.height();

                    frameWidth = mMinWidth;
                    frameHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
                    if (frameHeight >= mMinHeight && frameHeight <= mMaxHeight) break;

                    frameWidth = Math.min(width, mMaxWidth);
                    frameHeight = AspectRatioCalculator.getHeight(width, height, frameWidth);
                    if (frameHeight >= mMinHeight && frameHeight <= mMaxHeight) break;

                    frameHeight = mMinHeight;
                    frameWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                    if (frameWidth >= mMinWidth && frameWidth <= mMinWidth) break;

                    frameHeight = Math.min(height, mMaxHeight);
                    frameWidth = AspectRatioCalculator.getWidth(width, height, frameHeight);
                    break;

                default:
                    // This should never be occurred.
                    frameWidth = frameHeight = 0;
                    break;
            }

            if (frameWidth != 0 && frameHeight != 0) {
                d = mFraming.createFramedDecoder((BitmapDecoder) mDecodable, frameWidth,
                        frameHeight);
            }
        }

        Decodable.DecodeResult result = new Decodable.DecodeResult();
        d.decode(result);
        return result;
    }

    @Override
    protected void onPostExecute(Decodable.DecodeResult result) {
        BackgroundTaskRecord record = BitmapDecoder.sTaskManager.remove(key());
        if (record != null && !record.isStale) {
            record.isStale = true;
            mListener.onBitmapDecoded(result.bitmap, result.cacheSource);
        }
    }

    Object key() {
        return mStrongKey != null ? mStrongKey : mWeakKey.get();
    }

    boolean isKeyStrong() {
        return mStrongKey != null;
    }

    public void cancel() {
        BackgroundTaskRecord record = BitmapDecoder.sTaskManager.remove(key());
        if (record != null) {
            record.isStale = true;
        }
        mDecodable.cancel();
    }

    public void setFraming(FramingAlgorithm framing, int autoSizeMode, int minWidth,
                           int minHeight, int maxWidth, int maxHeight) {
        mFraming = framing;
        mAutoSizeMode = autoSizeMode;
        mMinWidth = minWidth;
        mMinHeight = minHeight;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }
}
