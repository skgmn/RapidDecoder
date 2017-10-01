package rapiddecoder

import android.graphics.BitmapFactory

internal abstract class BitmapSource : BitmapDecoder() {
    private var bitmapWidth = -1
    private var bitmapHeight = -1
    private var transformedWidth = -1
    private var transformedHeight = -1
    private var boundsDecoded = false
    private var imageMimeType: String? = null

    override val sourceWidth: Int
        get() {
            if (bitmapWidth == -1) {
                synchronized(decodeLock) {
                    if (bitmapWidth == -1) {
                        decodeBounds()
                    }
                }
            }
            return bitmapWidth
        }

    override val sourceHeight: Int
        get() {
            if (bitmapHeight == -1) {
                synchronized(decodeLock) {
                    if (bitmapHeight == -1) {
                        decodeBounds()
                    }
                }
            }
            return bitmapHeight
        }

    override val width: Int
        get() {
            if (transformedWidth == -1) {
                synchronized(decodeLock) {
                    if (transformedWidth == -1) {
                        decodeBounds()
                    }
                }
            }
            return transformedWidth
        }

    override val height: Int
        get() {
            if (transformedHeight == -1) {
                synchronized(decodeLock) {
                    if (transformedHeight == -1) {
                        decodeBounds()
                    }
                }
            }
            return transformedHeight
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
        get() = synchronized(decodeLock) { boundsDecoded }

    private fun decodeBounds() {
        val opts = BitmapFactory.Options()
        opts.inScaled = false
        opts.inJustDecodeBounds = true
        decodeBounds(opts)
    }

    protected fun saveMetadata(opts: BitmapFactory.Options) {
        if (boundsDecoded) {
            return
        }
        imageMimeType = opts.outMimeType
        val scale =
                if (opts.inDensity != 0 && opts.inTargetDensity != 0) {
                    opts.inTargetDensity.toDouble() / opts.inDensity
                } else {
                    1.0
                }
        if (opts.inScaled) {
            bitmapWidth = Math.floor(opts.outWidth / scale).toInt()
            bitmapHeight = Math.floor(opts.outHeight / scale).toInt()
            transformedWidth = opts.outWidth
            transformedHeight = opts.outHeight
        } else {
            bitmapWidth = opts.outWidth
            bitmapHeight = opts.outHeight
            transformedWidth = Math.ceil(opts.outWidth * scale).toInt()
            transformedHeight = Math.ceil(opts.outHeight * scale).toInt()
        }
        boundsDecoded = true
    }
}