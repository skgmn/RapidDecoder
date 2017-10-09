package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Rect

internal class ResourceRegionBitmapDecoder(
        source: BitmapSource,
        private val left: Int,
        private val top: Int,
        private val right: Int,
        private val bottom: Int) : ResourceBitmapDecoder(source) {
    private val l: Int
        get() = left.coerceAtLeast(0)
    private val t: Int
        get() = top.coerceAtLeast(0)
    private val r: Int by lazy {
        right.coerceAtMost(sourceWidth)
    }
    private val b: Int by lazy {
        bottom.coerceAtMost(sourceHeight)
    }

    override val width: Int
        get() = r - l
    override val height: Int
        get() = b - t

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasSize && left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        val newLeft = this.left + left
        val newTop = this.top + top
        return ResourceRegionBitmapDecoder(source, newLeft, newTop,
                newLeft + (right - left), newTop + (bottom - top))
    }

    override fun decode(state: BitmapDecodeState): Bitmap {
        val densityRatio = densityRatio
        state.scaleX *= densityRatio
        state.scaleY *= densityRatio
        state.prepareDecode()

        val opts = state.options
        val regionDecoder = source.createRegionDecoder()
        try {
            val scaledRegion = Rect(
                    Math.round(l / densityRatio),
                    Math.round(t / densityRatio),
                    Math.round(r / densityRatio),
                    Math.round(b / densityRatio))
            val bitmap = regionDecoder.decodeRegion(scaledRegion, opts)
                    ?: throw DecodeFailedException()
            if (!boundsDecoded) {
                imageMimeType = opts.outMimeType
                bitmapWidth = regionDecoder.width
                bitmapHeight = regionDecoder.height
                boundsDecoded = true
            }
            return bitmap
        } finally {
            regionDecoder.recycle()
        }
    }
}