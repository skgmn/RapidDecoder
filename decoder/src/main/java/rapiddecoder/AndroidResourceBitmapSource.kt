package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal class AndroidResourceBitmapSource(
        private val res: Resources,
        private val resId: Int) : BitmapSource {
    override val densityRatioSupported: Boolean
        get() = true

    override fun decode(opts: BitmapFactory.Options): Bitmap? =
            BitmapFactory.decodeResource(res, resId, opts)

    override fun createRegionDecoder(): BitmapRegionDecoder {
        val stream = res.openRawResource(resId)
        return BitmapRegionDecoder.newInstance(stream, false)
    }
}