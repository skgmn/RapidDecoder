package rapiddecoder

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.widget.ImageView
import rapiddecoder.frame.FramingMethod
import rapiddecoder.frame.FramingMethods
import java.io.File
import java.io.InputStream

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
        fun fromResource(context: Context, resId: Int): BitmapLoader =
                fromResource(context.resources, resId)

        @JvmStatic
        fun fromResource(res: Resources, resId: Int): BitmapLoader =
                ResourceFullBitmapDecoder(AndroidResourceBitmapSource(res, resId))

        @JvmStatic
        fun fromAsset(context: Context, path: String): BitmapLoader =
                fromAsset(context.assets, path)

        @JvmStatic
        fun fromAsset(assets: AssetManager, path: String): BitmapLoader =
                ResourceFullBitmapDecoder(AndroidAssetBitmapSource(assets, path))

        @JvmStatic
        fun fromFile(file: File): BitmapLoader =
                ResourceFullBitmapDecoder(FileBitmapSource(file.absolutePath))

        @JvmStatic
        fun fromFile(path: String): BitmapLoader =
                ResourceFullBitmapDecoder(FileBitmapSource(path))

        @JvmStatic
        fun fromMemory(bytes: ByteArray): BitmapLoader =
                fromMemory(bytes, 0, bytes.size)

        @JvmStatic
        fun fromMemory(bytes: ByteArray, offset: Int, length: Int): BitmapLoader =
                ResourceFullBitmapDecoder(MemoryBitmapSource(bytes, offset, length))

        @JvmStatic
        fun fromStream(stream: InputStream): BitmapLoader =
                ResourceFullBitmapDecoder(InputStreamBitmapSource(stream))
    }
}