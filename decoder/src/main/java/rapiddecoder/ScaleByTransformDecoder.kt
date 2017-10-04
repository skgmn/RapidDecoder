package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect

internal class ScaleByTransformDecoder(private val source: BitmapDecoder,
                                       private val x: Float,
                                       private val y: Float) : BitmapDecoder() {
    override val width: Int by lazy {
        Math.round(source.width * x)
    }
    override val height: Int by lazy {
        Math.round(source.height * y)
    }
    override val hasSize: Boolean
        get() = source.hasSize
    override val sourceWidth: Int
        get() = source.sourceWidth
    override val sourceHeight: Int
        get() = source.sourceHeight
    override val mimeType: String?
        get() = source.mimeType

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (source.hasSize && source.width == width && source.height == height) {
            source
        } else {
            ScaleToTransformDecoder(source, width.toFloat(), height.toFloat())
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
                source
            } else {
                ScaleByTransformDecoder(source, newX, newY)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return source.region(
                Math.round(left / x),
                Math.round(top / y),
                Math.round(right / x),
                Math.round(bottom / y))
                .scaleTo(right - left, bottom - top)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inSampleSize = 1
        opts.inScaled = true

        var sx = x
        var sy = y
        while (sx <= 0.5f && sy <= 0.5f) {
            opts.inSampleSize *= 2
            sx *= 2f
            sy *= 2f
        }

        val bitmap = synchronized(source.decodeLock) { source.decode(opts) }
        if (sx == 1f && sy == 1f || !options.finalScale) {
            return bitmap
        }

        val m = Matrix()
        m.setScale(sx, sy)
        val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m,
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