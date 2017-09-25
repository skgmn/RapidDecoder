package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect

internal class RegionTransformDecoder(private val source: BitmapDecoder,
                                      private val l: Int,
                                      private val t: Int,
                                      private val r: Int,
                                      private val b: Int) : BitmapDecoder() {
    private val left: Int
        get() = l.coerceAtLeast(0)
    private val top: Int
        get() = t.coerceAtLeast(0)
    private val right: Int by lazy {
        r.coerceAtMost(source.width)
    }
    private val bottom: Int by lazy {
        b.coerceAtMost(source.height)
    }

    override val width: Int
        get() = right - left
    override val height: Int
        get() = bottom - top
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
        return if (hasSize && this.width == width && this.height == height) {
            this
        } else {
            ScaleToTransformDecoder(this, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleToTransformDecoder(this, width * x, height * y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasSize && left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        val newLeft = this.left + left
        val newTop = this.top + top
        return RegionTransformDecoder(source, newLeft, newTop,
                newLeft + (right - left), newTop + (bottom - top))
    }

    override fun decode(opts: BitmapFactory.Options): Bitmap {
        val regionDecoder = source.createRegionDecoder()
        try {
            return regionDecoder.decodeRegion(Rect(left, top, right, bottom), opts)
        } finally {
            regionDecoder.recycle()
        }
    }

    override fun decodeBounds(opts: BitmapFactory.Options) {
        source.decodeBounds(opts)
    }

    override fun createRegionDecoder(): BitmapRegionDecoder = source.createRegionDecoder()
}