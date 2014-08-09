package rapid.decoder;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import rapid.decoder.cache.CacheSource;
import rapid.decoder.frame.AspectRatioCalculator;
import rapid.decoder.frame.FramingMethod;

public class BitmapLoadTask extends AsyncTask<Object, Object, Object[]> {
    public static final int AUTOSIZE_NONE = 0;
    public static final int AUTOSIZE_WIDTH = 1;
    public static final int AUTOSIZE_HEIGHT = 2;
    public static final int AUTOSIZE_BOTH = 3;

    private Decodable mDecodable;
    private Decodable.OnBitmapDecodedListener mListener;
    private WeakReference<Object> mWeakKey;
    private Object mStrongKey;
    private FramingMethod mFraming;
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
    protected Object[] doInBackground(Object... params) {
        Decodable d = mDecodable;
        if (mFraming != null && mDecodable instanceof BitmapDecoder) {
            int frameWidth, frameHeight;
            switch (mAutoSizeMode) {
                case AUTOSIZE_NONE:
                    frameWidth = mMinWidth;
                    frameHeight = mMinHeight;
                    break;

                case AUTOSIZE_WIDTH:
                    frameHeight = mMinHeight;
                    frameWidth = AspectRatioCalculator.getWidth(mDecodable.width(),
                            mDecodable.height(), frameHeight);
                    break;

                case AUTOSIZE_HEIGHT:
                    frameWidth = mMinWidth;
                    frameHeight = AspectRatioCalculator.getHeight(mDecodable.width(),
                            mDecodable.height(), frameWidth);
                    break;

                case AUTOSIZE_BOTH:
                    int width = frameWidth = mDecodable.width();
                    int height = frameHeight = mDecodable.height();

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
                    // This should never be occurred.
                    frameWidth = frameHeight = 0;
                    break;
            }

            if (frameWidth != 0 && frameHeight != 0) {
                d = mFraming.createFramedDecoder((BitmapDecoder) mDecodable, frameWidth,
                        frameHeight);
            }
        }

        Bitmap bitmap = d.decode();
        CacheSource cacheSource = d.cacheSource();
        return new Object[] { bitmap, cacheSource };
    }

    @Override
    protected void onPostExecute(Object[] result) {
        BackgroundTaskRecord record = BitmapDecoder.sTaskManager.remove(key());
        if (record != null && !record.isStale) {
            record.isStale = true;
            mListener.onBitmapDecoded((Bitmap) result[0], (CacheSource) result[1]);
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

    public void setFraming(FramingMethod framing, int autoSizeMode, int minWidth,
                           int minHeight, int maxWidth, int maxHeight) {
        mFraming = framing;
        mAutoSizeMode = autoSizeMode;
        mMinWidth = minWidth;
        mMinHeight = minHeight;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }
}
