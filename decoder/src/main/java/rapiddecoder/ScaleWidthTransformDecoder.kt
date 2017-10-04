package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect

internal class ScaleWidthTransformDecoder(private val source: BitmapDecoder,
                                          private val targetWidth: Float,
                                          private val heightAdjustRatio: Float) : BitmapDecoder() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val hasSize: Boolean
        get() = source.hasSize
    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val mimeType: String?
        get() = source.mimeType

    private val targetHeight: Float by lazy {
        targetWidth * (source.height.toFloat() / source.width) * heightAdjustRatio
    }

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (source.hasSize && source.width == width && source.height == height) {
            source
        } else {
            ScaleToTransformDecoder(source, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleWidth(width: Int): BitmapLoader {
        checkScaleToArguments(width, 1)
        return if (source.hasSize && source.width == width) {
            source
        } else {
            val floatWidth = width.toFloat()
            if (floatWidth == targetWidth) {
                this
            } else {
                ScaleWidthTransformDecoder(source, floatWidth, heightAdjustRatio)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else if (source.hasSize) {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (source.width.toFloat() == newWidth && source.height.toFloat() == newHeight) {
                source
            } else {
                ScaleToTransformDecoder(source, newWidth, newHeight)
            }
        } else {
            val newWidth = targetWidth * x
            val newHeightAdjustRatio = heightAdjustRatio * (y / x)
            ScaleWidthTransformDecoder(source, newWidth, newHeightAdjustRatio)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val scale = targetWidth / source.width
        return source.region(
                Math.round(left / scale),
                Math.round(top / scale),
                Math.round(right / scale),
                Math.round(bottom / scale))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inSampleSize = 1
        opts.inScaled = false

        var sourceWidth: Float = source.sourceWidth.toFloat()
        val targetWidth = width
        while (sourceWidth >= targetWidth * 2) {
            opts.inSampleSize *= 2
            sourceWidth /= 2
        }

        val bitmap = synchronized(source.decodeLock) { source.decode(opts) }
        if (bitmap.width == targetWidth || !options.finalScale) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, height,
                options.filterBitmap)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    override fun decode(opts: BitmapFactory.Options): Bitmap = source.decode(opts)

    override fun decodeBounds(opts: BitmapFactory.Options) = source.decodeBounds(opts)

    override fun decodeRegion(region: Rect, opts: BitmapFactory.Options): Bitmap =
            source.decodeRegion(region, opts)
}