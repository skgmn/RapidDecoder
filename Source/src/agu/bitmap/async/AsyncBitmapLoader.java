package agu.bitmap.async;

import java.util.ArrayList;
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
	
	private static AsyncBitmapLoader sGlobalInstance;
	
	private ArrayList<Loader> mLoaders;
	private WeakHashMap<Object, Loader> mSingletonLoaders;
	private OnLoadingStateChangedListener mOnLoadingStateChanged;
	
	public AsyncBitmapLoader() {
	}
	
	public void load(BitmapSource source, AsyncBitmapCallback callback) {
		if (mLoaders == null) {
			mLoaders = new ArrayList<AsyncBitmapLoader.Loader>();
		}
		checkLoadingStarted();
		
		Loader loader = new Loader(null, source, callback);
		mLoaders.add(loader);
		
		loader.execute();
	}
	
	public void load(Object singletonKey, BitmapSource source, AsyncBitmapCallback callback) {
		if (mSingletonLoaders == null) {
			mSingletonLoaders = new WeakHashMap<Object, AsyncBitmapLoader.Loader>();
		} else {
			cancel(singletonKey);
		}
		checkLoadingStarted();
		
		Loader loader = new Loader(singletonKey, source, callback);
		mSingletonLoaders.put(singletonKey, loader);
		
		loader.execute();
	}
	
	private void checkLoadingStarted() {
		if (mOnLoadingStateChanged != null &&
				(mSingletonLoaders == null || mSingletonLoaders.isEmpty()) &&
				(mLoaders == null || mLoaders.isEmpty())) {
			
			mOnLoadingStateChanged.onLoadingStateChanged(STATE_LOADING);
		}
	}
	
	public void load(BitmapSource source, BitmapBinder binder) {
		Object key = binder.singletonKey();
		
		if (key == null) {
			load(source, (AsyncBitmapCallback) binder);
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
	
	public void cancelAll() {
		if (mLoaders != null) {
			for (Loader loader: mLoaders) {
				loader.cancel();
			}
		}
		if (mSingletonLoaders != null) {
			for (Loader loader: mSingletonLoaders.values()) {
				loader.cancel();
			}
		}
	}
	
	public int getCount() {
		return (mLoaders == null ? 0 : mLoaders.size()) +
				(mSingletonLoaders == null ? 0 : mSingletonLoaders.size());
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
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			return mBitmapSource.decode();
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			checkStateChanged();
			if (isValid()) {
				mCallback.onBitmapLoaded(result);
			} else {
				mCallback.onBitmapCancelled();
			}
		}
		
		@Override
		protected void onCancelled() {
			checkStateChanged();
			isValid();
			
			mCallback.onBitmapCancelled();
		}
		
		private boolean isValid() {
			if (mKey != null) {
				return (mSingletonLoaders.remove(mKey) == this);
			} else {
				return mLoaders.remove(this);
			}
		}
		
		private void checkStateChanged() {
			if (mOnLoadingStateChanged != null &&
					(mLoaders == null || mLoaders.isEmpty()) &&
					(mSingletonLoaders == null || mSingletonLoaders.isEmpty())) {
				
				mOnLoadingStateChanged.onLoadingStateChanged(STATE_IDLE);
			}
		}
		
		public void cancel() {
			cancel(false);
			mBitmapSource.cancel();
		}
	}
	
	public static AsyncBitmapLoader getInstance() {
		if (sGlobalInstance == null) {
			synchronized (AsyncBitmapLoader.class) {
				if (sGlobalInstance == null) {
					sGlobalInstance = new AsyncBitmapLoader();
				}
			}
		}
		return sGlobalInstance;
	}
}
