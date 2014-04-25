package agu.bitmap.async;

import java.util.WeakHashMap;

import agu.bitmap.BitmapSource;
import android.graphics.Bitmap;
import android.os.AsyncTask;

public class AsyncBitmapLoader {
	public interface OnLoadingStateChangedListener {
		void onLoadingStateChanged(int state);
	}
	
	public static final int STATE_IDLE = 0;
	public static final int STATE_LOADING = 1;
	
	private WeakHashMap<Object, Loader> mSingletonLoaders;
	private int mLoadersCount = 0;
	private OnLoadingStateChangedListener mOnLoadingStateChanged;
	
	public AsyncBitmapLoader() {
	}
	
	public void load(BitmapSource source, AsyncBitmapCallback callback) {
		new Loader(null, source, callback).execute();
	}
	
	public void load(Object singletonKey, BitmapSource source, AsyncBitmapCallback callback) {
		if (mSingletonLoaders == null) {
			mSingletonLoaders = new WeakHashMap<Object, AsyncBitmapLoader.Loader>();
		} else {
			cancel(singletonKey);
		}
		
		Loader loader = new Loader(singletonKey, source, callback);
		mSingletonLoaders.put(singletonKey, loader);
		
		loader.execute();
	}
	
	public void load(BitmapSource source, BitmapBinder binder) {
		Object key = binder.getSingletonKey();
		
		if (key == null) {
			load(source, binder);
		} else {
			load(key, source, binder);
		}
	}
	
	public void cancel(Object singletonKey) {
		if (mSingletonLoaders == null) return;
		
		final Loader loader = mSingletonLoaders.remove(singletonKey);
		if (loader != null) {
			loader.cancel();
		}
	}
	
	public int getLoadingJobsCount() {
		return mSingletonLoaders.size();
	}
	
	public void setOnLoadingStateChangedListener(OnLoadingStateChangedListener listener) {
		mOnLoadingStateChanged = listener;
	}
	
	private class Loader extends AsyncTask<Object, Object, Bitmap> {
		private BitmapSource mBitmapSource;
		private AsyncBitmapCallback mCallback;
		private Object mKey;
		
		public Loader(Object key, BitmapSource source, AsyncBitmapCallback callback) {
			mKey = key;
			mBitmapSource = source;
			mCallback = callback;
			
			if (mLoadersCount++ == 0 && mOnLoadingStateChanged != null) {
				mOnLoadingStateChanged.onLoadingStateChanged(STATE_LOADING);
			}
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			return mBitmapSource.bitmap();
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			checkStateChanged();
			
			final boolean isValid;
			
			if (mKey != null) {
				isValid = (mSingletonLoaders.remove(mKey) == this);
			} else {
				isValid = true;
			}
			
			if (isValid) {
				mCallback.onBitmapLoaded(result);
			}
		}
		
		@Override
		protected void onCancelled() {
			checkStateChanged();
		}
		
		private void checkStateChanged() {
			if (--mLoadersCount == 0 && mOnLoadingStateChanged != null) {
				mOnLoadingStateChanged.onLoadingStateChanged(STATE_IDLE);
			}
		}
		
		public void cancel() {
			cancel(false);
			mBitmapSource.cancel();
		}
	}
}
