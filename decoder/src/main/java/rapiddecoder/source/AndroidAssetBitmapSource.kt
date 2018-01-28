package rapiddecoder.source

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

class AndroidAssetBitmapSource(
        private val assets: AssetManager,
        private val path: String) : BitmapSource {
    override val densityScaleSupported: Boolean
        get() = false

    override fun decode(opts: BitmapFactory.Options?): Bitmap? {
        return assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    override fun createRegionDecoder(): BitmapRegionDecoder {
        return assets.open(path).use { stream ->
            BitmapRegionDecoder.newInstance(stream, false)
        }
    }
}