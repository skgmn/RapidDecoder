package rapiddecoder

import android.graphics.Bitmap

internal class ScaleToTransformLoader(private val source: BitmapLoader,
                                      private val targetWidth: Float,
                                      private val targetHeight: Float) : BitmapLoader() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val hasSize: Boolean
        get() = true
    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        if (source.hasSize && source.width == width && source.height == height) {
            return source
        } else {
            val floatWidth = width.toFloat()
            val floatHeight = height.toFloat()
            return if (floatWidth == targetWidth && floatHeight == targetHeight) {
                this
            } else {
                ScaleToTransformLoader(source, floatWidth, floatHeight)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (source.hasSize &&
                    source.width.toFloat() == newWidth &&
                    source.height.toFloat() == newHeight) {
                source
            } else {
                ScaleToTransformLoader(source, newWidth, newHeight)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val sx = targetWidth / source.width
        val sy = targetHeight / source.height
        return source.region(
                Math.round(left / sx),
                Math.round(top / sy),
                Math.round(right / sx),
                Math.round(bottom / sy))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val bitmap1 = source.loadBitmap(options)
        val bitmap2 = Bitmap.createScaledBitmap(bitmap1,
                Math.round(targetWidth),
                Math.round(targetHeight),
                options.filterBitmap)
        if (bitmap1 !== bitmap2) {
            bitmap1.recycle()
        }
        return bitmap2
    }
}