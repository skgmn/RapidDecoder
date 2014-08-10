package rapid.decoder;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;

import java.lang.ref.WeakReference;

import rapid.decoder.cache.CacheSource;

class BackgroundBitmapLoadTask extends AsyncTask<Object, Object, Object[]> {
    private Decodable mDecodable;
    private Decodable.OnBitmapDecodedListener mListener;
    private WeakReference<Object> mWeakKey;
    private Object mStrongKey;
    private ViewFrameBuilder mFrameBuilder;

    public void setDecodable(Decodable decodable) {
        mDecodable = decodable;
    }

    public void setOnBitmapDecodedListener(Decodable.OnBitmapDecodedListener listener) {
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
        if (bitmap == null || isCancelled()) return null;

        CacheSource cacheSource = d.cacheSource();
        return new Object[] { bitmap, cacheSource };
    }

    @Override
    protected void onPostExecute(Object[] result) {
        Object key = key();
        if (key != null && BitmapDecoder.sTaskManager.remove(key) != null && result != null) {
            mListener.onBitmapDecoded((Bitmap) result[0], (CacheSource) result[1]);
        } else {
            mListener.onCancel();
        }
    }

    @Override
    protected void onCancelled() {
        if (mListener != null) {
            mListener.onCancel();
        }
    }

    Object key() {
        if (mStrongKey != null) {
            return mStrongKey;
        }
        return mWeakKey != null ? mWeakKey.get() : null;
    }

    boolean isKeyStrong() {
        return mStrongKey != null;
    }

    public void cancel() {
        cancel(false);
        if (mDecodable != null) {
            mDecodable.cancel();
        }
        Object key = key();
        if (key != null) {
            BitmapDecoder.sTaskManager.remove(key);
        }
    }

    public void setFrameBuilder(ViewFrameBuilder builder) {
        mFrameBuilder = builder;
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            execute();
        }
    }
}
