package rapiddecoder

import android.graphics.BitmapFactory

internal class BitmapDecodeState(private val loadBitmapOptions: LoadBitmapOptions) {
    val finalScale
        get() = loadBitmapOptions.finalScale
    val filterBitmap
        get() = loadBitmapOptions.filterBitmap

    var densityScale = loadBitmapOptions.finalScale
    var scaleX = 1f
    var scaleY = 1f

    val options = BitmapFactory.Options()
    var remainScaleX = 0f
        private set
    var remainScaleY = 0f
        private set

    fun prepareDecode() {
        val opts = options
        opts.inScaled = densityScale
        opts.inPreferredConfig = loadBitmapOptions.config
        opts.inMutable = loadBitmapOptions.mutable

        remainScaleX = scaleX
        remainScaleY = scaleY
        opts.inSampleSize = 1
        while (remainScaleX <= 0.5f && remainScaleY <= 0.5f) {
            opts.inSampleSize *= 2
            remainScaleX *= 2f
            remainScaleY *= 2f
        }
    }
}