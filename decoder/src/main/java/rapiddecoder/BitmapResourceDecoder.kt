package rapiddecoder

import android.graphics.BitmapFactory

internal abstract class BitmapResourceDecoder(
        protected val source: BitmapSource) : BitmapDecoder() {
    protected var bitmapWidth = INVALID_SIZE
    protected var bitmapHeight = INVALID_SIZE
    protected var imageMimeType: String? = null
    protected var boundsDecoded = false

    protected var bitmapDensityRatio = if (source.densityRatioSupported) Float.NaN else 1f

    override val sourceWidth: Int
        get() {
            if (bitmapWidth == INVALID_SIZE) {
                synchronized(decodeLock) {
                    decodeBounds()
                }
            }
            return bitmapWidth
        }

    override val sourceHeight: Int
        get() {
            if (bitmapHeight == INVALID_SIZE) {
                synchronized(decodeLock) {
                    decodeBounds()
                }
            }
            return bitmapHeight
        }

    override val mimeType: String?
        get() {
            if (imageMimeType == null) {
                synchronized(decodeLock) {
                    decodeBounds()
                }
            }
            return imageMimeType
        }

    override val hasSize: Boolean
        get() = synchronized(decodeLock) { boundsDecoded }

    override val densityRatio: Float
        get() {
            if (bitmapDensityRatio.isNaN()) {
                synchronized(decodeLock) {
                    decodeBounds()
                }
            }
            return bitmapDensityRatio
        }

    protected fun decodeBounds() {
        if (boundsDecoded) {
            return
        }
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        source.decode(opts)

        imageMimeType = opts.outMimeType
        bitmapWidth = opts.outWidth
        bitmapHeight = opts.outHeight
        if (bitmapDensityRatio.isNaN()) {
            bitmapDensityRatio =
                    if (opts.inTargetDensity != 0 && opts.inDensity != 0) {
                        opts.inTargetDensity.toFloat() / opts.inDensity
                    } else {
                        1f
                    }
        }

        boundsDecoded = true
    }

    companion object {
        internal const val INVALID_SIZE = -1
    }
}