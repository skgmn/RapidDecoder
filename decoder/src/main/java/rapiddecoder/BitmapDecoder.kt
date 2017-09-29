package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal abstract class BitmapDecoder : BitmapLoader() {
    protected val decodeLock = Any()

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        if (hasSize && width == this.width && height == this.height) {
            return this
        }
        return ScaleToTransformDecoder(this, width.toFloat(), height.toFloat())
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return if (x == 1f && y == 1f) {
            this
        } else {
            ScaleByTransformDecoder(this, x, y)
        }
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasSize && left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        return RegionTransformDecoder(this, left, top, right, bottom)
    }

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap {
        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = options.config
        return synchronized(decodeLock) {
            decode(opts)
        }
    }

    internal abstract fun decode(opts: BitmapFactory.Options): Bitmap
    internal abstract fun decodeBounds(opts: BitmapFactory.Options)
    internal abstract fun createRegionDecoder(): BitmapRegionDecoder
}