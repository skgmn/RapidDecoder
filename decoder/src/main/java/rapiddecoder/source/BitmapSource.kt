package rapiddecoder.source

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal interface BitmapSource {
    val densityScaleSupported: Boolean

    fun decode(opts: BitmapFactory.Options?): Bitmap?
    fun createRegionDecoder(): BitmapRegionDecoder
}