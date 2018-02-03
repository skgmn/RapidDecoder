package rapiddecoder.decoder

import android.graphics.Bitmap
import android.graphics.Point
import rapiddecoder.BitmapLoader
import rapiddecoder.LoadBitmapOptions
import rapiddecoder.MetadataType
import rapiddecoder.source.BitmapSource

internal class BitmapSourceFullDecoder(source: BitmapSource) : BitmapSourceDecoder(source) {
    private var densityScaledWidth = INVALID_SIZE
    private var densityScaledHeight = INVALID_SIZE

    override val width: Int
        get() {
            if (densityScaledWidth == INVALID_SIZE) {
                densityScaledWidth = Math.round(sourceWidth * densityScale)
            }
            return densityScaledWidth
        }

    override val height: Int
        get() {
            if (densityScaledHeight == INVALID_SIZE) {
                densityScaledHeight = Math.round(sourceHeight * densityScale)
            }
            return densityScaledHeight
        }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasMetadata(MetadataType.SIZE) &&
                left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        return BitmapSourceRegionDecoder(source, left, top, right, bottom)
    }

    override fun decodeSource(options: LoadBitmapOptions,
                              input: BitmapDecodeInput,
                              output: BitmapDecodeOutput): Bitmap {
        val opts = output.options

        if (!source.reopenable && opts.inSampleSize != 1) {
            decodeBounds()
        }

        val bitmap = source.decode(opts) ?: throw DecodeFailedException()

        imageMimeType = opts.outMimeType
        if (bitmapDensityScale.isNaN()) {
            val scale = if (source.supportsDensityScale &&
                    opts.inTargetDensity != 0 && opts.inDensity != 0) {
                opts.inTargetDensity.toDouble() / opts.inDensity
            } else {
                1.0
            }
            bitmapDensityScale = scale.toFloat()
        }
        if (opts.inSampleSize == 1) {
            bitmapWidth = opts.outWidth
            bitmapHeight = opts.outHeight
            densityScaledWidth = bitmap.width
            densityScaledHeight = bitmap.height
        }

        return bitmap
    }

    override fun hasMetadata(type: MetadataType): Boolean {
        return synchronized(decodeLock) {
            when (type) {
                MetadataType.SIZE -> densityScaledWidth != INVALID_SIZE
                MetadataType.DENSITY_SCALE -> !bitmapDensityScale.isNaN()
                MetadataType.MIME_TYPE -> imageMimeType != null
                MetadataType.SOURCE_SIZE -> bitmapWidth != INVALID_SIZE
            }
        }
    }

    override fun getScaledSizeWithSampling(scaleX: Float, scaleY: Float): Point {
        var scaleRemainX = scaleX
        var scaleRemainY = scaleY
        var sampleScale = 1f
        while (scaleRemainX <= 0.5f && scaleRemainY <= 0.5f) {
            sampleScale *= 0.5f
            scaleRemainX *= 2f
            scaleRemainY *= 2f
        }

        val scaledWidth = Math.round(Math.round(
                Math.round(sourceWidth * sampleScale) * densityScale) * scaleRemainX)
        val scaledHeight = Math.round(Math.round(
                Math.round(sourceHeight * sampleScale) * densityScale) * scaleRemainY)
        return Point(scaledWidth, scaledHeight)
    }
}