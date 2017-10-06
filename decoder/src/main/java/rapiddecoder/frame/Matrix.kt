package rapiddecoder.frame

import android.graphics.Rect

internal class Matrix : FramingMethod {
    override fun getBounds(sourceWidth: Int, sourceHeight: Int, frameWidth: Int, frameHeight: Int, outSrc: Rect, outDest: Rect) {
        val width = Math.min(sourceWidth, frameWidth)
        val height = Math.min(sourceHeight, frameHeight)
        outSrc.set(0, 0, width, height)
        outDest.set(0, 0, width, height)
    }
}