package rapiddecoder.decoder

import android.graphics.Bitmap
import rapiddecoder.BitmapLoader
import rapiddecoder.LoadBitmapOptions
import rapiddecoder.MetadataType

internal class ScaleWidthTransformDecoder(private val other: BitmapDecoder,
                                          private val targetWidth: Float,
                                          private val heightAdjustRatio: Float) : BitmapDecoder() {
    override val width: Int
        get() = Math.round(targetWidth)
    override val height: Int
        get() = Math.round(targetHeight)
    override val sourceWidth: Int
        get() = other.sourceWidth
    override val sourceHeight: Int
        get() = other.sourceHeight
    override val mimeType: String?
        get() = other.mimeType
    override val densityScale: Float
        get() = other.densityScale

    private val targetHeight: Float by lazy {
        targetWidth * (other.height.toFloat() / other.width) * heightAdjustRatio
    }

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasMetadata(MetadataType.SIZE) &&
                other.width == width && other.height == height) {
            other
        } else {
            ScaleToTransformDecoder(other, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleWidth(width: Int): BitmapLoader {
        checkScaleToArguments(width, 1)
        return if (other.hasMetadata(MetadataType.SIZE) && other.width == width) {
            other
        } else {
            val floatWidth = width.toFloat()
            if (floatWidth == targetWidth) {
                this
            } else {
                ScaleWidthTransformDecoder(other, floatWidth, heightAdjustRatio)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else if (other.hasMetadata(MetadataType.SIZE)) {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (other.width.toFloat() == newWidth && other.height.toFloat() == newHeight) {
                other
            } else {
                ScaleToTransformDecoder(other, newWidth, newHeight)
            }
        } else {
            val newWidth = targetWidth * x
            val newHeightAdjustRatio = heightAdjustRatio * (y / x)
            ScaleWidthTransformDecoder(other, newWidth, newHeightAdjustRatio)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val scale = targetWidth / other.width
        return other.region(
                Math.round(left / scale),
                Math.round(top / scale),
                Math.round(right / scale),
                Math.round(bottom / scale))
                .scaleTo(right - left, bottom - top)
    }

    override fun buildInput(options: LoadBitmapOptions): BitmapDecodeInput {
        val scale = targetWidth / other.width
        return other.buildInput(options).apply {
            scaleX *= scale
            scaleY *= scale
        }
    }

    override fun decode(options: LoadBitmapOptions,
                        input: BitmapDecodeInput,
                        output: BitmapDecodeOutput): Bitmap {
        return synchronized(other.decodeLock) {
            other.decode(options, input, output)
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean = when (type) {
        MetadataType.SIZE -> true
        else -> other.hasMetadata(type)
    }
}