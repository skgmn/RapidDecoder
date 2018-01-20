package rapiddecoder

import android.graphics.Bitmap

internal class ScaleByTransformDecoder(private val other: BitmapDecoder,
                                       private val x: Float,
                                       private val y: Float) : BitmapDecoder() {
    override val width: Int by lazy {
        Math.round(other.width * x)
    }
    override val height: Int by lazy {
        Math.round(other.height * y)
    }
    override val hasSize: Boolean
        get() = other.hasSize
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
        return if (other.hasSize && other.width == width && other.height == height) {
            other
        } else {
            ScaleToTransformDecoder(other, width.toFloat(), height.toFloat())
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
                other
            } else {
                ScaleByTransformDecoder(other, newX, newY)
            }
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        return other.region(
                Math.round(left / x),
                Math.round(top / y),
                Math.round(right / x),
                Math.round(bottom / y))
                .scaleTo(right - left, bottom - top)
    }

    override fun buildInput(options: LoadBitmapOptions): BitmapDecodeInput {
        return other.buildInput(options).apply {
            scaleX *= x
            scaleY *= y
        }
    }

    override fun decode(options: LoadBitmapOptions,
                        input: BitmapDecodeInput,
                        output: BitmapDecodeOutput): Bitmap {
        return synchronized(other.decodeLock) {
            other.decode(options, input, output)
        }
    }
}