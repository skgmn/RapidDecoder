package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable

abstract class BitmapLoader {
    abstract val sourceWidth: Int
    abstract val sourceHeight: Int
    abstract val width: Int
    abstract val height: Int
    abstract val mimeType: String?
    abstract val hasSize: Boolean

    abstract fun scaleTo(width: Int, height: Int): BitmapLoader
    abstract fun scaleBy(x: Float, y: Float): BitmapLoader
    abstract fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader

    fun region(bounds: Rect) = region(bounds.left, bounds.top, bounds.right, bounds.bottom)

    fun scaleWidth(width: Int) {
        if (width <= 0) {
            throw IllegalArgumentException("width should be positive")
        }
        scaleTo(width, Math.round(width * (this.height.toFloat() / this.width)))
    }

    fun scaleHeight(height: Int) {
        if (height <= 0) {
            throw IllegalArgumentException("height should be positive")
        }
        scaleTo(Math.round(height * (this.width.toFloat() / this.height)), height)
    }

    @JvmOverloads
    fun frame(framer: Framer, frameWidth: Int, frameHeight: Int,
              background: Drawable? = null): BitmapLoader =
            FramedBitmapLoader(this, framer, frameWidth, frameHeight, background)

    abstract fun loadBitmap(options: LoadBitmapOptions): Bitmap

    fun loadBitmap() = loadBitmap(LoadBitmapOptions())

    protected fun checkScaleToArguments(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("width and height should be positive")
        }
    }

    protected fun checkScaleByArguments(x: Float, y: Float) {
        if (x <= 0f || y <= 0f) {
            throw IllegalArgumentException("x and y should be positive")
        }
    }

    companion object {
        fun fromResource(res: Resources, resId: Int): BitmapLoader =
                BitmapFromResource(res, resId)
    }
}