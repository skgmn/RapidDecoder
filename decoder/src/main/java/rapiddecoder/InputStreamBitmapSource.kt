package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import java.io.InputStream

internal class InputStreamBitmapSource(stream: InputStream) : BitmapSource {
    private val wrappedStream = RewindableInputStream(stream)

    override val densityRatioSupported: Boolean
        get() = false

    override fun decode(opts: BitmapFactory.Options): Bitmap? {
        if (!opts.inJustDecodeBounds) {
            wrappedStream.rewind()
        }
        return BitmapFactory.decodeStream(wrappedStream, null, opts)
    }

    override fun createRegionDecoder(): BitmapRegionDecoder {
        wrappedStream.rewind()
        return BitmapRegionDecoder.newInstance(wrappedStream, false)
    }
}