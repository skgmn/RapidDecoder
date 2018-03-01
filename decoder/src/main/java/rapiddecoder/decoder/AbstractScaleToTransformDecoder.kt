package rapiddecoder.decoder

import android.graphics.Bitmap
import rapiddecoder.LoadBitmapOptions

internal abstract class AbstractScaleToTransformDecoder(
        protected val other: BitmapDecoder
) : BitmapDecoder() {
    protected abstract val targetWidth: Float
    protected abstract val targetHeight: Float

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

        val targetWidth = width
        val targetHeight = height
        return if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
    }
}