package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal class ScaleByTransformDecoder(private val source: BitmapDecoder,
                                       private val x: Float,
                                       private val y: Float) : BitmapDecoder() {
    override val width: Int by lazy {
        Math.round(source.width * x)
    }
    override val height: Int by lazy {
        Math.round(source.height * y)
    }
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return ScaleToTransformDecoder(source, width.toFloat(), height.toFloat())
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newX = this.x * x
            val newY = this.y * y
            if (newX == 1f && newY == 1f) {
                source
            } else {
                ScaleByTransformDecoder(source, newX, newY)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return RegionTransformDecoder(source,
                Math.round(left / x),
                Math.round(top / y),
                Math.round(right / x),
                Math.round(bottom / y))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(approx: Boolean): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inSampleSize = 1

        var ratioX = x
        var ratioY = y
        while (ratioX >= 2f && ratioY >= 2f) {
            opts.inSampleSize *= 2
            ratioX /= 2f
            ratioY /= 2f
        }

        val bitmap = synchronized(decodeLock) { source.decode(opts) }
        if (ratioX == 1f && ratioY == 1f) {
            return bitmap
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                Math.round(bitmap.width * ratioX),
                Math.round(bitmap.height * ratioY),
                true)
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }

    override fun decode(opts: BitmapFactory.Options): Bitmap = source.decode(opts)

    override fun decodeBounds(opts: BitmapFactory.Options) = source.decodeBounds(opts)

    override fun createRegionDecoder(): BitmapRegionDecoder = source.createRegionDecoder()
}