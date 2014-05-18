package agu.bitmap;

import static agu.caching.ResourcePool.POINT;
import static agu.caching.ResourcePool.RECT;
import static agu.caching.ResourcePool.RECTF;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import agu.caching.BitmapLruCache;
import agu.caching.DiskLruCache;
import agu.compat.DisplayCompat;
import agu.scaling.AspectRatioCalculator;
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
import android.graphics.RectF;
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
	
	//
	// Queries
	//
	
	protected static class Query {
	}
	
	protected static class ScaleTo extends Query {
		public float width;
		public float height;
		
		public ScaleTo(float width, float height) {
			this.width = width;
			this.height = height;
		}
	}
	
	protected static class ScaleBy extends Query {
		public float width;
		public float height;

		public ScaleBy(float width, float height) {
			this.width = width;
			this.height = height;
		}
	}
	
	protected static class Region extends Query {
		public int left;
		public int top;
		public int right;
		public int bottom;

		public Region(int left, int top, int right, int bottom) {
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}
	}
	
	protected LinkedList<Query> queries;
	private boolean queriesResolved = false;

	protected float ratioWidth = 1;
	protected float ratioHeight = 1;
	protected Rect region;
	
	protected BitmapDecoder() {
	}
	
	protected BitmapDecoder(BitmapDecoder other) {
		if (other.queries != null) {
			queries = new LinkedList<Query>();
			queries.addAll(other.queries);
		}
	}
	
	protected void addRequest(Query request) {
		if (queries == null) {
			queries = new LinkedList<BitmapDecoder.Query>();
		}
		queries.add(request);
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
	public int width() {
		resolveQueries();
		return (int) Math.ceil(
				(region == null ? sourceWidth() : region.width()) * ratioWidth);
	}

	/**
	 * @return The estimated height of decoded image.
	 */
	public int height() {
		resolveQueries();
		return (int) Math.ceil(
				(region == null ? sourceHeight() : region.height()) * ratioHeight);
	}
	
	/**
	 * <p>Request the decoder to scale the image to the specific dimension while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param width A desired width to be scaled to.
	 * @param height A desired height to be scaled to.
	 */
	public BitmapDecoder scale(int width, int height) {
		if (width < 0) {
			throw new IllegalArgumentException("Invalid width");
		}
		if (height < 0) {
			throw new IllegalArgumentException("Invalid height");
		}
		if (width == 0 && height == 0) {
			throw new IllegalArgumentException();
		}
		
		queriesResolved = false;
		
		Query lastRequest = (queries == null ? null : queries.getLast());
		if (lastRequest != null) {
			if (lastRequest instanceof ScaleTo) {
				ScaleTo scaleTo = (ScaleTo) lastRequest;
				scaleTo.width = width;
				scaleTo.height = height;
				
				return this;
			} else if (lastRequest instanceof ScaleBy) {
				queries.removeLast();
			}
		}
		
		addRequest(new ScaleTo(width, height));
		return this;
	}
	
	/**
	 * <p>Request the decoder to scale the image by the specific ratio while decoding.
	 * This will automatically calculate and set {@link BitmapFactory.Options#inSampleSize} internally,
	 * so you don't need to be concerned about it.</p>
	 * <p>It uses built-in decoder when scaleFilter is false.</p>
	 * @param widthRatio Scale ratio of width. 
	 * @param heightRatio Scale ratio of height.
	 */
	public BitmapDecoder scaleBy(float widthRatio, float heightRatio) {
		if (widthRatio <= 0 || heightRatio <= 0) {
			throw new IllegalArgumentException(MESSAGE_INVALID_RATIO);
		}
		
		queriesResolved = false;
		
		Query lastRequest = (queries == null ? null : queries.getLast());
		if (lastRequest != null) {
			if (lastRequest instanceof ScaleTo) {
				ScaleTo scaleTo = (ScaleTo) lastRequest;
				scaleTo.width = scaleTo.width * widthRatio;
				scaleTo.height = scaleTo.height * heightRatio;
				
				return this;
			} else if (lastRequest instanceof ScaleBy) {
				ScaleBy scaleBy = (ScaleBy) lastRequest;
				scaleBy.width *= widthRatio;
				scaleBy.height *= heightRatio;
				
				return this;
			}
		}
		
		addRequest(new ScaleBy(widthRatio, heightRatio));
		return this;
	}
	
	/**
	 * <p>Request the decoder to crop the image while decoding.
	 * Decoded image will be the same as an image which is cropped after decoding.</p>
	 * <p>It uses {@link BitmapRegionDecoder} on API level 10 or higher, otherwise it uses built-in decoder.</p>
	 */
	public BitmapDecoder region(int left, int top, int right, int bottom) {
		if (right < left || bottom < top) {
			throw new IllegalArgumentException();
		}
		
		queriesResolved = false;
		
		Query lastRequest = (queries == null ? null : queries.getLast());
		if (lastRequest != null) {
			if (lastRequest instanceof Region) {
				Region region = (Region) lastRequest;
				region.left += left;
				region.top += top;
				region.right = region.left + (right - left);
				region.bottom = region.top + (bottom - top);
				
				return this;
			}
		}
		
		addRequest(new Region(left, top, right, bottom));
		return this;
	}
	
	/**
	 * Equivalent to <code>region(region.left, region.top, region.right, region.bottom)</code>.
	 */
	public BitmapDecoder region(Rect region) {
		return region(region.left, region.top, region.right, region.bottom);
	}

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
	
	public abstract BitmapDecoder filterBitmap(boolean filter);
	
	public abstract Bitmap decode();
	public abstract BitmapDecoder clone();
	
	protected void resolveQueries() {
		if (queriesResolved) return;
		
		final float densityRatio = getDensityRatio();
		ratioWidth = ratioHeight = densityRatio;

		if (region != null) {
			RECT.recycle(region);
		}
		region = null;
		
		queriesResolved = true;
		if (queries == null) return;
		
		RectF regionf = null;
		
		for (Query r: queries) {
			if (r instanceof ScaleTo) {
				ScaleTo scaleTo = (ScaleTo) r;
				
				float w = (regionf == null ? sourceWidth() : regionf.width());
				float h = (regionf == null ? sourceHeight() : regionf.height());

				float targetWidth, targetHeight;
				if (scaleTo.width == 0) {
					targetHeight = scaleTo.height;
					targetWidth = AspectRatioCalculator.fitWidth(w, h, targetHeight);
				} else if (scaleTo.height == 0) {
					targetWidth = scaleTo.width;
					targetHeight = AspectRatioCalculator.fitHeight(w, h, targetWidth);
				} else {
					targetWidth = scaleTo.width;
					targetHeight = scaleTo.height;
				}
				
				ratioWidth = targetWidth / w;
				ratioHeight = targetHeight / h;
			} else if (r instanceof ScaleBy) {
				ScaleBy scaleBy = (ScaleBy) r;
				
				ratioWidth *= scaleBy.width;
				ratioHeight *= scaleBy.height;
			} else if (r instanceof Region) {
				Region rr = (Region) r;
				
				if (regionf == null) {
					final float left = rr.left / ratioWidth;
					final float top = rr.top / ratioHeight;
					final float right = rr.right / ratioWidth;
					final float bottom = rr.bottom / ratioHeight;
					
					regionf = RECTF.obtainNotReset();
					
					// Check boundaries
					regionf.left = Math.max(0, Math.min(left, sourceWidth()));
					regionf.top = Math.max(0, Math.min(top, sourceHeight()));
					regionf.right = Math.max(regionf.left, Math.min(right, sourceWidth()));
					regionf.bottom = Math.max(regionf.top, Math.min(bottom, sourceHeight()));
				} else {
					final float left = region.left + (rr.left / ratioWidth);
					final float top = region.top + (rr.top / ratioHeight);
					final float right = region.left + ((rr.right - rr.left) / ratioWidth);
					final float bottom = region.top + ((rr.bottom - rr.top) / ratioHeight);
					
					// Check boundaries
					regionf.left = Math.max(0, Math.min(left, regionf.right));
					regionf.top = Math.max(0, Math.min(top, regionf.bottom));
					regionf.right = Math.max(regionf.left, Math.min(right, regionf.right));
					regionf.bottom = Math.max(regionf.top, Math.min(bottom, regionf.bottom));
				}
			}
		}

		if (regionf != null) {
			region = RECT.obtain(
					(int) Math.round(regionf.left),
					(int) Math.round(regionf.top),
					(int) Math.round(regionf.right),
					(int) Math.round(regionf.bottom));
			RECTF.recycle(regionf);
		}
	}
	
	protected boolean queriesEquals(BitmapDecoder other) {
		if (queries == null) {
			return other.queries == null || other.queries.isEmpty();
		} else {
			if (queries.size() != other.queries.size()) return false;

			Iterator<Query> it1 = queries.iterator();
			Iterator<Query> it2 = other.queries.iterator();
			
			while (it1.hasNext()) {
				if (!it2.hasNext() || !it1.next().equals(it2.next())) return false;
			}
			
			return true;
		}
	}
	
	protected int queriesHash() {
		return (queries == null ? 0 : queries.hashCode());
	}

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
	 * Equivalent to <code>scaleBy(ratio, ratio)</code>.
	 */
	public BitmapDecoder scaleBy(float ratio) {
		return scaleBy(ratio, ratio);
	}

	public BitmapDecoder region(int width, int height) {
		return region(0, 0, width, height);
	}

	protected float getDensityRatio() {
		return 1f;
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			RECT.recycle(region);
		} finally {
			super.finalize();
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
