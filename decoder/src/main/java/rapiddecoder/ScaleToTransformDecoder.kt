package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal class ScaleToTransformDecoder(private val source: BitmapDecoder,
                                       private val targetWidth: Float,
                                       private val targetHeight: Float) : BitmapDecoder() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        val floatWidth = width.toFloat()
        val floatHeight = height.toFloat()
        return if (floatWidth == targetWidth && floatHeight == targetHeight) {
            this
        } else {
            ScaleToTransformDecoder(source, floatWidth, floatHeight)
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleToTransformDecoder(source, targetWidth * x, targetHeight * y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val ratioX = targetWidth / width
        val ratioY = targetHeight / height
        return RegionTransformDecoder(source,
                Math.round(left / ratioX),
                Math.round(top / ratioY),
                Math.round(right / ratioX),
                Math.round(bottom / ratioY))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(approx: Boolean): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inSampleSize = 1

        var sourceWidth = source.width
        var sourceHeight = source.height
        val targetWidth = Math.round(targetWidth)
        val targetHeight = Math.round(targetHeight)
        while (sourceWidth >= targetWidth * 2 && sourceHeight >= targetHeight * 2) {
            opts.inSampleSize *= 2
            sourceWidth /= 2
            sourceHeight /= 2
        }

        val bitmap = synchronized(decodeLock) { source.decode(opts) }
        if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    override fun decode(opts: BitmapFactory.Options): Bitmap = source.decode(opts)

    override fun decodeBounds(opts: BitmapFactory.Options) = source.decodeBounds(opts)

    override fun createRegionDecoder(): BitmapRegionDecoder = source.createRegionDecoder()
}