package rapiddecoder.util

import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object BitmapUtils {
    @JvmStatic
    fun getBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable) {
            return d.bitmap
        } else {
            val opaque = d.opacity == PixelFormat.OPAQUE

            val bitmap = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight,
                    if (opaque) Config.RGB_565 else Config.ARGB_8888)
            d.setDither(opaque)
            d.setBounds(0, 0, bitmap.width, bitmap.height)

            val cv = Canvas(bitmap)
            d.draw(cv)

            return bitmap
        }
    }

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
    fun copy(bitmap: Bitmap, srcRect: Rect?, targetWidth: Int, targetHeight: Int, config: Config,
             filter: Boolean): Bitmap {
        val newBitmap = Bitmap.createBitmap(targetWidth, targetHeight, config)
        val canvas = Canvas(newBitmap)
        val paint = if (filter) {
            Paint(Paint.FILTER_BITMAP_FLAG)
        } else {
            null
        }
        canvas.drawBitmap(bitmap, srcRect, Rect(0, 0, targetWidth, targetHeight), paint)
        return newBitmap
    }
}
