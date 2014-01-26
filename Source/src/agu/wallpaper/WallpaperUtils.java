package agu.wallpaper;

import android.graphics.Rect;

public final class WallpaperUtils {
	public static void translateOffsetX(int wallpaperWidth, int pageWidth, float offset, Rect out) {
		out.left = (int) Math.round((wallpaperWidth - pageWidth) * offset);
		out.right = out.left + pageWidth;
	}
	
	public static void translateOffsetY(int wallpaperHeight, int pageHeight, float offset, Rect out) {
		out.top = (int) Math.round((wallpaperHeight - pageHeight) * offset);
		out.bottom = out.top + pageHeight;
	}
}
