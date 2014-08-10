package rapid.decoder;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import rapid.decoder.cache.CacheSource;

public class LoadIntoViewTask extends AsyncTask<Object, Object, Object[]> {
    private Decodable mDecodable;
    private Decodable.OnBitmapDecodedListener mListener;
    private WeakReference<Object> mWeakKey;
    private Object mStrongKey;
    private ViewFrameBuilder mFrameBuilder;

    public LoadIntoViewTask(Decodable decoder, Decodable.OnBitmapDecodedListener listener) {
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
        Decodable d = null;
        if (mFrameBuilder != null) {
            d = mFrameBuilder.getFramedDecoder(false);
        }
        if (d == null) {
            d = mDecodable;
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
        } else {
            mListener.onCancel();
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

    public void setFrameBuilder(ViewFrameBuilder builder) {
        mFrameBuilder = builder;
    }
}
