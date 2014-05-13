package agu.caching;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class DiskLruCache<T> {
    private final Object mDiskCacheLock = new Object();
	private DiskLruCacheEngine mCache;
	private Context mContext;
	private String mCacheName;
	private long mCacheSize;
    private boolean mDiskCacheStarting = true;
	
	public DiskLruCache(Context context, String cacheName, long cacheSize) {
		mContext = context;
		mCacheName = cacheName;
		mCacheSize = cacheSize;
		
		new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				initDiskCache();
				return null;
			}
		}.execute();
	}

    private void initDiskCache() {
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mCache == null || mCache.isClosed()) {
                File diskCacheDir = getDiskCacheDir(mContext, mCacheName);
                if (!diskCacheDir.exists()) {
                    diskCacheDir.mkdirs();
                }
                if (getUsableSpace(diskCacheDir) > mCacheSize) {
                    try {
                        mCache = DiskLruCacheEngine.open(
                                diskCacheDir, 1, 2, mCacheSize);
                    } catch (final IOException e) {
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }
    
    public void put(T key, InputStream is) {
    	final String hash = Integer.toString(key.hashCode());

    	synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
        	
            // Add to disk cache
            if (mCache != null) {
                OutputStream out = null;
                try {
                   final DiskLruCacheEngine.Editor editor = mCache.edit(hash);
                   if (editor != null) {
                       out = editor.newOutputStream(0);
                       ObjectOutputStream oos = new ObjectOutputStream(out);
                       oos.writeObject(key);
                       editor.commit();
                       oos.close();
                       
                       out = editor.newOutputStream(1);
                       
                       byte[] buffer = new byte [4096];
                       int bytesCount;
                       while ((bytesCount = is.read(buffer)) > 0) {
                    	   out.write(buffer, 0, bytesCount);
                       }
                       
                       editor.commit();
                       oos.close();
                   }
                } catch (final IOException e) {
                } catch (Exception e) {
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
    }
    
    public InputStream get(T key) {
    	final String hash = Integer.toString(key.hashCode());

    	synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            
            if (mCache == null) return null;
        	
            // Add to disk cache
            InputStream inputStream = null;
            try {
                final DiskLruCacheEngine.Snapshot snapshot = mCache.get(hash);
                if (snapshot == null) return null;
                
                inputStream = snapshot.getInputStream(0);
                if (inputStream == null) return null;
                
            	ObjectInputStream ois = new ObjectInputStream(inputStream);
            	Object storedKey = ois.readObject();
            	ois.close();
            	
            	if (!storedKey.equals(key)) return null;
            	
            	return snapshot.getInputStream(1);
            } catch (final IOException e) {
            } catch (ClassNotFoundException e) {
			} finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {}
            }
        }
    	
    	return null;
    }
    
    private static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                                context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }
    
    @SuppressLint("NewApi")
	private static boolean isExternalStorageRemovable() {
    	return (Build.VERSION.SDK_INT >= 9 ? Environment.isExternalStorageRemovable() : true);
    }

    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= 9) {
            return path.getUsableSpace();
        } else {
            final StatFs stats = new StatFs(path.getPath());
            return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
        }
    }
}
