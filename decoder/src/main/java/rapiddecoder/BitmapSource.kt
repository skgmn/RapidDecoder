package rapiddecoder

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder

internal abstract class BitmapSource : BitmapDecoder() {
    private var bitmapWidth = INVALID_SIZE
    private var bitmapHeight = INVALID_SIZE
    private var densityScaledWidth = INVALID_SIZE
    private var densityScaledHeight = INVALID_SIZE
    private var imageMimeType: String? = null

    override val sourceWidth: Int
        get() {
            if (bitmapWidth == INVALID_SIZE) {
                synchronized(decodeLock) {
                    if (bitmapWidth == INVALID_SIZE) {
                        decodeBounds()
                    }
                }
            }
            return bitmapWidth
        }

    override val sourceHeight: Int
        get() {
            if (bitmapHeight == INVALID_SIZE) {
                synchronized(decodeLock) {
                    if (bitmapHeight == INVALID_SIZE) {
                        decodeBounds()
                    }
                }
            }
            return bitmapHeight
        }

    override val width: Int
        get() {
            if (densityScaledWidth == INVALID_SIZE) {
                synchronized(decodeLock) {
                    if (densityScaledWidth == INVALID_SIZE) {
                        decodeBounds()
                    }
                }
            }
            return densityScaledWidth
        }

    override val height: Int
        get() {
            if (densityScaledHeight == INVALID_SIZE) {
                synchronized(decodeLock) {
                    if (densityScaledHeight == INVALID_SIZE) {
                        decodeBounds()
                    }
                }
            }
            return densityScaledHeight
        }

    override val mimeType: String?
        get() {
            if (imageMimeType == null) {
                synchronized(decodeLock) {
                    if (imageMimeType == null) {
                        decodeBounds()
                    }
                }
            }
            return imageMimeType
        }

    override val hasSize: Boolean
        get() = synchronized(decodeLock) { densityScaledWidth != INVALID_SIZE }

    private fun decodeBounds() {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        decodeBounds(opts)
    }

    protected fun saveMetadata(opts: BitmapFactory.Options) {
        imageMimeType = opts.outMimeType
        if (opts.inScaled) {
            if (bitmapWidth == INVALID_SIZE) {
                val scale = getDensityScale(opts)
                bitmapWidth = Math.floor(opts.outWidth / scale).toInt()
                bitmapHeight = Math.floor(opts.outHeight / scale).toInt()
            }
            if (densityScaledWidth == INVALID_SIZE) {
                densityScaledWidth = opts.outWidth
                densityScaledHeight = opts.outHeight
            }
        } else {
            if (bitmapWidth == INVALID_SIZE) {
                bitmapWidth = opts.outWidth
                bitmapHeight = opts.outHeight
            }
            if (densityScaledWidth == INVALID_SIZE) {
                val scale = getDensityScale(opts)
                densityScaledWidth = Math.ceil(opts.outWidth * scale).toInt()
                densityScaledHeight = Math.ceil(opts.outHeight * scale).toInt()
            }
        }
    }

    protected fun saveMetadata(opts: BitmapFactory.Options, regionDecoder: BitmapRegionDecoder) {
        imageMimeType = opts.outMimeType
        if (bitmapWidth == INVALID_SIZE) {
            bitmapWidth = regionDecoder.width
            bitmapHeight = regionDecoder.height
        }
    }

    private fun getDensityScale(opts: BitmapFactory.Options): Double {
        return if (opts.inDensity != 0 && opts.inTargetDensity != 0) {
            opts.inTargetDensity.toDouble() / opts.inDensity
        } else {
            1.0
        }
    }

    companion object {
        private const val INVALID_SIZE = -1
    }
}