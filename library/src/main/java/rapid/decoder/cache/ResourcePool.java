package rapid.decoder.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

public abstract class ResourcePool<T> {
	private static final int DEFAULT_CAPACITY = 4;
	
	private static ArrayList<ResourcePool<?>> pools;
	
	private Object[] stack;
	private int top = 0;
	
	public static class PaintPool extends ResourcePool<Paint> {
		@Override
		protected Paint newInstance() {
			return new Paint();
		}

		@Override
		protected void onReset(Paint obj) {
			obj.reset();
		}
		
		public Paint obtain(int flags) {
			final Paint p = obtainImpl(true);
			p.setFlags(flags);
			return p;
		}
	}
	public static final PaintPool PAINT = new PaintPool();
	
	public static class RectPool extends ResourcePool<Rect> {
		@Override
		protected Rect newInstance() {
			return new Rect();
		}
		
		@Override
		protected void onReset(Rect obj) {
			obj.set(0, 0, 0, 0);
		}
		
		public Rect obtain(int left, int top, int right, int bottom) {
			final Rect rect = obtainImpl(false);
			rect.set(left, top, right, bottom);
			return rect;
		}
		
		public Rect obtain(Rect other) {
			final Rect rect = obtainImpl(false);
			rect.set(other);
			return rect;
		}
	}
	public static final RectPool RECT = new RectPool();
	
	public static class RectFPool extends ResourcePool<RectF> {
		@Override
		protected RectF newInstance() {
			return new RectF();
		}

		@Override
		protected void onReset(RectF obj) {
			obj.set(0, 0, 0, 0);
		}

		public RectF obtain(float left, float top, float right, float bottom) {
			final RectF rect = obtainImpl(false);
			rect.set(left, top, right, bottom);
			return rect;
		}
	}
	public static final RectFPool RECTF = new RectFPool();
	
	public static class PointPool extends ResourcePool<Point> {
		@Override
		protected Point newInstance() {
			return new Point();
		}
		
		@Override
		protected void onReset(Point obj) {
			obj.set(0, 0);
		}
		
		public Point obtain(int x, int y) {
			final Point p = obtainImpl(false);
			p.set(x, y);
			return p;
		}
	}
	public static final PointPool POINT = new PointPool();
	
	public static final ResourcePool<Matrix> MATRIX = new ResourcePool<Matrix>() {
		@Override
		protected Matrix newInstance() {
			return new Matrix();
		}
		
		@Override
		protected void onReset(Matrix obj) {
			obj.reset();
		}
	};
	
	public static final ResourcePool<Options> OPTIONS = new ResourcePool<Options>() {
		@Override
		protected Options newInstance() {
			return new Options();
		}
		
		@SuppressLint("NewApi")
		@Override
		protected void onReset(Options obj) {
			if (Build.VERSION.SDK_INT >= 10) {
				obj.inPreferQualityOverSpeed = false;
				if (Build.VERSION.SDK_INT >= 11) {
					obj.inMutable = false;
					
					if (Build.VERSION.SDK_INT >= 19) {
						obj.inPremultiplied = true;
					}
				}
			}
			
			obj.inDensity = 0;
			obj.inDither = true;
			obj.inInputShareable = false;
			obj.inJustDecodeBounds = false;
			obj.inPurgeable = false;
			obj.inSampleSize = 0;
			obj.inScaled = true;
			obj.inScreenDensity = 0;
			obj.inTargetDensity = 0;
			obj.mCancel = false;
			obj.outHeight = 0;
			obj.outWidth = 0;
		}
		
		@SuppressLint("NewApi")
		@Override
		protected boolean onRecycle(Options obj) {
			if (Build.VERSION.SDK_INT >= 11) {
				obj.inBitmap = null;
			}
			
			obj.inPreferredConfig = null;
			obj.inTempStorage = null;
			obj.outMimeType = null;
			
			return true;
		}
	};
	
	public static class CanvasPool extends ResourcePool<Canvas> {
		private Field Canvas_mNativeCanvas;
		private Field Canvas_mBitmap;
		private Field Canvas_mGL;
		private Field Canvas_mDensity;
		private Method Canvas_initRaster;
		private Method Canvas_finalizer;
		
		@Override
		protected Canvas newInstance() {
			return new Canvas();
		}
		
		@Override
		protected boolean onRecycle(Canvas obj) {
			if (Build.VERSION.SDK_INT >= 14) {
				obj.setBitmap(null);
				return true;
			} else if (Build.VERSION.SDK_INT >= 11) {
				// No source available for Honeycomb.
				return false;
			} else {
				// Canvas.setBitmap(null) throws an NullPointerException before API Level 14.
				
				try {
					if (Canvas_mNativeCanvas == null) {
						Canvas_mNativeCanvas = Canvas.class.getDeclaredField("mNativeCanvas");
						Canvas_mNativeCanvas.setAccessible(true);
					}
					if (Canvas_mBitmap == null) {
						Canvas_mBitmap = Canvas.class.getDeclaredField("mBitmap");
						Canvas_mBitmap.setAccessible(true);
					}
					if (Canvas_mGL == null) {
						Canvas_mGL = Canvas.class.getDeclaredField("mGL");
						Canvas_mGL.setAccessible(true);
					}
					if (Canvas_initRaster == null) {
						Canvas_initRaster = Canvas.class.getDeclaredMethod("initRaster", Integer.TYPE);
						Canvas_initRaster.setAccessible(true);
					}
					if (Canvas_mDensity == null) {
						Canvas_mDensity = Canvas.class.getDeclaredField("mDensity");
						Canvas_mDensity.setAccessible(true);
					}
					if (Canvas_finalizer == null) {
						Canvas_finalizer = Canvas.class.getDeclaredMethod("finalizer", Integer.TYPE);
						Canvas_finalizer.setAccessible(true);
					}
					
					Object gl = Canvas_mGL.get(obj);
					if (gl != null) {
						return false;
					}
					
					int nativeCanvas = Canvas_mNativeCanvas.getInt(obj);
					if (nativeCanvas != 0) {
						Canvas_finalizer.invoke(null, nativeCanvas);
					}
					
					int nullCanvas = (Integer) Canvas_initRaster.invoke(null, 0);
					Canvas_mNativeCanvas.set(obj, nullCanvas);
					
					Canvas_mBitmap.set(obj, null);
					Canvas_mDensity.set(obj, Bitmap.DENSITY_NONE);
					
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		}
		
		@Override
		protected void onReset(Canvas obj) {
			obj.clipRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
			obj.setDrawFilter(null);
			obj.setMatrix(null);
		}
		
		public Canvas obtain(Bitmap bitmap) {
			final Canvas cv = obtainImpl(true);
			cv.setBitmap(bitmap);
			return cv;
		}
	}
	public static final CanvasPool CANVAS = new CanvasPool();
	
	public static void clearPools() {
		synchronized (ResourcePool.class) {
			if (pools != null) {
				for (ResourcePool<?> pool: pools) {
					pool.clear();
				}
			}
		}
	}
	
	protected abstract T newInstance();
	
	public ResourcePool() {
		synchronized (ResourcePool.class) {
			if (pools == null) {
				pools = new ArrayList<ResourcePool<?>>();
			}
			pools.add(this);
		}
	}
	
	protected void onReset(T obj) {
	}
	
	protected boolean onRecycle(T obj) {
		return true;
	}

	public T obtain() {
		return obtainImpl(true);
	}
	
	public T obtainNotReset() {
		return obtainImpl(false);
	}
	
	@SuppressWarnings("unchecked")
	T obtainImpl(boolean reset) {
		synchronized (this) {
			if (stack == null || top == 0) {
				return newInstance();
			} else {
				final T obj = (T) stack[--top];
				stack[top] = null;
				
				if (reset) onReset(obj);
				return obj;
			}
		}
	}
	
	public void recycle(T obj) {
		if (obj == null || !onRecycle(obj)) return;
		
		synchronized (this) {
			if (stack == null) {
				stack = new Object [DEFAULT_CAPACITY];
			}
			if (top >= stack.length) {
				final Object[] newStack = new Object [stack.length * 2];
				System.arraycopy(stack, 0, newStack, 0, stack.length);
				stack = newStack;
			}
			
			stack[top++] = obj;
		}
	}
	
	public void clear() {
		synchronized (this) {
			stack = null;
			top = 0;
		}
	}
}
