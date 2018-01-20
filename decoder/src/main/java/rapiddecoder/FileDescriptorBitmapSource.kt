package rapiddecoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import java.io.FileDescriptor

class FileDescriptorBitmapSource(private val fd: FileDescriptor) : BitmapSource {
    override val densityScaleSupported: Boolean
        get() = false

    override fun decode(opts: BitmapFactory.Options): Bitmap? =
            BitmapFactory.decodeFileDescriptor(fd, null, opts)

    override fun createRegionDecoder(): BitmapRegionDecoder =
            BitmapRegionDecoder.newInstance(fd, false)
}