package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect

internal class ScaleHeightTransformDecoder(private val source: BitmapDecoder,
                                           private val targetHeight: Float,
                                           private val widthAdjustRatio: Float) : BitmapDecoder() {
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

    private val targetWidth: Float by lazy {
        targetHeight * (source.width.toFloat() / source.height) * widthAdjustRatio
    }

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (source.hasSize && source.width == width && source.height == height) {
            source
        } else {
            ScaleToTransformDecoder(source, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleHeight(height: Int): BitmapLoader {
        checkScaleToArguments(1, height)
        return if (source.hasSize && source.height == height) {
            source
        } else {
            val floatHeight = height.toFloat()
            if (floatHeight == targetHeight) {
                this
            } else {
                ScaleHeightTransformDecoder(source, floatHeight, widthAdjustRatio)
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
            val newHeight = targetHeight * y
            val newWidthAdjustRatio = widthAdjustRatio * (x / y)
            ScaleHeightTransformDecoder(source, newHeight, newWidthAdjustRatio)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val scale = targetHeight / source.height
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

        var sourceHeight: Float = source.sourceHeight.toFloat()
        val targetHeight = height
        while (sourceWidth >= targetHeight * 2) {
            opts.inSampleSize *= 2
            sourceHeight /= 2
        }

        val bitmap = synchronized(decodeLock) { source.decode(opts) }
        if (bitmap.height == targetHeight || !options.finalScale) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, targetHeight,
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