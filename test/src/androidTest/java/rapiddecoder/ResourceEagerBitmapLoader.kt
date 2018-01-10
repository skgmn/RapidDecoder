package rapiddecoder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

internal class ResourceEagerBitmapLoader(
        private val res: Resources,
        private val id: Int
) : EagerBitmapLoader() {
    override fun scaleTo(width: Int, height: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, id, opts)

        val sourceWidth = opts.outWidth
        val sourceHeight = opts.outHeight
        var scaleWidth = width / sourceWidth.toFloat()
        var scaleHeight = height / sourceHeight.toFloat()
        var sampleSize = 1
        while (scaleWidth <= 0.5f && scaleHeight <= 0.5f) {
            sampleSize *= 2
            scaleWidth *= 2f
            scaleHeight *= 2f
        }

        opts.inJustDecodeBounds = false
        opts.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeResource(res, id, opts)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return MemoryEagerBitmapLoader(scaledBitmap)
    }

    override fun scaleBy(x: Float, y: Float): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        var scaleWidth = x
        var scaleHeight = y
        var sampleSize = 1
        while (scaleWidth <= 0.5f && scaleHeight <= 0.5f) {
            sampleSize *= 2
            scaleWidth *= 2f
            scaleHeight *= 2f
        }

        opts.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeResource(res, id, opts)
        val m = Matrix()
        m.setScale(scaleWidth, scaleHeight)
        val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
                m, true)
        return MemoryEagerBitmapLoader(scaledBitmap)
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): EagerBitmapLoader {
        val bitmap = BitmapFactory.decodeResource(res, id)
        val newBitmap = Bitmap.createBitmap(bitmap, left, top, right, bottom)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun loadBitmap(): Bitmap = BitmapFactory.decodeResource(res, id)
}