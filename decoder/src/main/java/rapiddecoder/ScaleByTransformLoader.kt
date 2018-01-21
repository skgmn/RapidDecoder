package rapiddecoder

import android.graphics.Bitmap
import rapiddecoder.util.BitmapUtils

internal class ScaleByTransformLoader(private val other: BitmapLoader,
                                      private val x: Float,
                                      private val y: Float) : BitmapLoader() {
    override val width: Int by lazy {
        Math.round(other.width * x)
    }
    override val height: Int by lazy {
        Math.round(other.height * y)
    }
    override val sourceWidth: Int
        get() = other.sourceWidth
    override val sourceHeight: Int
        get() = other.sourceHeight
    override val mimeType: String?
        get() = other.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasMetadata(MetadataType.SIZE) &&
                other.width == width && other.height == height) {
            other
        } else {
            ScaleToTransformLoader(other, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newX = this.x * x
            val newY = this.y * y
            if (newX == 1f && newY == 1f) {
                other
            } else {
                ScaleByTransformLoader(other, newX, newY)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return other.region(
                Math.round(left / x),
                Math.round(top / y),
                Math.round(right / x),
                Math.round(bottom / y))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val newOptions = options.buildUpon()
                .setFinalScale(false)
                .setMutable(false)
                .setConfig(null)
                .build()
        val sourceBitmap = other.loadBitmap(newOptions)
        val finalWidth = Math.ceil(sourceBitmap.width.toDouble() * x).toInt()
        val finalHeight = Math.ceil(sourceBitmap.height.toDouble() * y).toInt()
        val scaledBitmap = if (options.shouldBeRedrawnFrom(sourceBitmap)) {
            BitmapUtils.copy(sourceBitmap, null, finalWidth, finalHeight,
                    options.config ?: sourceBitmap.config, options.filterBitmap)
        } else {
            Bitmap.createScaledBitmap(sourceBitmap, finalWidth, finalHeight,
                    options.filterBitmap)
        }
        if (sourceBitmap !== scaledBitmap) {
            sourceBitmap.recycle()
        }
        return scaledBitmap
    }

    override fun hasMetadata(type: MetadataType): Boolean = other.hasMetadata(type)
}