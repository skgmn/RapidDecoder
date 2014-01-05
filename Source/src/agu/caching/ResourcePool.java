package agu.caching;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory.Options;
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
		protected void reset(Paint obj) {
			obj.reset();
		}
	};
	
	public static final ResourcePool<Rect> RECT = new ResourcePool<Rect>() {
		@Override
		protected Rect newInstance() {
			return new Rect();
		}
		
		@Override
		protected void reset(Rect obj) {
			obj.set(0, 0, 0, 0);
		}
	};
	
	public static final ResourcePool<RectF> RECTF = new ResourcePool<RectF>() {
		@Override
		protected RectF newInstance() {
			return new RectF();
		}

		@Override
		protected void reset(RectF obj) {
			obj.set(0, 0, 0, 0);
		}
	};
	
	public static final ResourcePool<Point> POINT = new ResourcePool<Point>() {
		@Override
		protected Point newInstance() {
			return new Point();
		}
		
		@Override
		protected void reset(Point obj) {
			obj.set(0, 0);
		}
	};
	
	public static final ResourcePool<Matrix> MATRIX = new ResourcePool<Matrix>() {
		@Override
		protected Matrix newInstance() {
			return new Matrix();
		}
		
		@Override
		protected void reset(Matrix obj) {
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
		protected void reset(Options obj) {
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
	
	protected void reset(T obj) {
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
				if (reset) reset(obj);
				return obj;
			}
		}
	}
	
	public void recycle(T obj) {
		if (obj == null) return;
		
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
