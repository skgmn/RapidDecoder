package rapiddecoder.decoder

import rapiddecoder.BitmapLoader
import rapiddecoder.LoadBitmapOptions
import rapiddecoder.MetadataType

internal class ScaleToTransformDecoder(
        other: BitmapDecoder,
        override val targetWidth: Float,
        override val targetHeight: Float
) : AbstractScaleToTransformDecoder(other) {
    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (other.hasMetadata(MetadataType.SIZE) &&
                other.width == width && other.height == height) {
            other
        } else {
            val floatWidth = width.toFloat()
            val floatHeight = height.toFloat()
            if (floatWidth == targetWidth && floatHeight == targetHeight) {
                this
            } else {
                ScaleToTransformDecoder(other, floatWidth, floatHeight)
            }
        }
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            val newWidth = targetWidth * x
            val newHeight = targetHeight * y
            if (other.hasMetadata(MetadataType.SIZE) &&
                    other.width.toFloat() == newWidth &&
                    other.height.toFloat() == newHeight) {
                other
            } else {
                ScaleToTransformDecoder(other, newWidth, newHeight)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        val sx = targetWidth / other.width
        val sy = targetHeight / other.height
        return other.region(
                Math.round(left / sx),
                Math.round(top / sy),
                Math.round(right / sx),
                Math.round(bottom / sy))
                .scaleTo(right - left, bottom - top)
    }

    override fun buildInput(options: LoadBitmapOptions): BitmapDecodeInput {
        return BitmapDecodeInput(options).apply {
            scaleX = targetWidth / other.width
            scaleY = targetHeight / other.height
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean = when (type) {
        MetadataType.SIZE -> true
        else -> other.hasMetadata(type)
    }
}