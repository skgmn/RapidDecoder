package agu.compat;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import static agu.caching.ResourcePool.*;

public class DisplayCompat {
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static int getWidth(Display display) {
		if (Build.VERSION.SDK_INT >= 13) {
			final Point size = POINT.obtain();
			try {
				display.getSize(size);
				return size.x;
			} finally {
				POINT.recycle(size);
			}
		} else {
			return display.getWidth();
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static int getHeight(Display display) {
		if (Build.VERSION.SDK_INT >= 13) {
			final Point size = POINT.obtain();
			try {
				display.getSize(size);
				return size.y;
			} finally {
				POINT.recycle(size);
			}
		} else {
			return display.getHeight();
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static void getSize(Display display, Point outSize) {
		if (Build.VERSION.SDK_INT >= 13) {
			display.getSize(outSize);
		} else {
			outSize.x = display.getWidth();
			outSize.y = display.getHeight();
		}
	}
}
