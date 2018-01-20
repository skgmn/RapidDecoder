package rapiddecoder

import android.content.res.Resources
import android.graphics.*

internal class ResourceEagerBitmapLoader(
        private val res: Resources,
        private val id: Int
) : EagerBitmapLoader() {
    override fun scaleTo(width: Int, height: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, id, opts)

        val densityScale = if (opts.inDensity == 0) {
            1f
        } else {
            opts.inTargetDensity / opts.inDensity.toFloat()
        }

        val sourceWidth = Math.round(opts.outWidth * densityScale)
        val sourceHeight = Math.round(opts.outHeight * densityScale)
        var scaleWidth = width / sourceWidth.toFloat()
        var scaleHeight = height / sourceHeight.toFloat()
        var sampleSize = 1
        while (scaleWidth <= 0.5f && scaleHeight <= 0.5f) {
            sampleSize *= 2
            scaleWidth *= 2f
            scaleHeight *= 2f
        }

        opts.inScaled = true
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
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, id, opts)

        opts.inJustDecodeBounds = false
        opts.inSampleSize = 1
        val densityScale = if (opts.inDensity == 0) {
            1f
        } else {
            opts.inTargetDensity / opts.inDensity.toFloat()
        }

        var scale = densityScale
        while (scale <= 0.5f) {
            opts.inSampleSize *= 2
            scale *= 2f
        }

        val stream = res.openRawResource(id)
        val decoder = BitmapRegionDecoder.newInstance(stream, false)
        val region = Rect(
                Math.round(left / densityScale),
                Math.round(top / densityScale),
                Math.round(right / densityScale),
                Math.round(bottom / densityScale)
        )
        val newBitmap = decoder.decodeRegion(region, opts)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun loadBitmap(): Bitmap = BitmapFactory.decodeResource(res, id)
}