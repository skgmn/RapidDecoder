package agu.caching;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class DiskLruCache {
    private final Object mDiskCacheLock = new Object();
	private DiskLruCacheEngine mCache;
	private Context mContext;
	private String mCacheName;
	private long mCacheSize;
    private boolean mDiskCacheStarting = true;
	
	@SuppressLint("NewApi")
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

		AsyncTask<Integer, Object, Object> task = new AsyncTask<Integer, Object, Object>() {
			@Override
			protected Object doInBackground(Integer... params) {
				initDiskCache(params[0]);
				return null;
			}
		};

		if (Build.VERSION.SDK_INT >= 11) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, version);
		} else {
			task.execute(version);
		}
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
    
    public TransactionOutputStream getOutputStream(String key) {
    	final String hash = hashKeyForDisk(key);

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
                       DataOutputStream dos = new DataOutputStream(out);
                       dos.writeUTF(key);
                       dos.close();
                       
                       return new TransactionOutputStream(this, editor, editor.newOutputStream(1));
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
    
    public InputStream get(String key) {
    	final String hash = hashKeyForDisk(key);

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
                
                DataInputStream dis = new DataInputStream(inputStream);
                String storedKey = dis.readUTF();
            	dis.close();
            	
            	if (!storedKey.equals(key)) return null;
            	
            	return snapshot.getInputStream(1);
            } catch (final IOException e) {
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
    
    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a
     * disk filename.
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
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
    
    public void flush() throws IOException {
    	synchronized (mDiskCacheLock) {
    		if (mCache != null) {
    			mCache.flush();
    		}
    	}
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
