package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.widget.ImageView
import rapiddecoder.frame.FramingMethod
import rapiddecoder.frame.FramingMethods

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

    fun scaleBy(scale: Float) = scaleBy(scale, scale)

    fun region(bounds: Rect) = region(bounds.left, bounds.top, bounds.right, bounds.bottom)

    open fun scaleWidth(width: Int): BitmapLoader {
        if (width <= 0) {
            throw IllegalArgumentException("width should be positive")
        }
        return scaleTo(width, Math.round(width * (this.height.toFloat() / this.width)))
    }

    open fun scaleHeight(height: Int): BitmapLoader {
        if (height <= 0) {
            throw IllegalArgumentException("height should be positive")
        }
        return scaleTo(Math.round(height * (this.width.toFloat() / this.height)), height)
    }

    @JvmOverloads
    fun frame(framingMethod: FramingMethod, frameWidth: Int, frameHeight: Int,
              background: Drawable? = null): BitmapLoader =
            FramedBitmapLoader(this, framingMethod, frameWidth, frameHeight, background)

    @JvmOverloads
    fun frame(scaleType: ImageView.ScaleType, frameWidth: Int, frameHeight: Int,
              background: Drawable? = null): BitmapLoader =
            frame(FramingMethods.fromScaleType(scaleType), frameWidth, frameHeight, background)

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
        @JvmStatic
        fun fromResource(res: Resources, resId: Int): BitmapLoader {
            val source = object : BitmapSource {
                override val densityRatioSupported: Boolean
                    get() = true

                override fun decode(opts: BitmapFactory.Options): Bitmap? =
                        BitmapFactory.decodeResource(res, resId, opts)

                override fun createRegionDecoder(): BitmapRegionDecoder {
                    val stream = res.openRawResource(resId)
                    return BitmapRegionDecoder.newInstance(stream, false)
                }
            }
            return BitmapResourceFullDecoder(source)
        }
    }
}