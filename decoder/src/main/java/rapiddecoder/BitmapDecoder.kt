package rapiddecoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build

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

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val state = BitmapDecodeState(options)
        val bitmap = synchronized(decodeLock) { decode(state) }
        return checkMutable(bitmap, options)
    }

    private fun checkMutable(bitmap: Bitmap, options: LoadBitmapOptions): Bitmap {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB &&
                options.mutable &&
                !bitmap.isMutable) {
            val clone = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            Canvas(clone).drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            clone
        } else {
            bitmap
        }
    }

    internal abstract fun decode(state: BitmapDecodeState): Bitmap
}