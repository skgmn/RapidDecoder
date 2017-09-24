package rapiddecoder

import android.graphics.Bitmap

abstract class BitmapLoader {
    abstract val width: Int
    abstract val height: Int
    abstract val mimeType: String?

    abstract fun scaleTo(width: Int, height: Int): BitmapLoader
    abstract fun scaleBy(x: Float, y: Float): BitmapLoader
    abstract fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader

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

    abstract fun loadBitmap(approx: Boolean): Bitmap

    fun loadBitmap() = loadBitmap(false)

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
}