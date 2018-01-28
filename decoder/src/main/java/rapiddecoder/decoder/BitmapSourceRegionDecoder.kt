package rapiddecoder.decoder

import android.graphics.Bitmap
import android.graphics.Rect
import rapiddecoder.BitmapLoader
import rapiddecoder.LoadBitmapOptions
import rapiddecoder.MetadataType
import rapiddecoder.source.BitmapSource

internal class BitmapSourceRegionDecoder(
        source: BitmapSource,
        private val left: Int,
        private val top: Int,
        private val right: Int,
        private val bottom: Int) : BitmapSourceDecoder(source) {
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
        if (hasMetadata(MetadataType.SIZE) &&
                left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        val newLeft = this.left + left
        val newTop = this.top + top
        return BitmapSourceRegionDecoder(source, newLeft, newTop,
                newLeft + (right - left), newTop + (bottom - top))
    }

    override fun buildInput(options: LoadBitmapOptions): BitmapDecodeInput {
        val densityScale = densityScale
        return super.buildInput(options).apply {
            scaleX *= densityScale
            scaleY *= densityScale
        }
    }

    override fun decodeResource(options: LoadBitmapOptions, input: BitmapDecodeInput, output: BitmapDecodeOutput): Bitmap {
        val opts = output.options
        val regionDecoder = source.createRegionDecoder()
        try {
            val densityScale = densityScale
            val scaledRegion = Rect(
                    Math.round(l / densityScale),
                    Math.round(t / densityScale),
                    Math.round(r / densityScale),
                    Math.round(b / densityScale))
            val bitmap = regionDecoder.decodeRegion(scaledRegion, opts)
                    ?: throw DecodeFailedException()
            imageMimeType = opts.outMimeType
            if (bitmapWidth == INVALID_SIZE) {
                bitmapWidth = regionDecoder.width
                bitmapHeight = regionDecoder.height
            }
            return bitmap
        } finally {
            regionDecoder.recycle()
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean {
        return synchronized(decodeLock) {
            when (type) {
                MetadataType.DENSITY_SCALE -> !bitmapDensityScale.isNaN()
                MetadataType.MIME_TYPE -> imageMimeType != null
                MetadataType.SIZE, MetadataType.SOURCE_SIZE -> bitmapWidth != INVALID_SIZE
            }
        }
    }
}