package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect

internal class BitmapFromResource(private val res: Resources,
                                  private val resId: Int) : BitmapSource() {
    override fun decode(opts: BitmapFactory.Options): Bitmap {
        val bitmap = BitmapFactory.decodeResource(res, resId, opts)
                ?: throw Resources.NotFoundException("id=$resId")
        saveMetadata(opts)
        return bitmap
    }

    override fun decodeBounds(opts: BitmapFactory.Options) {
        BitmapFactory.decodeResource(res, resId, opts)
        saveMetadata(opts)
    }

    override fun decodeRegion(region: Rect, opts: BitmapFactory.Options): Bitmap {
        val inputStream = res.openRawResource(resId)
        val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
        try {
            val bitmap = regionDecoder.decodeRegion(region, opts)
            saveMetadata(opts, regionDecoder)
            return bitmap
        } finally {
            regionDecoder.recycle()
        }
    }
}