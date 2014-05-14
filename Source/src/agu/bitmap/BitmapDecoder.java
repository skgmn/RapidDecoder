package agu.bitmap;

import static agu.caching.ResourcePool.POINT;
import static agu.caching.ResourcePool.RECT;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import agu.caching.BitmapLruCache;
import agu.caching.DiskLruCache;
import agu.compat.DisplayCompat;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public abstract class BitmapDecoder {
	static final String MESSAGE_INVALID_RATIO = "Ratio should be positive.";

	private static final String MESSAGE_INVALID_URI = "Invalid uri: %s";
	private static final String MESSAGE_PACKAGE_NOT_FOUND = "Package not found: %s";
	private static final String MESSAGE_RESOURCE_NOT_FOUND = "Resource not found: %s";
	private static final String MESSAGE_UNSUPPORTED_SCHEME = "Unsupported scheme: %s";
	private static final String MESSAGE_URI_REQUIRES_CONTEXT = "This type of uri requires Context. Use BitmapDecoder.from(Context, Uri) instead.";
	
	protected static final int HASHCODE_NULL_REGION = 0x09DF79A9;
	protected static final int HASHCODE_NULL_BITMAP_OPTIONS = 0x00F590B9;

	private static final long DEFAULT_CACHE_SIZE = 4 * 1024 * 1024; // 4MB

	protected static Object sMemCacheLock = new Object();
	protected static BitmapLruCache<Object> sMemCache;
	
	static Object sDiskCacheLock = new Object();
	static DiskLruCache sDiskCache;

	public static void initMemoryCache(Context context) {
		final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		final Display display = wm.getDefaultDisplay();
		
		Point size = POINT.obtainNotReset();
		DisplayCompat.getSize(display, size);
		
		final Config defaultConfig = Build.VERSION.SDK_INT < 9 ? Config.RGB_565 : Config.ARGB_8888;
		initMemoryCache(BitmapUtils.getByteCount(size.x, size.y, defaultConfig));
		
		POINT.recycle(size);
	}

	public static void initMemoryCache(int size) {
		synchronized (sMemCacheLock) {
			if (sMemCache != null) {
				sMemCache.evictAll();
			}
			sMemCache = new BitmapLruCache<Object>(size, true);
		}
	}
	
	public static void destroyMemoryCache() {
		synchronized (sMemCacheLock) {
			if (sMemCache != null) {
				sMemCache.evictAll();
				sMemCache = null;
			}
		}
	}
	
	public static void clearMemoryCache() {
		synchronized (sMemCacheLock) {
			if (sMemCache != null) {
				sMemCache.evictAll();
			}
		}
	}
	
	public static void initDiskCache(Context context) {
		initDiskCache(context, DEFAULT_CACHE_SIZE);
	}
	
	public static void initDiskCache(Context context, long size) {
		synchronized (sDiskCacheLock) {
			sDiskCache = new DiskLruCache(context, "agu", size);
		}
	}
	
	public static void destroyDiskCache() {
		synchronized (sDiskCacheLock) {
			if (sDiskCache != null) {
				sDiskCache.close();
				sDiskCache = null;
			}
		}
	}
	
	public static void clearDiskCache() {
		synchronized (sDiskCacheLock) {
			if (sDiskCache != null) {
				sDiskCache.clear();
			}
		}
	}
	
	/**
	 * @return The width of the source image.
	 */
	public abstract int sourceWidth();
	
	/**
	 * @return The height of the source image.
	 */
	public abstract int sourceHeight();
	
	/**
	 * @return The estimated width of decoded image.
	 */
	public abstract int width();
	
	/**
	 * @return The estimated height of decoded image.
	 */
	public abstract int height();
	
	/**
	 * <p>Request the decoder to scale the image to the specific dimension while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param width A desired width to be scaled to.
	 * @param height A desired height to be scaled to.
	 * @param scaleFilter true if the image should be filtered.
	 */
	public abstract BitmapDecoder scale(int width, int height, boolean scaleFilter);
	
	/**
	 * <p>Request the decoder to scale the image by the specific ratio while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param widthRatio Scale ratio of width. 
	 * @param heightRatio Scale ratio of height.
	 * @param scaleFilter true if the image should be filtered.
	 */
	public abstract BitmapDecoder scaleBy(float widthRatio, float heightRatio, boolean scaleFilter);
	
	/**
	 * <p>Request the decoder to crop the image while decoding.
	 * Decoded image will be the same as an image which is cropped after decoding.</p>
	 * <p>It uses {@link BitmapRegionDecoder} on API level 10 or higher, otherwise it uses built-in decoder.</p>
	 */
	public abstract BitmapDecoder region(int left, int top, int right, int bottom);
	
	public abstract Rect region();
	
	/**
	 * Equivalent to <code>region(region.left, region.top, region.right, region.bottom)</code>.
	 */
	public abstract BitmapDecoder region(Rect region);

	/**
	 * Directly draw the image to canvas without any unnecessary scaling.
	 */
	public abstract void draw(Canvas cv, Rect rectDest);
	
	/**
	 * Set preferred bitmap configuration.
	 */
	public abstract BitmapDecoder config(Config config);
	
	/**
	 * Tell the decoder to either force using built-in decoder or not.
	 * @param force true if it should always use built-in decoder.
	 */
	public abstract BitmapDecoder useBuiltInDecoder(boolean force);
	
	public abstract Bitmap decode();
	public abstract BitmapDecoder clone();
	
	/**
	 * Request the decoder to cancel the decoding job currently working.
	 * This should be called by another thread.
	 */
	public abstract void cancel();
	
	/**
	 * <p>Tell the decoder whether decoded image should be mutable or not.</p>
	 * <p>It sets {@link BitmapFactory.Options#inMutable} to true on API level 11 or higher,
	 * otherwise it uses built-in decoder which always returns mutable bitmap.</p>
	 * @param mutable true if decoded image should be mutable.
	 */
	public abstract BitmapDecoder mutable(boolean mutable);

	//
	// Shortcuts
	//
	
	/**
	 * Equivalent to <code>useBuiltInDecoder(true)</code>.
	 */
	public BitmapDecoder useBuiltInDecoder() {
		return useBuiltInDecoder(true);
	}
	
	/**
	 * Equivalent to {@link #draw(Canvas, Rect)}.
	 */
	public void draw(Canvas cv, int left, int top, int right, int bottom) {
		final Rect bounds = RECT.obtain(left, top, right, bottom);
		draw(cv, bounds);
		RECT.recycle(bounds);
	}
	
	public void draw(Canvas cv, int left, int top) {
		draw(cv, left, top, left + width(), top + height());
	}
	
	/**
	 * Equivalent to <code>mutable(true)</code>.
	 */
	public BitmapDecoder mutable() {
		return mutable(true);
	}
	
	/**
	 * Equivalent to <code>scaleBy(widthRatio, heightRatio, true)</code>.
	 */
	public BitmapDecoder scaleBy(float widthRatio, float heightRatio) {
		return scaleBy(widthRatio, heightRatio, true);
	}

	/**
	 * Equivalent to <code>scaleBy(ratio, ratio, scaleFilter)</code>.
	 */
	public BitmapDecoder scaleBy(float ratio, boolean scaleFilter) {
		return scaleBy(ratio, ratio, scaleFilter);
	}

	/**
	 * Equivalent to <code>scaleBy(ratio, ratio, true)</code>.
	 */
	public BitmapDecoder scaleBy(float ratio) {
		return scaleBy(ratio, ratio, true);
	}

	/**
	 * Equivalent to <code>scale(width, height, true)</code>.
	 */
	public BitmapDecoder scale(int width, int height) {
		return scale(width, height, true);
	}
	
	public BitmapDecoder region(int width, int height) {
		final Rect prevRegion = region();
		if (prevRegion == null) {
			return region(0, 0, width, height);
		} else {
			return region(prevRegion.left, prevRegion.top,
					prevRegion.left + width, prevRegion.top + height);
		}
	}
	
	//
	// from()
	//
	
	public static BitmapDecoder from(byte[] data) {
		return new ByteArrayDecoder(data, 0, data.length);
	}
	
	public static BitmapDecoder from(byte[] data, int offset, int length) {
		return new ByteArrayDecoder(data, offset, length);
	}
	
	public static BitmapDecoder from(Resources res, int id) {
		return new ResourceDecoder(res, id);
	}
	
	public static BitmapDecoder from(String pathName) {
		return new FileDecoder(pathName);
	}
	
	public static BitmapDecoder from(FileDescriptor fd) {
		return new FileDescriptorDecoder(fd);
	}
	
	public static BitmapDecoder from(InputStream in) {
		return new StreamDecoder(in);
	}

	public static BitmapDecoder from(Uri uri) {
		return from(null, uri);
	}
	
	public static BitmapDecoder from(Context context, final Uri uri) {
		String scheme = uri.getScheme();
		
		if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
			if (context == null) {
				throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
			}
			
			List<String> segments = uri.getPathSegments();
			if (segments.size() != 2 || !segments.get(0).equals("drawable")) {
				throw new IllegalArgumentException(String.format(MESSAGE_INVALID_URI, uri));
			}
			
			Resources res;
			
			String packageName = uri.getAuthority();
			if (context.getPackageName().equals(packageName)) {
				res = context.getResources();
			} else {
				PackageManager pm = context.getPackageManager();
				try {
					res = pm.getResourcesForApplication(packageName);
				} catch (NameNotFoundException e) {
					throw new IllegalArgumentException(String.format(MESSAGE_PACKAGE_NOT_FOUND, packageName));
				}
			}
			
			String resName = segments.get(1);
			int id = res.getIdentifier(resName, "drawable", packageName);
			if (id == 0) {
				throw new IllegalArgumentException(String.format(MESSAGE_RESOURCE_NOT_FOUND, resName));
			}
			
			return new ResourceDecoder(res, id);
		} else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
			if (context == null) {
				throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
			}
			
			try {
				StreamDecoder d = new StreamDecoder(context.getContentResolver().openInputStream(uri));
				synchronized (sMemCacheLock) {
					if (sMemCache != null) {
						d.setMemCacheEnabler(new MemCacheEnabler<Uri>(uri));
					}
				}
				return d;
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
			return new FileDecoder(uri.getPath());
		} else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")) {
			String uriString = uri.toString();
			ExternalBitmapDecoder d = null;

			synchronized (sDiskCacheLock) {
				if (sDiskCache != null) {
					InputStream in = sDiskCache.get(uriString);
					if (in != null) {
						d = new StreamDecoder(in);
					}
				}
				
				if (d == null) {
					StreamDecoder sd = new StreamDecoder(new LazyInputStream(new StreamOpener() {
						@Override
						public InputStream openInputStream() {
							try {
								return new URL(uri.toString()).openStream();
							} catch (MalformedURLException e) {
								throw new IllegalArgumentException(e);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}					
						}
					}));
					if (sDiskCache != null) {
						sd.setCacheOutputStream(sDiskCache.getOutputStream(uriString));
					}
					
					d = sd;
				}
			}
			
			synchronized (sMemCacheLock) {
				if (sMemCache != null) {
					d.setMemCacheEnabler(new MemCacheEnabler<Uri>(uri));
				}
			}
			return d;
		} else {
			throw new IllegalArgumentException(String.format(MESSAGE_UNSUPPORTED_SCHEME, scheme));
		}
	}
	
	public static BitmapDecoder from(Bitmap bitmap) {
		return new InternalBitmapDecoder(bitmap);
	}
	
	public static BitmapDecoder from(Cursor cursor, int columnIndex) {
		return from(cursor.getBlob(columnIndex));
	}
	
	public static BitmapDecoder from(Cursor cursor, String columnName) {
		return from(cursor, cursor.getColumnIndex(columnName));
	}
}
