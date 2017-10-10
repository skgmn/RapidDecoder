package rapiddecoder.util

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object BitmapUtils {
    @JvmStatic
    fun getByteCount(bitmap: Bitmap): Int {
        return if (Build.VERSION.SDK_INT >= 19) {
            bitmap.allocationByteCount
        } else if (Build.VERSION.SDK_INT >= 12) {
            bitmap.byteCount
        } else {
            getByteCount(bitmap.width, bitmap.height, bitmap.config)
        }
    }

    @JvmStatic
    fun getByteCount(width: Int, height: Int, config: Config): Int {
        val bytesPerPixel = if (config == Config.ARGB_8888) 4 else 2
        return width * height * bytesPerPixel
    }

    @JvmStatic
    fun copy(bitmap: Bitmap, rectSrc: Rect?, targetWidth: Int, targetHeight: Int, config: Config,
             filter: Boolean): Bitmap {
        val newBitmap = Bitmap.createBitmap(targetWidth, targetHeight, config)
        val canvas = Canvas(newBitmap)
        val paint = if (filter) {
            Paint(Paint.FILTER_BITMAP_FLAG)
        } else {
            null
        }
        canvas.drawBitmap(bitmap, rectSrc, Rect(0, 0, targetWidth, targetHeight), paint)
        return newBitmap
    }

    @JvmStatic
    fun copy(d: Drawable, rectSrc: Rect?, targetWidth: Int, targetHeight: Int, config: Config,
             filter: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, config)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && d is ColorDrawable) {
            bitmap.eraseColor(d.color)
        } else {
            val canvas = Canvas(bitmap)
            d.isFilterBitmap = filter
            if (rectSrc == null || d.intrinsicWidth == -1 || d.intrinsicHeight == -1 ||
                    (rectSrc.left == 0 && rectSrc.top == 0 &&
                            rectSrc.right == targetWidth && rectSrc.bottom == targetHeight)) {
                d.setBounds(0, 0, targetWidth, targetHeight)
            } else {
                val scaleWidth = targetWidth.toFloat() / rectSrc.width()
                val scaleHeight = targetHeight.toFloat() / rectSrc.height()
                val left = Math.round(-rectSrc.left * scaleWidth)
                val top = Math.round(-rectSrc.top * scaleHeight)
                val right = left + Math.round(d.intrinsicWidth * scaleWidth)
                val bottom = top + Math.round(d.intrinsicHeight * scaleHeight)
                d.setBounds(left, top, right, bottom)
            }
            d.draw(canvas)
        }
        return bitmap
    }
}
