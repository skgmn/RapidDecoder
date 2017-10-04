package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

interface BitmapSource {
    val densityRatioSupported: Boolean

    fun decode(opts: BitmapFactory.Options): Bitmap?
    fun createRegionDecoder(): BitmapRegionDecoder
}