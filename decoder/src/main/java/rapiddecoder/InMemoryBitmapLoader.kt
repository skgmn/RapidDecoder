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

    override fun scaleTo(width: Int, height: Int): BitmapLoader =
            ScaleToTransformLoader(this, width.toFloat(), height.toFloat())

    override fun scaleBy(x: Float, y: Float): BitmapLoader = ScaleByTransformLoader(this, x, y)

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return if (left <= 0 && top <= 0 && right >= width && bottom >= height) {
            this
        } else {
            RegionTransformLoader(this, left, top, right, bottom)
        }
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        return if (options.shouldBeRedrawnFrom(bitmap)) {
            val config = options.config ?: bitmap.config
            bitmap.copy(config, options.mutable) ?:
                    Bitmap.createBitmap(bitmap.width, bitmap.height, config).also {
                        Canvas(it).drawBitmap(bitmap, 0f, 0f, null)
                    }
        } else {
            bitmap
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean = type != MetadataType.DENSITY_SCALE
}