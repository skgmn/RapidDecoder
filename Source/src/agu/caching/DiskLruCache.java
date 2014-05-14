package agu.caching;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import agu.caching.DiskLruCacheEngine.Editor;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class DiskLruCache<T extends Serializable> {
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
		
		int version;
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			version = pi.versionCode;
		} catch (NameNotFoundException e) {
			version = 1;
		}

		new AsyncTask<Object, Object, Object>() {
			@Override
			protected Object doInBackground(Object... params) {
				initDiskCache((Integer) params[0]);
				return null;
			}
		}.execute(version);
	}
	
	public void close() {
		synchronized (mDiskCacheLock) {
			if (mCache != null) {
				try {
					mCache.close();
				} catch (IOException e) {
				}
			}
		}
	}

    private void initDiskCache(int version) {
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
                                diskCacheDir, version, 2, mCacheSize);
                    } catch (final IOException e) {
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }
    
    public OutputStream getOutputStream(T key) {
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
                       oos.close();
                       
                       return new AutoCommitOutputStream(editor, editor.newOutputStream(1));
                   }
                } catch (Exception e) {
                	e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
    	
    	return null;
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
    
    public void clear() {
    	synchronized (mDiskCacheLock) {
			if (mCache != null) {
				try {
					mCache.delete();
				} catch (IOException e) {
				}
			}
		}
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
    
    private class AutoCommitOutputStream extends OutputStream {
    	private Editor mEditor;
    	private OutputStream mOut;
    	
    	public AutoCommitOutputStream(Editor editor, OutputStream out) {
    		mEditor = editor;
    		mOut = out;
    	}
    	
    	@Override
    	public void close() throws IOException {
    		mOut.close();
    		mEditor.commit();

    		synchronized (mDiskCacheLock) {
    			if (mCache != null) {
    				mCache.flush();
    			}
			}
    	}
    	
    	@Override
    	public void flush() throws IOException {
    		mOut.flush();
    	}
    	
    	@Override
    	public void write(byte[] buffer, int offset, int count)
    			throws IOException {
    		
    		mOut.write(buffer, offset, count);
    	}
    	
		@Override
		public void write(int oneByte) throws IOException {
			mOut.write(oneByte);
		}
		
		@Override
		public void write(byte[] buffer) throws IOException {
			mOut.write(buffer);
		}
    }
}
