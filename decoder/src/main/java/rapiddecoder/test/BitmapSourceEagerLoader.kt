package rapiddecoder.test

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import rapiddecoder.decoder.DecodeFailedException
import rapiddecoder.source.BitmapSource

internal class BitmapSourceEagerLoader(
        private val source: BitmapSource
) : EagerBitmapLoader() {
    override fun scaleTo(width: Int, height: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        source.decode(opts)

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
        val bitmap = source.decode(opts)
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
        val bitmap = source.decode(opts) ?: throw DecodeFailedException()
        val m = Matrix()
        m.setScale(scaleWidth, scaleHeight)
        val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
                m, true)
        return MemoryEagerBitmapLoader(scaledBitmap)
    }

    override fun scaleWidth(width: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        source.decode(opts)

        val densityScale = if (opts.inDensity == 0) {
            1f
        } else {
            opts.inTargetDensity / opts.inDensity.toFloat()
        }

        val sourceWidth = Math.round(opts.outWidth * densityScale)
        val sourceHeight = Math.round(opts.outHeight * densityScale)
        var scaleWidth = width / sourceWidth.toFloat()
        val height = Math.round(sourceHeight * scaleWidth)
        var sampleSize = 1
        while (scaleWidth <= 0.5f) {
            sampleSize *= 2
            scaleWidth *= 2f
        }

        opts.inScaled = true
        opts.inJustDecodeBounds = false
        opts.inSampleSize = sampleSize
        val bitmap = source.decode(opts)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return MemoryEagerBitmapLoader(scaledBitmap)
    }

    override fun scaleHeight(height: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        source.decode(opts)

        val densityScale = if (opts.inDensity == 0) {
            1f
        } else {
            opts.inTargetDensity / opts.inDensity.toFloat()
        }

        val sourceWidth = Math.round(opts.outWidth * densityScale)
        val sourceHeight = Math.round(opts.outHeight * densityScale)
        var scaleHeight = height / sourceHeight.toFloat()
        val width = Math.round(sourceWidth * scaleHeight)
        var sampleSize = 1
        while (scaleHeight <= 0.5f) {
            sampleSize *= 2
            scaleHeight *= 2f
        }

        opts.inScaled = true
        opts.inJustDecodeBounds = false
        opts.inSampleSize = sampleSize
        val bitmap = source.decode(opts)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return MemoryEagerBitmapLoader(scaledBitmap)
    }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): EagerBitmapLoader {
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        source.decode(opts)

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

        val decoder = source.createRegionDecoder()
        val region = Rect(
                Math.round(left / densityScale),
                Math.round(top / densityScale),
                Math.round(right / densityScale),
                Math.round(bottom / densityScale)
        )
        val newBitmap = decoder.decodeRegion(region, opts)
        return MemoryEagerBitmapLoader(newBitmap)
    }

    override fun loadBitmap(): Bitmap = source.decode(null) ?: throw DecodeFailedException()
}