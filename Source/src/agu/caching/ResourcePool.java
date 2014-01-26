package agu.caching;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
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
	
	public static final ResourcePool<Paint> PAINT = new ResourcePool<Paint>() {
		@Override
		protected Paint newInstance() {
			return new Paint();
		}

		@Override
		protected void onReset(Paint obj) {
			obj.reset();
		}
	};
	
	public static final ResourcePool<Rect> RECT = new ResourcePool<Rect>() {
		@Override
		protected Rect newInstance() {
			return new Rect();
		}
		
		@Override
		protected void onReset(Rect obj) {
			obj.set(0, 0, 0, 0);
		}
	};
	
	public static final ResourcePool<RectF> RECTF = new ResourcePool<RectF>() {
		@Override
		protected RectF newInstance() {
			return new RectF();
		}

		@Override
		protected void onReset(RectF obj) {
			obj.set(0, 0, 0, 0);
		}
	};
	
	public static final ResourcePool<Point> POINT = new ResourcePool<Point>() {
		@Override
		protected Point newInstance() {
			return new Point();
		}
		
		@Override
		protected void onReset(Point obj) {
			obj.set(0, 0);
		}
	};
	
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
					obj.inBitmap = null;
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
			obj.inPreferredConfig = null;
			obj.inPurgeable = false;
			obj.inSampleSize = 0;
			obj.inScaled = true;
			obj.inScreenDensity = 0;
			obj.inTargetDensity = 0;
			obj.inTempStorage = null;
			obj.mCancel = false;
			obj.outHeight = 0;
			obj.outMimeType = null;
			obj.outWidth = 0;
		}
	};
	
	public static final ResourcePool<Canvas> CANVAS = new ResourcePool<Canvas>() {
		private Field Canvas_mNativeCanvas;
		private Field Canvas_mBitmap;
		private Method Canvas_native_setBitmap;
		
		@Override
		protected Canvas newInstance() {
			return new Canvas();
		}
		
		@Override
		protected void onReset(Canvas obj) {
		}
		
		@Override
		protected boolean onRecycle(Canvas obj) {
			if (Build.VERSION.SDK_INT >= 11) {
				obj.setBitmap(null);
				return true;
			} else {
				// Canvas.setBitmap(null) throws an NullPointerException before API Level 11.
				
				try {
					if (Canvas_mNativeCanvas == null) {
						Canvas_mNativeCanvas = Canvas.class.getDeclaredField("mNativeCanvas");
					}
					if (Canvas_mBitmap == null) {
						Canvas_mBitmap = Canvas.class.getDeclaredField("mBitmap");
					}
					if (Canvas_native_setBitmap == null) {
						Canvas_native_setBitmap = Canvas.class.getDeclaredMethod("native_setBitmap", Integer.TYPE, Integer.TYPE);
					}
					
					final int nativeCanvas = Canvas_mNativeCanvas.getInt(obj);
					Canvas_native_setBitmap.invoke(null, nativeCanvas, 0);
					Canvas_mBitmap.set(obj, null);
					
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		}
	};
	
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
		return obtain(true);
	}
	
	@SuppressWarnings("unchecked")
	public T obtain(boolean reset) {
		synchronized (this) {
			if (stack == null || top == 0) {
				return newInstance();
			} else {
				final T obj = (T) stack[--top];
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
		}
	}
}
