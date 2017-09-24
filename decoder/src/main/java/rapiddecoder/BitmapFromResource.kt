package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal class BitmapFromResource(private val res: Resources,
                                  private val resId: Int) : BitmapSource() {
    override fun decode(opts: BitmapFactory.Options): Bitmap {
        val bitmap = BitmapFactory.decodeResource(res, resId, opts)
        saveMetadata(opts)
        return bitmap
    }

    override fun decodeBounds(opts: BitmapFactory.Options) {
        BitmapFactory.decodeResource(res, resId, opts)
        saveMetadata(opts)
    }

    override fun createRegionDecoder(): BitmapRegionDecoder {
        val afd = res.openRawResourceFd(resId)
        if (afd != null) {
            try {
                return BitmapRegionDecoder.newInstance(afd.fileDescriptor, false)
            } catch (e: Throwable) {
                afd.close()
            }
        }
        val inputStream = res.openRawResource(resId)
        return BitmapRegionDecoder.newInstance(inputStream, false)
    }
}