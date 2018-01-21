package rapiddecoder

import android.graphics.Bitmap
import kotlin.math.roundToInt

internal class ScaleToTransformDecoder(private val other: BitmapDecoder,
                                       private val targetWidth: Float,
                                       private val targetHeight: Float) : BitmapDecoder() {
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

    override fun decode(options: LoadBitmapOptions,
                        input: BitmapDecodeInput,
                        output: BitmapDecodeOutput): Bitmap {
        val newInput = if (!input.finalScale) {
            input
        } else {
            BitmapDecodeInput(input).apply {
                finalScale = false
            }
        }
        val bitmap = synchronized(other.decodeLock) {
            other.decode(options, newInput, output)
        }

        if (!input.finalScale) {
            return bitmap
        }

        val targetWidth = targetWidth.roundToInt()
        val targetHeight = targetHeight.roundToInt()
        return if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
    }

    override fun hasMetadata(type: MetadataType): Boolean = when (type) {
        MetadataType.SIZE -> true
        else -> other.hasMetadata(type)
    }
}