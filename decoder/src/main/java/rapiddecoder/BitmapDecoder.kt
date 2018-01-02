package rapiddecoder

import android.graphics.Bitmap

internal abstract class BitmapDecoder : BitmapLoader() {
    internal abstract val densityRatio: Float

    internal val decodeLock = Any()

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return if (hasSize && width == this.width && height == this.height) {
            this
        } else {
            ScaleToTransformDecoder(this, width.toFloat(), height.toFloat())
        }
    }

    override fun scaleWidth(width: Int): BitmapLoader {
        checkScaleToArguments(width, 1)
        return ScaleWidthTransformDecoder(this, width.toFloat(), 1f)
    }

    override fun scaleHeight(height: Int): BitmapLoader {
        checkScaleToArguments(1, height)
        return ScaleHeightTransformDecoder(this, height.toFloat(), 1f)
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleByTransformDecoder(this, x, y)
        }
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap =
            synchronized(decodeLock) { decode(BitmapDecodeState(options)) }

    internal abstract fun decode(state: BitmapDecodeState): Bitmap
}