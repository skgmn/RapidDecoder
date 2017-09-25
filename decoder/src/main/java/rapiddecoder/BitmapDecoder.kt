package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal abstract class BitmapDecoder : BitmapLoader() {
    protected val decodeLock = Any()

    override fun scaleTo(width: Int, height: Int): BitmapLoader {
        checkScaleToArguments(width, height)
        return ScaleToTransformDecoder(this, width.toFloat(), height.toFloat())
    }

    override fun scaleBy(x: Float, y: Float): BitmapLoader {
        checkScaleByArguments(x, y)
        return ScaleByTransformDecoder(this, x, y)
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader =
            RegionTransformDecoder(this, left, top, right, bottom)

    override fun loadBitmap(options: LoadBitmapOptions): Bitmap =
            synchronized(decodeLock) { decode(BitmapFactory.Options()) }

    internal abstract fun decode(opts: BitmapFactory.Options): Bitmap
    internal abstract fun decodeBounds(opts: BitmapFactory.Options)
    internal abstract fun createRegionDecoder(): BitmapRegionDecoder
}