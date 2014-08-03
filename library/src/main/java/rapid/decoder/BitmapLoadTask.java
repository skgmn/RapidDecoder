package rapid.decoder;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public class BitmapLoadTask extends AsyncTask<Object, Object, Decodable.DecodeResult> {
    private Decodable mDecodable;
    private BitmapLoader.OnBitmapDecodedListener mListener;
    private WeakReference<Object> mWeakKey;
    private Object mStrongKey;

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
        Decodable.DecodeResult result = new Decodable.DecodeResult();
        mDecodable.decode(result);
        return result;
    }

    @Override
    protected void onPostExecute(Decodable.DecodeResult result) {
        if (BitmapDecoder.removeJob(getKey())) {
            mListener.onBitmapDecoded(result.bitmap, result.cacheSource);
        }
    }

    Object getKey() {
        return mStrongKey != null ? mStrongKey : mWeakKey;
    }

    Object getStrongKey() {
        return mStrongKey;
    }

    Object getWeakKey() {
        return mWeakKey != null ? mWeakKey.get() : null;
    }

    public void cancel() {
        BitmapDecoder.removeJob(getKey());
        mDecodable.cancel();
    }
}
