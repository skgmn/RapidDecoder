package rapiddecoder.frame

import android.graphics.Rect

internal class Center : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int, outSrc: Rect, outDest: Rect) {
        if (sourceWidth > frameWidth) {
            outSrc.left = (sourceWidth - frameWidth) / 2
            outSrc.right = outSrc.left + frameWidth
            outDest.left = 0
            outDest.right = frameWidth
        } else {
            outSrc.left = 0
            outSrc.right = sourceWidth
            outDest.left = (frameWidth - sourceWidth) / 2
            outDest.right = outDest.left + sourceWidth
        }
        if (sourceHeight > frameHeight) {
            outSrc.top = (sourceHeight - frameHeight) / 2
            outSrc.bottom = outSrc.top + frameHeight
            outDest.top = 0
            outDest.bottom = frameHeight
        } else {
            outSrc.top = 0
            outSrc.bottom = sourceHeight
            outDest.top = (frameHeight - sourceHeight) / 2
            outDest.bottom = outDest.top + sourceHeight
        }
    }
}