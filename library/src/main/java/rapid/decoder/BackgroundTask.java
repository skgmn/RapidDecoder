package rapid.decoder;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;

import java.lang.ref.WeakReference;

import rapid.decoder.cache.CacheSource;

class BackgroundTask extends AsyncTask<Object, Object, Object[]> {
    private Decodable mDecodable;
    private Decodable.OnBitmapDecodedListener mListener;
    private ViewFrameBuilder mFrameBuilder;
    private Object mKey;

    public BackgroundTask(Object key) {
        mKey = key;
    }

    public void setDecodable(Decodable decodable) {
        mDecodable = decodable;
    }

    public void setOnBitmapDecodedListener(Decodable.OnBitmapDecodedListener listener) {
        mListener = listener;
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

    private boolean removeKey() {
        if (mKey == null) return false;
        if (mKey instanceof WeakReference) {
            return BackgroundTaskManager.removeWeak(((WeakReference) mKey).get()) != null;
        } else {
            return BackgroundTaskManager.removeStrong(mKey) != null;
        }
    }

    @Override
    protected void onPostExecute(Object[] result) {
        if (removeKey() && result != null) {
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

    public void cancel() {
        cancel(false);
        if (mDecodable != null) {
            mDecodable.cancel();
        }
        removeKey();
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
