package agu.bitmap.async;

import java.util.HashSet;
import java.util.WeakHashMap;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.BitmapDecoderDelegate;
import android.graphics.Bitmap;
import android.os.AsyncTask;

public class AsyncBitmapLoader {
	public interface OnLoadingStateChangedListener {
		void onLoadingStateChanged(int state);
	}
	
	public static final int STATE_IDLE = 0;
	public static final int STATE_LOADING = 1;
	
	private static AsyncBitmapLoader sGlobalInstance;
	
	private HashSet<Loader> mLoaders;
	private WeakHashMap<Object, Loader> mSingletonLoaders;
	private OnLoadingStateChangedListener mOnLoadingStateChanged;
	
	public AsyncBitmapLoader() {
	}
	
	public void load(BitmapDecoder decoder, AsyncBitmapCallback callback) {
		load(decoder, callback, null);
	}
	
	public void load(BitmapDecoder decoder, AsyncBitmapCallback callback, AsyncBitmapLoadStarter starter) {
		if (mLoaders == null) {
			mLoaders = new HashSet<AsyncBitmapLoader.Loader>();
		}
		checkLoadingStarted();
		
		Loader loader = new Loader(null, decoder, callback);
		mLoaders.add(loader);
		
		if (starter == null) {
			loader.execute();
		} else {
			starter.execute(loader);
		}
	}

	public void load(Object singletonKey, BitmapDecoder decoder, AsyncBitmapCallback callback) {
		load(singletonKey, decoder, callback, null);
	}
	
	public void load(Object singletonKey, BitmapDecoder decoder, AsyncBitmapCallback callback, AsyncBitmapLoadStarter starter) {
		if (mSingletonLoaders == null) {
			mSingletonLoaders = new WeakHashMap<Object, AsyncBitmapLoader.Loader>();
		} else {
			cancel(singletonKey);
		}
		checkLoadingStarted();
		
		Loader loader = new Loader(singletonKey, decoder, callback);
		mSingletonLoaders.put(singletonKey, loader);
		
		if (starter == null) {
			loader.execute();
		} else {
			starter.execute(loader);
		}
	}
	
	private void checkLoadingStarted() {
		if (mOnLoadingStateChanged != null &&
				(mSingletonLoaders == null || mSingletonLoaders.isEmpty()) &&
				(mLoaders == null || mLoaders.isEmpty())) {
			
			mOnLoadingStateChanged.onLoadingStateChanged(STATE_LOADING);
		}
	}
	
	public void load(BitmapDecoder decoder, BitmapBinder binder) {
		Object key = binder.singletonKey();
		
		if (key == null) {
			load(decoder, (AsyncBitmapCallback) binder);
		} else {
			load(key, decoder, binder);
		}
	}
	
	public void cancel(Object singletonKey) {
		if (mSingletonLoaders == null || singletonKey == null) return;
		
		final Loader loader = mSingletonLoaders.remove(singletonKey);
		if (loader != null) {
			loader.cancel();
		}
	}
	
	public void cancel(BitmapBinder binder) {
		if (binder == null) return;
		cancel(binder.singletonKey());
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
	
	private class Loader extends AsyncTask<Object, Object, Bitmap> implements AsyncBitmapLoaderJob {
		private BitmapDecoder mDecoder;
		private AsyncBitmapCallback mCallback;
		private Object mKey;
		private BitmapDecoderDelegate mDelegate;
		
		public Loader(Object key, BitmapDecoder decoder, AsyncBitmapCallback callback) {
			mKey = key;
			mDecoder = decoder;
			mCallback = callback;
		}
		
		@Override
		protected Bitmap doInBackground(Object... params) {
			if (mDelegate == null) {
				return mDecoder.decode();
			} else {
				return mDelegate.decode(mDecoder);
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			checkStateChanged();
			if (isValid(true)) {
				mCallback.onBitmapLoaded(result);
			} else {
				mCallback.onBitmapCancelled();
			}
		}
		
		@Override
		protected void onCancelled() {
			checkStateChanged();
			isValid(true);
			
			mCallback.onBitmapCancelled();
		}
		
		public boolean isValid() {
			return !isCancelled() && isValid(false);
		}
		
		private boolean isValid(boolean remove) {
			if (remove) {
				if (mKey != null) {
					return (mSingletonLoaders.remove(mKey) == this);
				} else {
					return mLoaders.remove(this);
				}
			} else {
				if (mKey != null) {
					return mSingletonLoaders.get(mKey) == this;
				} else {
					return mLoaders.contains(this);
				}
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
			mDecoder.cancel();
		}

		@Override
		public void setDelegate(BitmapDecoderDelegate d) {
			mDelegate = d;
		}

		@Override
		public void start() {
			execute();
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
