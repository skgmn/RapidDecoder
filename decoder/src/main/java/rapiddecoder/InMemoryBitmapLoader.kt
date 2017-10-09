package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas

internal class InMemoryBitmapLoader(private val bitmap: Bitmap) : BitmapLoader() {
    override val sourceWidth: Int
        get() = bitmap.width
    override val sourceHeight: Int
        get() = bitmap.height
    override val width: Int
        get() = bitmap.width
    override val height: Int
        get() = bitmap.height
    override val mimeType: String?
        get() = "image/png"
    override val hasSize: Boolean
        get() = true

    override fun scaleTo(width: Int, height: Int): BitmapLoader =
            ScaleToTransformLoader(this, width.toFloat(), height.toFloat())

    override fun scaleBy(x: Float, y: Float): BitmapLoader = ScaleByTransformLoader(this, x, y)

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        if (options.shouldBeRedrawnFrom(bitmap)) {
            val config = options.config ?: bitmap.config
            return bitmap.copy(config, options.mutable) ?:
                    Bitmap.createBitmap(bitmap.width, bitmap.height, config).also {
                        Canvas(it).drawBitmap(bitmap, 0f, 0f, null)
                    }
        } else {
            return bitmap
        }
    }
}