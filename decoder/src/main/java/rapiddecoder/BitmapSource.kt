package rapiddecoder

import android.graphics.BitmapFactory

internal abstract class BitmapSource : BitmapDecoder() {
    private var bitmapWidth = -1
    private var bitmapHeight = -1
    private var imageMimeType: String? = null

    override val width: Int
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

    override val height: Int
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

    private fun decodeBounds() {
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        decodeBounds(opts)
    }

    protected fun saveMetadata(opts: BitmapFactory.Options) {
        bitmapWidth = opts.outWidth
        bitmapHeight = opts.outHeight
        imageMimeType = opts.outMimeType
    }
}