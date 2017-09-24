package rapid.decoder;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;

import java.lang.ref.WeakReference;

import rapid.decoder.cache.CacheSource;

class BackgroundTask extends AsyncTask<Object, Object, BackgroundTask.Result> {
    public static class Result {
        public Bitmap bitmap;
        public CacheSource cacheSource;

        public Result(Bitmap bitmap, CacheSource cacheSource) {
            this.bitmap = bitmap;
            this.cacheSource = cacheSource;
        }
    }

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

    private boolean removeKey() {
        if (mKey == null) return false;
        if (mKey instanceof WeakReference) {
            return BackgroundTaskManager.removeWeak(((WeakReference) mKey).get()) != null;
        } else {
            return BackgroundTaskManager.removeStrong(mKey) != null;
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (removeKey()) {
            if (mListener != null) {
                if (result == null) {
                    mListener.onBitmapDecoded(null, null);
                } else {
                    mListener.onBitmapDecoded(result.bitmap, result.cacheSource);
                }
                mListener = null;
            }
        } else {
            dispatchCancel();
        }
    }

    @Override
    protected Result doInBackground(Object... params) {
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
        return new Result(bitmap, cacheSource);
    }

    protected void onCancelled() {
        dispatchCancel();
    }

    private void dispatchCancel() {
        if (mListener != null) {
            mListener.onCancel();
            mListener = null;
        }
    }

    public void cancel() {
        cancel(false);
        if (mDecodable != null) {
            mDecodable.cancel();
        }
        removeKey();
        dispatchCancel();
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
