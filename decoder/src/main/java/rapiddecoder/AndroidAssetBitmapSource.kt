package rapiddecoder

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

class AndroidAssetBitmapSource(
        private val assets: AssetManager,
        private val path: String) : BitmapSource {
    override val densityRatioSupported: Boolean
        get() = false

    override fun decode(opts: BitmapFactory.Options): Bitmap? {
        assets.openFd(path).use { pfd ->
            return BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, opts)
        }
    }

    override fun createRegionDecoder(): BitmapRegionDecoder {
        assets.openFd(path).use { pfd ->
            return BitmapRegionDecoder.newInstance(pfd.fileDescriptor, false)
        }
    }
}